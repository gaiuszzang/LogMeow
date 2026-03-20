import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.maven.publish)
}

val publicationGroup = providers.gradleProperty("GROUP").get()
val publicationVersion = providers.gradleProperty("LIBRARY_VERSION").get()

group = publicationGroup
version = publicationVersion

android {
    namespace = "io.groovin.logmeow.interceptor.core"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

dependencies {
    api(project(":interceptor-api"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

mavenPublishing {
    coordinates(publicationGroup, "network-interceptor-core", publicationVersion)
    publishToMavenCentral()
    // CI environment variable is automatically set by GitHub Actions
    if (providers.environmentVariable("CI").isPresent) {
        signAllPublications()
    }

    pom {
        name = "LogMeow Interceptor Core"
        description = "Core module for LogMeow network interceptor"
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
