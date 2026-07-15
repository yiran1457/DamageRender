package net.yiran.damagerender.data;

import net.minecraft.resources.ResourceKey;

/** 兼容 Minecraft 26.1 将 ResourceLocation 更名为 Identifier 的改动。 */
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
