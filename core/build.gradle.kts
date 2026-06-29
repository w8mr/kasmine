plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish")
    id("com.ncorti.ktfmt.gradle")
    id("org.jlleitschuh.gradle.ktlint")
    id("dev.detekt")
}

group = parent?.group ?: group

version = parent?.version ?: version

ktfmt { kotlinLangStyle() }

detekt {
    source.setFrom(
        files(
            "src/jvmMain/kotlin",
            "src/commonMain/kotlin",
            "src/jvmTest/kotlin",
            "src/commonTest/kotlin",
        )
    )
    config.setFrom("$rootDir/config/detekt.yml")
    buildUponDefaultConfig = true
}

kotlin {
    jvm { java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } } }
    js {
        browser()
        nodejs()
    }

    sourceSets {
        val commonMain by getting { dependencies { implementation(kotlin("stdlib-common")) } }
        val commonTest by getting { dependencies { implementation(kotlin("test")) } }
        val jvmMain by getting { dependencies { implementation(kotlin("stdlib")) } }
        val jvmTest by getting { dependencies { implementation(kotlin("test")) } }
    }

    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_1)
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
    compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) }
}

tasks.withType<Test> { useJUnitPlatform() }

mavenPublishing {
    configure(
        com.vanniktech.maven.publish.KotlinMultiplatform(
            javadocJar = com.vanniktech.maven.publish.JavadocJar.Empty()
        )
    )
    publishToMavenCentral()

    coordinates("nl.w8mr.kasmine", "core", "${version}")

    pom {
        name.set("Kasmine Core")
        description.set("A library for writing JVM bytecode in Kotlin")
        inceptionYear.set("2023")
        url.set("https://github.com/w8mr/kasmine")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/license/mit")
                distribution.set("https://opensource.org/license/mit")
            }
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/w8mr/kasmine/issues")
        }
        developers {
            developer {
                id.set("w8mr")
                name.set("Elmar Wachtmeester")
                url.set("https://github.com/w8mr")
            }
        }
        scm {
            url.set("https://github.com/w8mr/kasmine")
            connection.set("scm:git:git://github.com/w8mr/kasmine.git")
            developerConnection.set("scm:git:ssh://git@github.com/w8mr/kasmine.git")
        }
    }
    signAllPublications()
}
