package net.yiran.damagerender.client;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.ClientConfig;

import static net.yiran.damagerender.DamageRender.reloadDamageColorMap;

/**
 * 客户端命令。
 * 数值/布尔类配置项通过命令 set/get 调整；
 * 颜色映射重载：damagerender-damage-color.json 是自由格式 JSON，ModConfigSpec 不支持，
 * 命令是它的唯一重载入口。
 */
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
                                Commands.literal("mergeMaxAge")
                                        .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                                        .executes(ctx -> {
                                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                                            ClientConfig.MERGE_MAX_AGE.set(value);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 mergeMaxAge 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MERGE_MAX_AGE.get();
                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 mergeMaxAge 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("damageStringLife")
                                        .then(
                                                Commands.argument("value", IntegerArgumentType.integer(5, 600))
                                                        .executes(ctx -> {
                                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                                            ClientConfig.DAMAGE_STRING_LIFE.set(value);
                                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 damageStringLife 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            int value = ClientConfig.DAMAGE_STRING_LIFE.get();
                                            ctx.getSource().sendSuccess(() -> Component.literal("配置项 damageStringLife 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("setDamageColor")
                                        .then(
                                                Commands.literal("reload")
                                                        .executes(ctx -> {
                                                            reloadDamageColorMap();
                                                            ctx.getSource().sendSuccess(() -> Component.literal("已重载伤害颜色映射"), false);
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
}
