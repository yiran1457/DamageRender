package net.yiran.damagerender.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.arguments.*;
import com.mojang.serialization.JsonOps;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.yiran.damagerender.ClientConfig;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;

import static net.yiran.damagerender.DamageRender.DAMAGE_COLOR_PATH;
import static net.yiran.damagerender.DamageRender.getHexColor;

public class Command {
    @SubscribeEvent
    public static void commandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("damagerender")
                        .then(
                                Commands.literal("minValueDisplay")
                                        .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                                        .executes(ctx -> {
                                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                                            ClientConfig.MIN_VALUE_DISPLAY.set(value);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 minValueDisplay 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MIN_VALUE_DISPLAY.get();
                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 minValueDisplay 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("maxShowRender")
                                        .then(
                                                Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> {
                                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                                            ClientConfig.MAX_SHOW_RENDER.set(value);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 maxShowRender 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            int value = ClientConfig.MAX_SHOW_RENDER.get();
                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 maxShowRender 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("enableCombineString")
                                        .then(
                                                Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(ctx -> {
                                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                                            ClientConfig.ENABLE_COMBINE_STRING.set(value);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 enableCombineString 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.ENABLE_COMBINE_STRING.get();
                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 enableCombineString 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("mergeDistanceSQ")
                                        .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                                        .executes(ctx -> {
                                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                                            ClientConfig.MERGE_DISTANCE_SQ.set(value);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 mergeDistanceSQ 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MERGE_DISTANCE_SQ.get();
                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 mergeDistanceSQ 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("mergeMaxAge")
                                        .then(
                                                Commands.argument("value", FloatArgumentType.floatArg(0))
                                                        .executes(ctx -> {
                                                            float value = FloatArgumentType.getFloat(ctx, "value");
                                                            ClientConfig.MERGE_MAX_AGE.set(value);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 mergeMaxAge 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            float value = ClientConfig.MERGE_MAX_AGE.get();
                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 mergeMaxAge 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("setDamageColor")
                                        .then(
                                                Commands.literal("reload")
                                                        .executes(ctx -> {
                                                            if (Files.exists(DAMAGE_COLOR_PATH)) {
                                                                JsonElement json = null;
                                                                try {
                                                                    json = JsonParser.parseString(Files.readString(DAMAGE_COLOR_PATH));
                                                                } catch (IOException e) {
                                                                    e.printStackTrace();
                                                                }
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
                                                                } catch (Exception e) {
                                                                    e.printStackTrace();
                                                                }
                                                                ClientDamageInfoManager.getInstance().setDamageColorMap(ClientDamageInfoManager.COLOR_CODEC.parse(JsonOps.INSTANCE, json).result().get());
                                                            }
                                                            ctx.getSource().sendSuccess(() -> Component.literal("已重载伤害颜色映射"),false);
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
}
