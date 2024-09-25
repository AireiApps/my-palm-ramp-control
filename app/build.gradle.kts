plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.airei.milltracking.mypalm.mqtt.lrc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.airei.milltracking.mypalm.mqtt.lrc"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        setProperty("archivesBaseName", "my_palm_mqtt-($versionName)")
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    viewBinding{
        enable = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.media3.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.localbroadcastmanager)

    // Security
    implementation(libs.androidx.security.crypto)
    // Mqtt
    implementation (libs.org.eclipse.paho.client.mqttv3)
    implementation (libs.org.eclipse.paho.android.service)
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.android)
    // Navigate
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui.ktx)
    // Hilt
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.android)
    // GSON
    implementation(libs.gson)
    // RoomDb
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.runtime)

    // Exo
    //implementation("androidx.media3:media3-ui:1.4.0")
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.rtsp)

    // Lottie
    implementation(libs.lottie)

    // Glide
    implementation (libs.glide)
    implementation (libs.gifdecoder)
    implementation (libs.android.gif.drawable)


}