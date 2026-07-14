pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.minecraftforge.net/") {
            name = "MinecraftForge"
        }
        maven("https://maven.neoforged.net/releases/") {
            name = "NeoForged"
        }
        maven ("https://maven.parchmentmc.org"){
            name = "ParchmentMC"
        }
        maven("https://maven.kikugie.dev/releases") {
            name = "KikuGie Releases"
        }
        maven ("https://maven.kikugie.dev/snapshots"){
            name = "KikuGie Snapshots"
        }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.6"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter.create(rootProject) {
    mapBuilds { _, node ->
        val separator = node.project.lastIndexOf('-')
        val loader = if (separator >= 0) node.project.substring(separator + 1) else "forge"
        "build.$loader.gradle.kts"
    }

    versions(
        mapOf(
            "1.19.2-forge" to "1.19.2",
            "1.20.1-forge" to "1.20.1",
            "1.21.1-neoforge" to "1.21.1",
            "26.1.2-neoforge" to "26.1.2",
        ),
    )
    vcsVersion.set("1.20.1-forge")
}

rootProject.name = "DamageRender"
