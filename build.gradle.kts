plugins {
    java
    `maven-publish`
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.140" apply false
    id("fabric-loom") version "1.9-SNAPSHOT" apply false
}

val modId: String by project
val modName: String by project
val modVersion: String by project
val modGroupId: String by project
val modAuthors: String by project
val modDescription: String by project
val modLicense: String by project
val minecraftVersion: String by project

val loader = project.name.substringAfterLast('-')

version = modVersion
group = modGroupId
base.archivesName.set(modId)

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    maven("https://maven.parchmentmc.org/")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.terraformersmc.com/")
}

sourceSets {
    main {
        resources.srcDir("src/generated/resources")
    }
}

// apply loader-specific build logic (Groovy scripts for dynamic typing)
when (loader) {
    "neoforge" -> apply(from = rootProject.file("neoforge.gradle"))
    "fabric" -> apply(from = rootProject.file("fabric.gradle"))
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri("file://${project.projectDir}/repo")
        }
    }
}
