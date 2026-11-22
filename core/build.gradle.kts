import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// JDK 21 runs the build; bytecode targets Java 17.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.junit)
}

// BridgeFailureTest and VerificationTest read the README, so a README edit must rerun them.
tasks.test {
    inputs.file(rootProject.file("README.md")).withPropertyName("readme")
}
