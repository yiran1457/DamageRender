package net.yiran.damagerender.data;

import net.minecraft.resources.ResourceKey;

/** Version bridge for the ResourceLocation-to-Identifier rename in Minecraft 26.1. */
public final class ResourceKeyCompat {
    private ResourceKeyCompat() {
    }

    public static String identifierString(ResourceKey<?> key) {
//? if >1.21.1 {
        /*return key.identifier().toString();
*///?} else {
        return key.location().toString();
//?}
    }
}
