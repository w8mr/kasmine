import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    kotlin("multiplatform") version "2.1.10" apply false
    id("com.vanniktech.maven.publish") version "0.31.0"
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
    apply(plugin = "com.vanniktech.maven.publish")

    mavenPublishing {
        configure(
            KotlinMultiplatform(
                javadocJar = JavadocJar.Empty()
            )
        )
        publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

        coordinates("nl.w8mr.kasmine", "core", "0.0.5")

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
}
