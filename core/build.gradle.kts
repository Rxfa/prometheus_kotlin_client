import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    id("com.vanniktech.maven.publish") version "0.34.0"
}

group = "io.github.rxfa"
version = "0.0.1"

kotlin {
    explicitApi()
    jvm()
    androidTarget {
        publishLibraryVariants("release")
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    linuxX64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                implementation("org.jetbrains.kotlinx:atomicfu:0.22.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            }
        }
        val jvmMain by getting {
        }
        val jvmTest by getting {
        }
    }
}

android {
    namespace = "org.jetbrains.kotlinx.multiplatform.library.template"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), "prometheus-core", version.toString())

    pom {
        name = "Prometheus Core"
        description = "Core Prometheus metrics collection logic."
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
