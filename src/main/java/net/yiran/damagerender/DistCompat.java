package net.yiran.damagerender;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
*///?}

/** 兼容不同 FML 环境 API 的客户端侧判断。 */
public final class DistCompat {
    private DistCompat() {
    }

    public static boolean isClient() {
//? if forge {
        return FMLEnvironment.dist == Dist.CLIENT;
//?}
//? if neoforge && =1.21.1 {
        /*return FMLEnvironment.dist == Dist.CLIENT;
*///?}
//? if neoforge && >1.21.1 {
        /*return FMLEnvironment.getDist() == Dist.CLIENT;
*///?}
    }
}
