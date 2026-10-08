plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.goraya.videoedition"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.goraya.videoedition"
        minSdk = 24
        targetSdk = 34
        versionCode = 6
        versionName = "0.5.0"
    }
    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "goraya123"
            keyAlias = "goraya"
            keyPassword = System.getenv("KEYSTORE_PASSWORD") ?: "goraya123"
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}
dependencies {
    implementation(project(":video-engine"))
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-transformer:1.4.1")
    implementation("androidx.media3:media3-effect:1.4.1")
    implementation("com.google.guava:guava:33.2.1-android")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
