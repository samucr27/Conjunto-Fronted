plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Avisos en el celular (Firebase Cloud Messaging). Sin app/google-services.json la app compila
// y funciona igual, solo que no recibe avisos. Cómo obtenerlo: docs/firebase.md
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.conjuntoresidencial"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.residencial"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Dos apps del mismo código (P5): residentes y administración. Lo común está en src/main;
    // R.bool.app_admin dice en cuál se está. Compilar: assembleResidenteDebug / assembleAdministracionDebug
    flavorDimensions += "app"
    productFlavors {
        create("residente") {
            dimension = "app"
            resValue("string", "app_name", "MiConjunto")
            resValue("bool", "app_admin", "false")
            buildConfigField("String", "TIPO_APP", "\"RESIDENTE\"")
        }
        create("administracion") {
            dimension = "app"
            applicationIdSuffix = ".admin"
            resValue("string", "app_name", "MiConjunto Admin")
            resValue("bool", "app_admin", "true")
            buildConfigField("String", "TIPO_APP", "\"ADMIN\"")
        }
    }

    // URL del backend en Render. Va como recurso (R.string.api_url) y no en BuildConfig:
    // Kotlin copia las constantes de BuildConfig dentro de cada clase al compilar, y la
    // compilación incremental podía dejar en el APK la URL anterior.
    val urlRender = "https://conjunto-backend-uq7y.onrender.com/"

    buildTypes {
        debug {
            // Por defecto, Render. Para el backend local (perfil "demo") desde el emulador:
            //   ./gradlew installDebug -PapiUrl=http://10.0.2.2:8080/
            val apiUrl = (project.findProperty("apiUrl") as String?) ?: urlRender
            resValue("string", "api_url", apiUrl)
        }
        release {
            resValue("string", "api_url", urlRender)
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true // BuildConfig.VERSION_NAME
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    implementation("androidx.core:core-ktx:1.13.1")
    // Pantalla de inicio con el logo animado (Android 12+; en anteriores, el logo fijo)
    implementation("androidx.core:core-splashscreen:1.0.1")
    // Entrar con huella o rostro
    implementation("androidx.biometric:biometric:1.1.0")
    // Checkout de Wompi en una pestaña del navegador (Custom Tabs)
    implementation("androidx.browser:browser:1.8.0")
    // Desenfoque en vivo del contenido bajo las barras translúcidas (Apache 2.0)
    implementation("com.github.Dimezis:BlurView:version-3.2.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Retrofit & Gson
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // Imágenes del feed (P5) desde enlaces firmados: carga, caché y crossfade (Apache 2.0)
    implementation("io.coil-kt:coil:2.6.0")

    // Corrutinas
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    // await() sobre las tareas de Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Avisos en el celular
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-messaging")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")

    // Navigation
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}