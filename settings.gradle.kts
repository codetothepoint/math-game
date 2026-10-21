pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "math-game"

// `core` is pure Kotlin and builds anywhere with a JDK (ADR 0005).
include(":core")

// The Android modules are included only when an Android SDK is configured, so that
// `./gradlew :core:test` works on a machine without one (specs/platform/android/README.md).
val sdkFromEnv = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
val sdkFromLocalProperties = file("local.properties").takeIf { it.exists() }?.readLines()
    ?.firstOrNull { it.trim().startsWith("sdk.dir=") }?.substringAfter("=")?.trim()
val hasAndroidSdk = listOfNotNull(sdkFromEnv, sdkFromLocalProperties).any { file(it).isDirectory }

if (hasAndroidSdk) {
    include(":ui", ":identity", ":engines", ":app")
} else {
    logger.lifecycle("No Android SDK found: configuring only :core. Set ANDROID_HOME or local.properties sdk.dir for the app modules.")
}
