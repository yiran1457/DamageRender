plugins {
    id("idea")
    id("maven-publish")
    id("net.minecraftforge.gradle")
    id("org.parchmentmc.librarian.forgegradle")
}

val mod_id: String by project
val mod_name: String by project
val mod_version: String by project
val mod_group_id: String by project
val mod_authors: String by project
val mod_license: String by project
val mod_description: String by project

val minecraft_version: String by project
val forge_version: String by project
val forge_version_range: String by project
val loader_version_range: String by project
val minecraft_version_range: String by project
val mapping_channel: String by project
val mapping_version: String by project

version = "$mod_version+$minecraft_version-forge"
group = mod_group_id
base {
    archivesName.set("$mod_name-$minecraft_version-Forge")
}

java.toolchain.languageVersion.set(JavaLanguageVersion.of(17))

minecraft {
    mappings(mapping_channel, mapping_version)
    copyIdeResources = true

    accessTransformer(file("src/main/resources/META-INF/accesstransformer.cfg"))

    runs {
        configureEach {
            workingDirectory(project.file("run"))
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")

            mods {
                create(mod_id) {
                    source(sourceSets.main.get())
                }
            }
        }

        create("client") {
            property("forge.enabledGameTestNamespaces", mod_id)
        }

        create("server") {
            property("forge.enabledGameTestNamespaces", mod_id)
            args("--nogui")
        }
    }
}

repositories {
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
    }
}

dependencies {
    minecraft("net.minecraftforge:forge:${minecraft_version}-${forge_version}")
}

tasks.named<ProcessResources>("processResources").configure {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "forge_version" to forge_version,
        "forge_version_range" to forge_version_range,
        "loader_version_range" to loader_version_range,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_license" to mod_license,
        "mod_version" to mod_version,
        "mod_authors" to mod_authors,
        "mod_description" to mod_description,
    )
    inputs.properties(replaceProperties)
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties)
    }
    exclude("META-INF/neoforge.mods.toml")
}

tasks.named<Jar>("jar").configure {
    manifest {
        attributes(
            "Specification-Title" to mod_id,
            "Specification-Vendor" to mod_authors,
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to mod_authors,
        )
    }
    finalizedBy("reobfJar")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.named("compileJava") {
    dependsOn("stonecutterGenerate")
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds mod jars and copies results to root build/libs/{mod version}/"
    dependsOn("build")
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$mod_version"))
}