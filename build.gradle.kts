plugins {
    kotlin("multiplatform") version "2.4.0" apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
}

group = "nl.w8mr.kasmine"
version = "0.0.5"

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.multiplatform")
}
