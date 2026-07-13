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
//?} else {
/*package net.yiran.damagerender;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.ClientEventHandler;
import net.yiran.damagerender.client.Command;
import net.yiran.damagerender.data.DamageInfoBatchPacket;
import net.yiran.damagerender.data.DamageInfoPacket;
import net.yiran.damagerender.data.UpdateConfigPacket;
import net.yiran.damagerender.server.ServerEventHandler;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

@Mod(DamageRender.MODID)
public class DamageRender {
    public static final String MODID = "damagerender";
    public static Path DAMAGE_COLOR_PATH = FMLPaths.CONFIGDIR.get().resolve("damagerender-damage-color.json");

    public static String getHexColor(int color) {
        return "#" + Integer.toHexString(color).substring(2).toUpperCase();
    }

    public DamageRender(IEventBus modBus, ModContainer mod) throws IOException {
        modBus.addListener(this::onRegisterPayloads);

        mod.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        NeoForge.EVENT_BUS.register(ServerEventHandler.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            NeoForge.EVENT_BUS.register(ClientEventHandler.class);
            NeoForge.EVENT_BUS.register(Command.class);
            // 注册 Mods 列表里的配置界面（仅客户端）
            mod.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        }

        loadDamageColorMap();
    }

         // 加载伤害颜色映射：配置文件存在则读取，否则写入默认值。供主类构造与 Command reload 复用。
     //
    private static void loadDamageColorMap() {
        var manager = ClientDamageInfoManager.getInstance();
        if (Files.exists(DAMAGE_COLOR_PATH)) {
            try {
                var json = JsonParser.parseString(Files.readString(DAMAGE_COLOR_PATH));
                manager.parseAndApply(json);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        // 首次运行：写入默认颜色映射
        JsonObject json = ClientDamageInfoManager.defaultColorJson();
        try (Writer fileWriter = Files.newBufferedWriter(DAMAGE_COLOR_PATH)) {
            JsonWriter jsonWriter = new JsonWriter(fileWriter);
            jsonWriter.setIndent("\t");
            jsonWriter.setSerializeNulls(true);
            jsonWriter.setLenient(true);
            Streams.write(json, jsonWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
        manager.parseAndApply(json);
    }

         // 重新加载伤害颜色映射（Command reload 调用）。
     //
    public static void reloadDamageColorMap() {
        loadDamageColorMap();
    }

    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(DamageRender.MODID)
                .playToClient(DamageInfoPacket.TYPE, DamageInfoPacket.STREAM_CODEC, DamageInfoPacket::handle)
                .playToClient(DamageInfoBatchPacket.TYPE, DamageInfoBatchPacket.STREAM_CODEC, DamageInfoBatchPacket::handle)
                .playToServer(UpdateConfigPacket.TYPE, UpdateConfigPacket.STREAM_CODEC, UpdateConfigPacket::handle);
    }
}
*///?}