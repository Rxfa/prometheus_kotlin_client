plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jmh) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs tests on all subprojects"
    dependsOn(subprojects.map { it.tasks.named("test") })
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        google()
        mavenCentral()
    }
}
