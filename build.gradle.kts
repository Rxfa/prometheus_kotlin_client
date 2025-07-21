plugins {
    base
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.jmh) apply false
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1" apply false
    id("maven-publish")
}

group = project.findProperty("GROUP_ID") as String
version = project.findProperty("VERSION_NAME") as String

tasks.register("testAll") {
    group = "verification"
    description = "Runs tests on all subprojects"
    dependsOn(subprojects.map { it.tasks.named("test") })
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "maven-publish")

    repositories {
        google()
        mavenCentral()
    }

    publishing {
        publications.withType<MavenPublication>().configureEach {
            groupId = project.group.toString()
            version = project.version.toString()

            pom {
                name = "Prometheus Kotlin client"
                description = "Prometheus client library."
                inceptionYear = "2025"
                url = "https://github.com/Rxfa/prometheus_kotlin_client/"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "Rxfa"
                        name = "Rafael Nicolau"
                        url = "https://github.com/rxfa"
                    }
                    developer {
                        id = "MarioRJ16"
                        name = "Mário Carvalho"
                        url = "https://github.com/MarioRj16"
                    }
                }
                scm {
                    url = "https://github.com/Rxfa/prometheus_kotlin_client"
                    connection = "scm:git:git://github.com:Rxfa/prometheus_kotlin_client.git"
                    developerConnection = "scm:git:ssh://github.com:Rxfa/prometheus_kotlin_client.git"
                }
            }
        }
    }
}
