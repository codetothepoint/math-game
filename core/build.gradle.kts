// core: pure Kotlin, no Android (ADR 0005). Curriculum, mastery model, scheduler, generators,
// engine logic, data model, stats. Builds and tests with a JDK alone.
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(true)
    }
}

// The curriculum and recipes have one source: specs/. They are synced into core's resources at
// build time so the JSON the game reads is exactly what tools/curriculum.py and tools/recipes.py
// validated (specs/build-plan.md M0).
val syncSpecs by tasks.registering(Sync::class) {
    description = "Copies generated spec data from specs/ into core resources"
    from(rootProject.file("specs/curriculum/generated/curriculum.json")) { into("curriculum") }
    from(rootProject.file("specs/mechanics/recipes/generated")) { into("recipes") }
    into(layout.buildDirectory.dir("spec-resources"))
}

sourceSets {
    main {
        resources.srcDir(syncSpecs)
    }
}

dependencies {
    implementation(libs.gson)

    testImplementation(kotlin("test"))
    testImplementation(platform(libs.junit.bom))
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "failed", "skipped")
        showStandardStreams = false
    }
}
