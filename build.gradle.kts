import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

group = providers.gradleProperty("GROUP").get()

repositories {
    mavenCentral()
    google()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(project(":interceptor-api"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.collections.immutable)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.koin.core)
    implementation(libs.koin.compose)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

val desktopAppVersion = providers.gradleProperty("DESKTOP_APP_VERSION").get()

tasks.named<ProcessResources>("processResources") {
    val versionProps = mapOf("desktop.app.version" to desktopAppVersion)
    inputs.properties(versionProps)
    filesMatching("version.properties") {
        expand(versionProps)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "LogMeow"
            packageVersion = providers.gradleProperty("DESKTOP_APP_VERSION").get()

            macOS {
                iconFile.set(project.file("src/main/resources/logmeow.icns"))
                jvmArgs("-Dapple.awt.application.appearance=NSAppearanceNameDarkAqua")

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
