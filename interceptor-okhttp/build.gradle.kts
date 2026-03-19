import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.maven.publish)
}

val publicationGroup = providers.gradleProperty("GROUP").get()
val publicationVersion = providers.gradleProperty("LIBRARY_VERSION").get()

group = publicationGroup
version = publicationVersion

android {
    namespace = "io.groovin.logmeow.interceptor.okhttp"
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
    api(project(":interceptor-core"))
    compileOnly(libs.okhttp)
}

mavenPublishing {
    publishToMavenCentral()
    // CI environment variable is automatically set by GitHub Actions
    if (providers.environmentVariable("CI").isPresent) {
        signAllPublications()
    }

    coordinates(publicationGroup, "network-interceptor-okhttp", publicationVersion)

    pom {
        name = "LogMeow Interceptor OkHttp"
        description = "OkHttp interceptor for LogMeow network inspector"
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
