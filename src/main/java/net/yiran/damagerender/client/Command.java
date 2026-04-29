package net.yiran.damagerender.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.ClientConfig;

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
        );
    }
}
