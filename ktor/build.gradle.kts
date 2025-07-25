plugins {
    kotlin("jvm")
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "io.github.rxfa"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":core"))

    implementation("io.ktor:ktor-server-core:3.1.2")
    implementation("io.ktor:ktor-server-cio:3.1.2")
    implementation("io.ktor:ktor-server-host-common:3.1.2")
    implementation("io.ktor:ktor-server-status-pages:3.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    testImplementation(project(":core"))
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.1.3")
}

kotlin {
    jvmToolchain(21)
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "prometheus-ktor", version.toString())

    pom {
        name = "Prometheus Ktor"
        description = "Ktor integration for Prometheus metrics."
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
                url = "https://github.com/Rxfa"
            }
            developer {
                id = "MarioRJ16"
                name = "Mário Carvalho"
                url = "https://github.com/marioRj16/"
            }
        }
        scm {
            url = "https://github.com/Rxfa/prometheus_kotlin_client"
            connection = "scm:git:git://github.com/Rxfa/prometheus_kotlin_client.git"
            developerConnection = "scm:git:ssh://git@github.com/Rxfa/prometheus_kotlin_client.git"
        }
    }
}
