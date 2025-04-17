plugins {
    kotlin("multiplatform") version "2.1.10" apply false
}

group = "nl.w8mr.kasmine"
version = "1.0-SNAPSHOT"

allprojects {
    group = "nl.w8mr.kasmine"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // Kotlin Multiplatform plugin will be applied in each subproject
}
