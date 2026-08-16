pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // A subproject that declared its own repository would resolve artifacts from
    // somewhere this file never named. Refuse it rather than allow it silently.
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "nowplaying"

// :domain is a pure Kotlin/JVM library and :app is the Android application.
// The split is structural rather than conventional: without the Android plugin,
// :domain cannot import an Android type even by accident, and its tests run on
// the JVM in under a second. See CONTRIBUTING.md, "The loop".
include(":app")
include(":domain")
