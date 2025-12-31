import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "io.groovin.logmeow"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.7")

    // Koin for Dependency Injection
    val koinVersion = "3.5.6"
    implementation("io.insert-koin:koin-core:$koinVersion")
    implementation("io.insert-koin:koin-compose:1.1.5") // koin-compose has its own versioning
}

kotlin {
    jvmToolchain(17)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LogMeow"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("src/main/resources/logmeow.icns"))

                // 다크 모드 + 환경변수 전달
                jvmArgs(
                    "-Dapple.awt.application.appearance=NSAppearanceNameDarkAqua"
                )

                // ANDROID_HOME 환경변수 전달 (옵션)
                val androidHome = System.getenv("ANDROID_HOME")
                if (androidHome != null) {
                    jvmArgs("-DANDROID_HOME=$androidHome")
                }
            }
            windows {
                iconFile.set(project.file("src/main/resources/logmeow.ico"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/logmeow.png"))
            }
        }
    }
}
