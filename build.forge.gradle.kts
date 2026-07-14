import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("idea")
    id("maven-publish")
    id("net.neoforged.moddev.legacyforge")
}

val modId = project.property("mod_id").toString()
val modName = project.property("mod_name").toString()
val modVersion = project.property("mod_version").toString()
val modGroupId = project.property("mod_group_id").toString()
val modAuthors = project.property("mod_authors").toString()
val modLicense = project.property("mod_license").toString()
val modDescription = project.property("mod_description").toString()

val targetMinecraftVersion = project.property("minecraft_version").toString()
val forgeVersion = project.property("forge_version").toString()
val forgeVersionRange = project.property("forge_version_range").toString()
val loaderVersionRange = project.property("loader_version_range").toString()
val minecraftVersionRange = project.property("minecraft_version_range").toString()
val parchmentMinecraftVersion = project.property("parchment_minecraft_version").toString()
val parchmentMappingsVersion = project.property("parchment_mappings_version").toString()

version = modVersion
group = modGroupId

base {
    archivesName.set("$modName-$targetMinecraftVersion-Forge")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
    }
}
legacyForge {
    version = "$targetMinecraftVersion-$forgeVersion"

    parchment {
        mappingsVersion.set(parchmentMappingsVersion)
        minecraftVersion.set(parchmentMinecraftVersion)
    }

    runs {
        register("client") {
            client()
            gameDirectory = file("run")
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }

        register("server") {
            server()
            gameDirectory = file("run")
            programArgument("--nogui")
            systemProperty("forge.enabledGameTestNamespaces", modId)
        }

        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "minecraft_version" to targetMinecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "forge_version" to forgeVersion,
        "forge_version_range" to forgeVersionRange,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
    )
    inputs.properties(replaceProperties)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties)
    }
    exclude("META-INF/neoforge.mods.toml")
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            mapOf(
                "Specification-Title" to modId,
                "Specification-Vendor" to modAuthors,
                "Specification-Version" to "1",
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
                "Implementation-Vendor" to modAuthors,
            ),
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds mod jars and copies results to root build/libs/{mod version}/"
    dependsOn("build")
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
