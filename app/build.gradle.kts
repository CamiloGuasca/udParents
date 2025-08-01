plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.example.udparents"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.udparents"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        compose = true
    }
}

dependencies {

    // Dependencias de AndroidX y Compose
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    //implementation(libs.androidx.material3)
    // Necesario para usar FlowRow (ExperimentalLayoutApi)
    implementation("androidx.compose.foundation:foundation-layout:1.6.0")
    implementation("androidx.compose.material3:material3:1.2.1") // 👈 versión explícita si la anterior falla
    implementation("androidx.compose.material:material-icons-extended:1.6.1")
// Asegúrate de tener esta también (ya la tienes probablemente en libs.androidx.material3, pero mejor explícita si tienes errores)


    // Dependencias de Navegación (Compose)
    implementation(libs.androidx.navigation.runtime.android)
    implementation("androidx.navigation:navigation-compose:2.9.1")

    // Dependencias de Lifecycle (Compose)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")

    // Firebase BOM: Gestiona las versiones de todas las librerías de Firebase
    // ¡Esta línea es CRÍTICA y debe estar presente UNA SOLA VEZ!
    implementation(platform("com.google.firebase:firebase-bom:33.1.0")) // Asegúrate de que esta sea la versión más reciente

    // Declaraciones de librerías de Firebase (SIN versión, ya que la BOM las maneja)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)

    // Dependencia de Google Mobile Ads SDK
    implementation(libs.ads.mobile.sdk)

    // Dependencias de testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
