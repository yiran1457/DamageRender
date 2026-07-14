package net.yiran.damagerender;

//? if forge {
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
//?} else {
/*import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
*///?}

/** Version bridge for the FML environment API used to detect the physical client. */
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
