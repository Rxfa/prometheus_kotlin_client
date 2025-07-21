plugins {
    kotlin("jvm")
    id("java-library")
    alias(libs.plugins.vanniktech.mavenPublish)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":ktor"))
    implementation(project(":core"))

    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("io.ktor:ktor-server-host-common:3.1.2")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-server-status-pages:3.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    testImplementation(project(":core"))
    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.1.3")
}

kotlin {
    jvmToolchain(21)
}

publishing {
    publications {
        create<MavenPublication>("maven-http") {
            artifactId = "prometheus-http"
            from(components["java"])
        }
    }
}
