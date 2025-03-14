plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.horllymobile.statusvideocutter"
    compileSdk = 35

    androidResources {
        generateLocaleConfig = true
    }

    defaultConfig {
        applicationId = "com.horllymobile.statusvideocutter"
        minSdk = 24
        targetSdk = 35
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
    implementation("androidx.navigation:navigation-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
//    implementation("com.arthenica:ffmpegkit-full:6.0")
    implementation("com.arthenica:ffmpeg-kit-full:6.0-2")

    implementation("androidx.media3:media3-exoplayer:1.5.1")

    implementation("com.google.accompanist:accompanist-systemuicontroller:0.30.1")

    implementation("androidx.media3:media3-ui:1.5.1")

    implementation("androidx.compose.material:material-icons-extended")

    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))

    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")

    implementation("com.google.firebase:firebase-appdistribution-api-ktx:16.0.0-beta14")

    implementation("androidx.datastore:datastore-preferences:1.1.3")

    implementation("com.google.android.gms:play-services-ads:24.1.0")

//    betaImplementation("com.google.firebase:firebase-appdistribution:16.0.0-beta14")


    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.ui.text.google.fonts)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}