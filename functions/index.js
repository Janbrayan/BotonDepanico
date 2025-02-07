// index.js en tu carpeta functions
// Versión con prioridad alta en los mensajes FCM
const { onValueWritten } = require("firebase-functions/v2/database");
const admin = require("firebase-admin");

admin.initializeApp();

exports.onAlarmStateChange = onValueWritten("/alarmState", async (event) => {
  const before = event.data.before.val() || {};
  const after = event.data.after.val() || {};

  // ALARMA ACTIVADA
  if (!before.active && after.active) {
    const triggeredBy = after.triggeredBy;
    const triggeredByName = after.triggeredByName;
    const snapshot = await admin.database().ref("/users").get();

    const tokens = [];
    snapshot.forEach((userSnap) => {
      const uid = userSnap.key;
      if (uid !== triggeredBy) {
        const token = userSnap.child("fcmToken").val();
        if (token) tokens.push(token);
      }
    });

    if (tokens.length > 0) {
      // Mensaje con prioridad alta
      const message = {
        data: {
          type: "ALARM_TRIGGERED",
          triggeredByName: triggeredByName || "Alguien"
        },
        android: {
          priority: "high"  // alta prioridad en Android
        },
        apns: {
          headers: {
            "apns-priority": "10" // alta prioridad en iOS
          }
        },
        tokens: tokens
      };
      await admin.messaging().sendMulticast(message);
    }

  // ALARMA DESACTIVADA
  } else if (before.active && !after.active) {
    const snapshot = await admin.database().ref("/users").get();
    const tokens = [];
    snapshot.forEach((userSnap) => {
      const token = userSnap.child("fcmToken").val();
      if (token) tokens.push(token);
    });

    if (tokens.length > 0) {
      // Mensaje con prioridad alta (si quieres notificar también la desactivación)
      const message = {
        data: {
          type: "ALARM_STOPPED"
        },
        android: {
          priority: "high"
        },
        apns: {
          headers: {
            "apns-priority": "10"
          }
        },
        tokens: tokens
      };
      await admin.messaging().sendMulticast(message);
    }
  }
});
