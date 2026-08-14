import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// No Kotlin plugin here; see the root build.gradle.kts for why.
plugins {
    alias(libs.plugins.android.application)
}

// version.properties is the single source of both halves of the version, and the
// two are maintained in opposite ways: `name` is generated output written by
// cocogitto's pre-bump hook, `codeEpoch` is hand-written once and must never be
// raised. See docs/adr/0005-elapsed-seconds-version-code.md.
// Read through providers.fileContents so the file is a tracked configuration
// input rather than an untracked side read.
val versionPropertiesText: String = providers.fileContents(
    rootProject.layout.projectDirectory.file("version.properties"),
).asText.get()
val versionProperties = Properties().apply { load(versionPropertiesText.reader()) }

// Checked for blankness, not just absence: cog's pre-bump hook substitutes this
// line with sed, and a failed substitution leaves `name = `, which would ship a
// versionName of "-10889" rather than failing.
val semanticVersion: String = versionProperties.getProperty("name")?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: error("version.properties has a missing or empty `name`")
val codeEpochSeconds: Long = (
    versionProperties.getProperty("codeEpoch")?.trim()
        ?: error("version.properties is missing `codeEpoch`")
    ).let { raw ->
    raw.toLongOrNull() ?: error("version.properties has a non-numeric `codeEpoch`: \"$raw\"")
}

/**
 * The build time, as a value source rather than a bare clock read.
 *
 * Gradle treats a value source as something it must re-check, so with the
 * configuration cache enabled a changed timestamp invalidates the entry and
 * configuration re-runs. Reading System.currentTimeMillis() directly would
 * instead be baked into the cache, and every later build would ship an
 * identical, stale versionCode — destroying the property that every build is
 * distinguishable, which is the whole point of ADR-0005.
 *
 * The cost is that this build cannot reuse a configuration cache entry across
 * builds. That is the correct trade: a stale versionCode is silent and breaks
 * installs, whereas re-running configuration is merely slower.
 */
abstract class BuildTimeSeconds : ValueSource<Long, ValueSourceParameters.None> {
    override fun obtain(): Long = System.currentTimeMillis() / 1000
}

// Resolved once, here, so that both halves of the version describe the same
// instant and neither a Provider nor a script-capturing lambda is serialized
// into a task. Wiring providers into the variant outputs instead made five AGP
// tasks hold references to this build script, which the configuration cache
// cannot serialize.
val computedVersionCode: Long = providers.of(BuildTimeSeconds::class) {}.get() - codeEpochSeconds

require(computedVersionCode > 0) {
    "codeEpoch ($codeEpochSeconds) is in the future; versionCode would be negative"
}
require(computedVersionCode <= Int.MAX_VALUE) {
    "elapsed seconds ($computedVersionCode) exceed the 32-bit versionCode range"
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
        // Both halves describe the same instant, so the manifest and Android's
        // app info always agree. Carrying the same string into the APK filename
        // is still to come: nothing sets outputFileName yet, so AGP emits
        // app-release.apk. That is ticket a1c076, which ADR-0004's Obtainium
        // version regex depends on.
        versionCode = computedVersionCode.toInt()
        versionName = "$semanticVersion-$computedVersionCode"
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
