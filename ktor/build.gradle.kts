plugins {
    kotlin("jvm")
}

group = "org.example"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":library"))

    implementation("io.ktor:ktor-server-core:3.1.2")
    implementation("io.ktor:ktor-server-cio:3.1.2")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:3.1.3")
}

kotlin {
    jvmToolchain(21)
}