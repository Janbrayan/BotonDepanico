plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.botondepanico"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.botondepanico"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Habilitar Compose
    buildFeatures {
        compose = true
    }

    // ¡IMPORTANTE! especifica la versión del compiler extension
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1" // o la versión que uses
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // BOM de Compose (usa una versión reciente, p. ej. 2023.07.00 o la que tengas en libs)
    implementation(platform(libs.androidx.compose.bom))

    // Módulos esenciales de Compose
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Añade foundation y animation si no están en tu libs
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.animation)

    // Firebase, Google Auth, Navigation, etc.
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)
    //implementation(libs.play.services.awareness)
    //implementation(libs.play.services.contextmanager)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.messaging.ktx)
    implementation(libs.play.services.maps)
    implementation(libs.volley)
    implementation(libs.androidx.runner)
    implementation(libs.play.services.fido)
    implementation(libs.play.services.fido)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation (libs.accompanist.permissions)
    implementation (libs.android.maps.compose)
    implementation (libs.coil.compose)
    implementation (libs.maps.compose.vversion)
    implementation (libs.android.maps.utils)


}
