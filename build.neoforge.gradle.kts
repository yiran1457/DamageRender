plugins {
    id("net.neoforged.moddev")
    id("neoforge-mutex")
    id("maven-publish")
}

val modId = property("mod_id") as String
val modName = property("mod_name") as String
val modVersion = property("mod_version") as String
val modGroup = property("mod_group_id") as String
val modLicense = property("mod_license") as String

val minecraftVersion = property("minecraft_version") as String
val neoVersion = property("neo_version") as String
val loaderRange = property("loader_version_range") as String
val mcRange = property("minecraft_version_range") as String
val parchmentMc = property("parchment_minecraft_version") as String
val parchmentMappings = property("parchment_mappings_version") as String

version = "$modVersion+$minecraftVersion-neoforge"
group = modGroup
base.archivesName.set("$modName-$minecraftVersion-NeoForge")

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    withSourcesJar()
}

repositories {
    maven("https://cursemaven.com") { name = "CurseMaven" }
}

neoForge {
    version = neoVersion

    parchment {
        mappingsVersion = parchmentMappings
        minecraftVersion = parchmentMc
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
    java {
        exclude("**/DamageColorManager.java")
        exclude("**/DamageNumberRenderer.java")
        exclude("**/Mat4Util.java")
    }
    resources {
        exclude("META-INF/mods.toml")
        exclude("META-INF/accesstransformer.cfg")
        exclude("pack.mcmeta")
        exclude("assets/damagerender/textures/**")
    }
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to mcRange,
        "neo_version" to neoVersion,
        "loader_version_range" to loaderRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_license" to modLicense,
        "mod_version" to modVersion,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}

sourceSets.main.get().resources.srcDir(generateModMetadata)
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