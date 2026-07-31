// Android library module :voice - Long's voice pipeline (VAD, ASR client,
// intent router, TTS, latency trace).
//
// It is a LIBRARY, not an app: Duong owns the app shell (D1) and includes
// this module. That split is what lets the trace and audio logic be unit
// tested on a plain JVM while the shell is still being built - see
// android/voice/README.md.
plugins {
    id("com.android.library")
}

android {
    namespace = "com.viva.voice"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 29 // AAOS / Android 10, the CarSky Device baseline
        consumerProguardFiles("consumer-rules.pro")

        // 03-contracts.md §2: "Config trong app doc tu BuildConfig.ASR_BASE_URL
        // - khong hard-code". Overridden per build; PA-2 (adb reverse) is the
        // default because it works before the Container Node exists.
        buildConfigField(
            "String",
            "ASR_BASE_URL",
            "\"" + (project.findProperty("vivaAsrBaseUrl") ?: "http://127.0.0.1:8080") + "\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
    }
}

dependencies {
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.20.0")
    testImplementation("junit:junit:4.13.2")
}
