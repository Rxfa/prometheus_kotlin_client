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
include(":library")
include(":benchmarks")
include(":ktor")
include(":example")