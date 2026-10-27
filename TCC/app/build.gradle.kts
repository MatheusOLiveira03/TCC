plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.tcc"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.tcc"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val apiUrl = providers.gradleProperty("oficinaApiUrl")
            .getOrElse("https://oficina-aprender-api.onrender.com/")
            .trim().trimEnd('/') + "/"
        require(apiUrl.none { it == '"' || it == '\\' || it == '\n' || it == '\r' })
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
    }
    buildFeatures { buildConfig = true }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    doFirst {
        val url = providers.gradleProperty("oficinaApiUrl")
            .getOrElse("https://oficina-aprender-api.onrender.com/").trim()
        require(url.startsWith("https://")) { "A versão release exige uma API HTTPS." }
    }
}
