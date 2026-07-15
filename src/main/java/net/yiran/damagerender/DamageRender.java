//? if forge {
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
    // Damage batches use a type dictionary and float payloads as of protocol 2.
    private static final String PROTOCOL_VERSION = "2";

    @SuppressWarnings("removal")
    public DamageRender() {
        NETWORK = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        MinecraftForge.EVENT_BUS.register(ServerEventHandler.class);
        if (DistCompat.isClient()) {
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
//?} else {
/*package net.yiran.damagerender;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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

    public DamageRender(IEventBus modBus, ModContainer mod) {
        modBus.addListener(this::onRegisterPayloads);

        mod.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        NeoForge.EVENT_BUS.register(ServerEventHandler.class);
        if (DistCompat.isClient()) {
            NeoForge.EVENT_BUS.register(ClientEventHandler.class);
            NeoForge.EVENT_BUS.register(Command.class);
            // 注册 Mods 列表里的配置界面（仅客户端）
            mod.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
            // 颜色映射加载统一委托 DamageColorManager（与 Forge 版共用同一份实现）
            DamageColorManager.getInstance().load();
        }
    }

    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(DamageRender.MODID)
                .playToClient(DamageInfoPacket.TYPE, DamageInfoPacket.STREAM_CODEC, DamageInfoPacket::handle)
                .playToClient(DamageInfoBatchPacket.TYPE, DamageInfoBatchPacket.STREAM_CODEC, DamageInfoBatchPacket::handle)
                .playToServer(UpdateConfigPacket.TYPE, UpdateConfigPacket.STREAM_CODEC, UpdateConfigPacket::handle);
    }
}
*///?}
