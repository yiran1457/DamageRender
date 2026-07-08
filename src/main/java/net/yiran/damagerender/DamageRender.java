package net.yiran.damagerender;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.serialization.JsonOps;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.ClientEventHandler;
import net.yiran.damagerender.client.Command;
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
        }

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
    }

    public void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar(DamageRender.MODID)
                .playToClient(DamageInfoPacket.TYPE, DamageInfoPacket.STREAM_CODEC, DamageInfoPacket::handle)
                .playToServer(UpdateConfigPacket.TYPE, UpdateConfigPacket.STREAM_CODEC, UpdateConfigPacket::handle);
    }
}
