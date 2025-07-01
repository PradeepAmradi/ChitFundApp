plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm {
        jvmToolchain(8)
        withJava()
    }
    
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(libs.ktor.server.core)
                implementation(libs.ktor.server.netty)
                implementation(libs.ktor.server.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.server.cors)
                implementation(libs.ktor.server.auth)
                implementation(libs.ktor.server.auth.jwt)
                implementation(libs.exposed.core)
                implementation(libs.exposed.dao)
                implementation(libs.exposed.jdbc)
                implementation(libs.exposed.java.time)
                implementation(libs.postgresql)
                implementation(libs.logback)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.ktor.server.tests)
                implementation(libs.kotlin.test.junit)
            }
        }
    }
}

application {
    mainClass.set("com.chitfund.backend.ApplicationKt")
}

ktor {
    fatJar {
        archiveFileName.set("chitfund-backend.jar")
    }
}