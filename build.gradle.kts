// Declaring the plugins here (apply false) puts Kotlin Gradle Plugin 2.4.21 on the root build
// classpath, so AGP's built-in Kotlin support in :app and Kotlin JVM in :core share one version.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ktlint) apply false
}

val ktlintVersion = libs.versions.ktlint.get()

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set(ktlintVersion)
    }
}
