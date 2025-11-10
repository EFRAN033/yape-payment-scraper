plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.yape_payment_scraper"
    // 1. CORRECCIÓN: Volvemos a 36
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.yape_payment_scraper"
        minSdk = 24
        // 2. CORRECCIÓN: Volvemos a 36
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
        // 3. CORRECCIÓN: Volvemos a tu versión original
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11" // 4. CORRECCIÓN: Volvemos a tu versión original
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // --- 5. DEPENDENCIAS AÑADIDAS (que faltaban) ---
    // Para hacer llamadas de red (API)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Para convertir JSON (el formato de tu API) a objetos de Kotlin
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // --- FIN DE DEPENDENCIAS AÑADIDAS ---
}