// app: the Android application. Navigation, DI, lifecycle, notifications, persistence adapters.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.codetothepoint.mathgame"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.codetothepoint.mathgame"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

// The app reads the same spec data core does (specs/build-plan.md M0): sync it into assets.
val syncSpecAssets by tasks.registering(Sync::class) {
    from(rootProject.file("specs/curriculum/generated/curriculum.json")) { into("curriculum") }
    from(rootProject.file("specs/mechanics/recipes/generated")) { into("recipes") }
    into(layout.buildDirectory.dir("spec-assets"))
}
android.sourceSets["main"].assets.srcDir(syncSpecAssets)

dependencies {
    implementation(project(":core"))
    implementation(project(":ui"))
    implementation(project(":engines"))
    implementation(project(":identity"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
