
plugins {
    kotlin("jvm")
    id("me.champeau.jmh") version "0.7.2"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":library"))
    implementation(libs.jmh.core)
    annotationProcessor(libs.jmh.generator)
    implementation(libs.prometheus.client)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
}

kotlin {
    jvmToolchain(21)
}

jmh {
    warmupIterations.set(5)
    iterations.set(10)
    fork.set(1)
    benchmarkMode.set(listOf("Throughput"))
    timeOnIteration.set("1s")
    resultFormat.set("JSON")
}

