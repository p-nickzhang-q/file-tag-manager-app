import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

group = "com.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}
//
//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>() {
//    kotlinOptions.jvmTarget = "17"
//}

dependencies {
    // Note, if you develop a library, you should use compose.desktop.common.
    // compose.desktop.currentOs should be used in launcher-sourceSet
    // (in a separate module for demo project and in testMain).
    // With compose.desktop.common you will also lose @Preview functionality
    implementation(compose.desktop.currentOs)
    implementation(compose.desktop.windows_x64)
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")
    implementation("cn.hutool:hutool-all:5.8.26")

}

compose.desktop {
    application {
        mainClass = "FileTagMainKt"
//        buildTypes.release.proguard {
//            isEnabled.set(false)
//        }
        nativeDistributions {
            // 有些包,如果通过反射用到的,不会被打包进去,所以需要显示申明一下
            modules("java.sql")
            targetFormats(TargetFormat.Exe, TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "TagManager"
            packageVersion = "1.0.0"
            windows {
                menuGroup = "TagManager"
                description = "TagManager"
                vendor = "zhang yi da"
            }
        }
    }
}
