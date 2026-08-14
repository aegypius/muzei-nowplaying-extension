import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// No Kotlin plugin here; see the root build.gradle.kts for why.
plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.aegypius.muzei.nowplaying"
    compileSdk = libs.versions.compileSdk.get().toInt()

    // Stated explicitly rather than left to AGP, which otherwise selects the
    // highest installed minor platform and the default build-tools. Both must
    // name what the Containerfile installs: platforms;android-37.0 and
    // build-tools;37.0.0.
    compileSdkMinor = libs.versions.compileSdkMinor.get().toInt()
    buildToolsVersion = libs.versions.buildTools.get()

    defaultConfig {
        applicationId = "com.aegypius.muzei.nowplaying"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // Placeholders. version.properties drives both, and wiring it up is
        // ticket 178068 -- do not treat these as the real version.
        versionCode = 1
        versionName = "0.0.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(libs.muzei.api)
}
