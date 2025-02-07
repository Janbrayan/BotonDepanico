package com.example.botondepanico.ui.theme.alarm

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.botondepanico.MainActivity

class AlarmFullScreenActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlarmFullScreenUI(
                onOpenApp = {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}

@Composable
fun AlarmFullScreenUI(onOpenApp: () -> Unit) {
    MaterialTheme {
        Surface {
            Column {
                Text(
                    text = "¡ALERTA ACTIVADA!",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "No puedes detenerla.\nSolo el emisor la apagará.")
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onOpenApp) {
                    Text("ABRIR APP")
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewAlarmFullScreenUI() {
    AlarmFullScreenUI {}
}
