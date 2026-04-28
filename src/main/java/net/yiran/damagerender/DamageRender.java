package net.yiran.damagerender;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.ClientEventHandler;
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
    public static SimpleChannel NETWORK;
    private static final String PROTOCOL_VERSION = "1";

    public static String getHexColor(int color) {
        return "#" + Integer.toHexString(color).substring(2).toUpperCase();
    }

    @SuppressWarnings("removal")
    public DamageRender() throws IOException {
        NETWORK = NetworkRegistry.newSimpleChannel(
                new ResourceLocation(MODID, "main"),
                () -> PROTOCOL_VERSION,
                PROTOCOL_VERSION::equals,
                PROTOCOL_VERSION::equals
        );
        MinecraftForge.EVENT_BUS.register(ServerEventHandler.class);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            MinecraftForge.EVENT_BUS.register(ClientEventHandler.class);
            FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        }
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);

        if (Files.exists(DAMAGE_COLOR_PATH)) {
            var json = JsonParser.parseString(Files.readString(DAMAGE_COLOR_PATH));
            ClientDamageInfoManager.getInstance().setDamageColorMap(ClientDamageInfoManager.COLOR_CODEC.parse(JsonOps.INSTANCE, json).result().get());
        } else {
            var json = new JsonObject();
            json.addProperty("magic", getHexColor(-7722014));
            json.addProperty("lightningBolt", getHexColor(-256));
            json.addProperty("lava", getHexColor(-65536));
            json.addProperty("indirectMagic", getHexColor(-7722014));
            json.addProperty("freeze", getHexColor(-16711681));
            json.addProperty("witherSkull", getHexColor(-14221237));
            json.addProperty("inFire", getHexColor(-65536));
            json.addProperty("onFire", getHexColor(-65536));
            json.addProperty("wither", getHexColor(-14221237));
            json.addProperty("heal", "#00FF00");

            try (Writer fileWriter = Files.newBufferedWriter(DAMAGE_COLOR_PATH)) {
                JsonWriter jsonWriter = new JsonWriter(fileWriter);
                jsonWriter.setIndent("\t");
                jsonWriter.setSerializeNulls(true);
                jsonWriter.setLenient(true);
                Streams.write(json, jsonWriter);
            }
            ClientDamageInfoManager.getInstance().setDamageColorMap(ClientDamageInfoManager.COLOR_CODEC.parse(JsonOps.INSTANCE, json).result().get());

        }

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ClientConfig.SPEC
        );
    }

    public void onCommonSetup(FMLCommonSetupEvent event) {
        NETWORK.registerMessage(0, DamageInfoPacket.class, DamageInfoPacket::toBytes, DamageInfoPacket::newInstance, DamageInfoPacket::handle);
        NETWORK.registerMessage(1, UpdateConfigPacket.class, UpdateConfigPacket::toBytes, UpdateConfigPacket::newInstance, UpdateConfigPacket::handle);
    }

    public void onClientSetup(FMLClientSetupEvent event) {

    }

}
