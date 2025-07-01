plugins {
    kotlin("jvm") version "1.8.22" apply false
    kotlin("multiplatform") version "1.8.22" apply false
    kotlin("android") version "1.8.22" apply false
    kotlin("plugin.serialization") version "1.8.22" apply false
    id("com.android.application") version "8.0.2" apply false
    id("com.android.library") version "8.0.2" apply false
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