plugins {
    id("dev.kikugie.stonecutter")
    id("net.neoforged.moddev") version "2.0.141" apply false
    id("net.neoforged.moddev.legacyforge") version "2.0.141" apply false
}

stonecutter.active("1.20.1-forge")

stonecutter.parameters {
    val loader = current.project.substringAfter('-', "forge")

    constants {
        match(loader, "forge", "neoforge")
    }

    swaps {
        put("mod_version", "\"${node.project.property("mod_version")}\";")
        put("minecraft", "\"${node.metadata.version}\";")
    }
}
