pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.10"
}

stonecutter {
    centralScript = "build.gradle.kts"
    create(rootProject) {
        vers("1.21.1-fabric", "1.21.1")
        vers("1.21.1-neoforge", "1.21.1")
        vcsVersion = "1.21.1-neoforge"
    }
}
