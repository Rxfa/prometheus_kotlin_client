plugins {
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply  false
    alias(libs.plugins.vanniktech.mavenPublish) apply false
}

tasks.register("testAll") {
    group = "verification"
    description = "Runs tests on all subprojects"
    dependsOn(subprojects.map { it.tasks.named("test") })
}