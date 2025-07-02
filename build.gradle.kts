plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ktor) apply false
}

repositories {
    google()
    mavenCentral()
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}