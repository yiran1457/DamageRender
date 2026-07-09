package net.yiran.damagerender;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.yiran.damagerender.client.ClientEventHandler;
import net.yiran.damagerender.client.Command;
import net.yiran.damagerender.client.DamageColorManager;
import net.yiran.damagerender.data.DamageInfoBatchPacket;
import net.yiran.damagerender.data.DamageInfoPacket;
import net.yiran.damagerender.data.UpdateConfigPacket;
import net.yiran.damagerender.server.ServerEventHandler;

@Mod(DamageRender.MODID)
public class DamageRender {
    public static final String MODID = "damagerender";
    public static SimpleChannel NETWORK;
    private static final String PROTOCOL_VERSION = "1";

    @SuppressWarnings("removal")
    public DamageRender() {
        NETWORK = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        MinecraftForge.EVENT_BUS.register(ServerEventHandler.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(ClientEventHandler.class);
            MinecraftForge.EVENT_BUS.register(Command.class);
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        DamageColorManager.getInstance().load();

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ClientConfig.SPEC
        );
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        NETWORK.registerMessage(0, DamageInfoPacket.class, DamageInfoPacket::toBytes, DamageInfoPacket::newInstance, DamageInfoPacket::handle);
        NETWORK.registerMessage(1, UpdateConfigPacket.class, UpdateConfigPacket::toBytes, UpdateConfigPacket::newInstance, UpdateConfigPacket::handle);
        NETWORK.registerMessage(2, DamageInfoBatchPacket.class, DamageInfoBatchPacket::toBytes, DamageInfoBatchPacket::newInstance, DamageInfoBatchPacket::handle);
    }
}
