plugins {
    id("dev.kikugie.stonecutter")
    id("net.minecraftforge.gradle") version "[6.0,6.2)" apply false
    id("org.parchmentmc.librarian.forgegradle") version "1.+" apply false
    id("net.neoforged.moddev") version "2.0.107" apply false
}

stonecutter active "1.20.1-forge"

stonecutter parameters {
    val parts = current.project.split("-", limit = 2)
    val loader = parts.getOrElse(1) { "forge" }

    constants {
        match(loader, "forge", "neoforge")
    }

    swaps["mod_version"] = "\"${node.project.property("mod_version")}\";"
    swaps["minecraft"] = "\"${node.metadata.version}\";"
}