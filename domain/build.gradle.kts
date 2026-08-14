import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Pinned to match :app rather than following whichever JDK happens to be
// running: this module's output is a dependency of the Android application.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// No Android plugin here, deliberately: an Android import in this module is a
// compile error rather than a broken convention. Domain logic lives here so its
// tests run on the JVM in under a second.
dependencies {
    // The publish path is a suspend function taking an injected dispatcher, so
    // coroutines are needed in main and not only in tests.
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test.junit5)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform()
}
