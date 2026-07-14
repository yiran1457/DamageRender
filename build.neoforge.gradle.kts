import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    id("net.neoforged.moddev")
    id("maven-publish")
}

val modId = project.property("mod_id").toString()
val modName = project.property("mod_name").toString()
val modVersion = project.property("mod_version").toString()
val modGroupId = project.property("mod_group_id").toString()
val modLicense = project.property("mod_license").toString()

val targetMinecraftVersion = project.property("minecraft_version").toString()
val neoVersion = project.property("neo_version").toString()
val loaderVersionRange = project.findProperty("loader_version_range")?.toString()
val minecraftVersionRange = project.property("minecraft_version_range").toString()
val parchmentMinecraftVersion = project.findProperty("parchment_minecraft_version")?.toString()
val parchmentMappingsVersion = project.findProperty("parchment_mappings_version")?.toString()
val javaVersion = project.findProperty("java_version")?.toString()?.toInt() ?: 21
val metadataTemplateDirectory = project.findProperty("metadata_template_dir")?.toString() ?: "src/main/templates"

version = modVersion
group = modGroupId

base {
    archivesName.set("$modName-$targetMinecraftVersion-NeoForge")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion))
    withSourcesJar()
}

repositories {
    maven {
        name = "CurseMaven"
        url = uri("https://cursemaven.com")
    }
}

neoForge {
    version = neoVersion

    if (parchmentMinecraftVersion != null && parchmentMappingsVersion != null) {
        parchment {
            mappingsVersion.set(parchmentMappingsVersion)
            minecraftVersion.set(parchmentMinecraftVersion)
        }
    }

    runs {
        register("client") {
            client()
            gameDirectory = file("run")
        }
        register("server") {
            server()
            gameDirectory = file("run")
            programArgument("--nogui")
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

sourceSets.named("main") {
    resources {
        exclude("META-INF/mods.toml")
        exclude("META-INF/accesstransformer.cfg")
        exclude("pack.mcmeta")
    }
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mutableMapOf(
        "minecraft_version" to targetMinecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
    )
    loaderVersionRange?.let { replaceProperties["loader_version_range"] = it }
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.layout.projectDirectory.dir(metadataTemplateDirectory))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

sourceSets.named("main") {
    resources.srcDir(generateModMetadata)
}
neoForge.ideSyncTask(generateModMetadata)

tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.register<Copy>("buildAndCollect") {
    group = "build"
    description = "Builds mod jars and copies results to root build/libs/{mod version}/"
    dependsOn("build")
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(rootProject.layout.buildDirectory.dir("libs/$modVersion"))
}
