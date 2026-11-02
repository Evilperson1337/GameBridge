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

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
        }
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
}
