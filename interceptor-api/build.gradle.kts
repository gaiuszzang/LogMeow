import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

val publicationGroup = providers.gradleProperty("GROUP").get()
val publicationVersion = providers.gradleProperty("LIBRARY_VERSION").get()

group = publicationGroup
version = publicationVersion

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
}

mavenPublishing {
    coordinates(publicationGroup, "network-interceptor-api", publicationVersion)
    publishToMavenCentral()
    // CI environment variable is automatically set by GitHub Actions
    if (providers.environmentVariable("CI").isPresent) {
        signAllPublications()
    }

    pom {
        name = "LogMeow Interceptor API"
        description = "Shared API contract for LogMeow network interceptor"
        url = "https://github.com/gaiuszzang/LogMeow"
        licenses {
            license {
                name = "Apache License 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "gaiuszzang"
                name = "gaiuszzang"
                url = "https://github.com/gaiuszzang"
            }
        }
        scm {
            url = "https://github.com/gaiuszzang/LogMeow"
            connection = "scm:git:git://github.com/gaiuszzang/LogMeow.git"
            developerConnection = "scm:git:ssh://git@github.com/gaiuszzang/LogMeow.git"
        }
    }
}
