// Plugins are declared here and applied in the modules that need them.
//
// There is no Kotlin Android plugin: AGP 9.0+ applies Kotlin itself, and
// declaring org.jetbrains.kotlin.android alongside it fails the build outright.
// See https://kotl.in/gradle/agp-built-in-kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.jvm) apply false
}
