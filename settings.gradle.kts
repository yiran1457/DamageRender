pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.parchmentmc.org") { name = "ParchmentMC" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.11"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

stonecutter {
    create(rootProject) {
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) {
                version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
            }
        }

        match("1.19.2", "forge")
        match("1.20.1", "forge")
        match("1.21.1", "neoforge")
        vcsVersion = "1.20.1-forge"
    }
}

rootProject.name = "DamageRender"