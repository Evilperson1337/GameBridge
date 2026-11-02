plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.allourmedia.gamebridge"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.allourmedia.gamebridge"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Signing reads from environment variables only. When any is missing the
    // release build stays unsigned, so `./gradlew check` works for forks.
    val signingStoreFile = System.getenv("GAMEBRIDGE_SIGNING_STORE_FILE")
    val signingStorePassword = System.getenv("GAMEBRIDGE_SIGNING_STORE_PASSWORD")
    val signingKeyAlias = System.getenv("GAMEBRIDGE_SIGNING_KEY_ALIAS")
    val signingKeyPassword = System.getenv("GAMEBRIDGE_SIGNING_KEY_PASSWORD")
    val signingValues =
        listOf(
            signingStoreFile,
            signingStorePassword,
            signingKeyAlias,
            signingKeyPassword,
        )
    val hasSigning = signingValues.all { !it.isNullOrBlank() }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                storeFile = file(signingStoreFile!!)
                storePassword = signingStorePassword
                keyAlias = signingKeyAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            if (hasSigning) signingConfig = signingConfigs.getByName("release")
        }
    }

    sourceSets {
        // Shortcut fixtures shared with the :core JVM tests.
        getByName("androidTest").assets.directories.add("../fixtures")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        managedDevices {
            localDevices {
                create("pixel2api33") {
                    device = "Pixel 2"
                    apiLevel = 33
                    systemImageSource = "google"
                    testedAbi = "x86_64"
                }
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.espresso.intents)
}
