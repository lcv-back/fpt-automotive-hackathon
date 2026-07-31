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
    // Deliberately no runtime dependencies yet. Silero VAD (L3b) needs
    // onnxruntime-android; add it in that commit, not before, and record the
    // source in the README - the submission checklist requires every
    // open-source library outside the AAOS SDK to be credited.
    testImplementation("junit:junit:4.13.2")
}
