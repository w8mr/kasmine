plugins {
    kotlin("multiplatform") version "2.4.0" apply false
    id("com.vanniktech.maven.publish") version "0.37.0" apply false
    id("com.ncorti.ktfmt.gradle") version "0.26.0" apply false
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

rootProject.plugins.withType<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin> {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension>().apply {
        resolution("serialize-javascript", "7.0.6")
        resolution("webpack", "5.104.1")
        resolution("diff", "8.0.3")
    }
}
