pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "prometheus-kotlin-client"
include(":benchmarks")
include(":core")
include(":ktor")
include(":ktorExample")
include(":http")
