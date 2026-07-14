package net.yiran.damagerender.client;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.client.Minecraft;
//? if >1.19.2 {
import net.minecraft.core.registries.Registries;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
//? if forge {
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.DamageRender;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
*///?}
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.data.UpdateConfigPacket;

import java.util.LinkedHashSet;
import java.util.Set;

/** Client commands for configuration and persistent damage-color mappings. */
public class Command {
    @SubscribeEvent
    public static void commandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("damagerender")
                        .then(
                                Commands.literal("showDistance")
                                        .then(
                                                Commands.argument("value", IntegerArgumentType.integer(0))
                                                        .executes(ctx -> {
                                                            int value = IntegerArgumentType.getInteger(ctx, "value");
                                                            ClientConfig.SHOW_DISTANCE.set(value);
                                                            ClientConfig.SPEC.save();
                                                            syncShowDistance(value);
                                                            sendSuccess(ctx.getSource(), "配置项 showDistance 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            int value = ClientConfig.SHOW_DISTANCE.get();
                                            sendSuccess(ctx.getSource(), "配置项 showDistance 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("minValueDisplay")
                                        .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg(0))
                                                        .executes(ctx -> {
                                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                                            ClientConfig.MIN_VALUE_DISPLAY.set(value);
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 minValueDisplay 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MIN_VALUE_DISPLAY.get();
                                            sendSuccess(ctx.getSource(), "配置项 minValueDisplay 值为 : " + value);
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
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 maxShowRender 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            int value = ClientConfig.MAX_SHOW_RENDER.get();
                                            sendSuccess(ctx.getSource(), "配置项 maxShowRender 值为 : " + value);
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
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 enableCombineString 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.ENABLE_COMBINE_STRING.get();
                                            sendSuccess(ctx.getSource(), "配置项 enableCombineString 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("enableCombineEntity")
                                        .then(
                                                Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(ctx -> {
                                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                                            ClientConfig.ENABLE_COMBINE_ENTITY.set(value);
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 enableCombineEntity 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.ENABLE_COMBINE_ENTITY.get();
                                            sendSuccess(ctx.getSource(), "配置项 enableCombineEntity 值为 : " + value);
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
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 mergeMaxAge 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MERGE_MAX_AGE.get();
                                            sendSuccess(ctx.getSource(), "配置项 mergeMaxAge 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("mergeScaleMax")
                                        .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg(1.0, 10.0))
                                                        .executes(ctx -> {
                                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                                            ClientConfig.MERGE_SCALE_MAX.set(value);
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 mergeScaleMax 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MERGE_SCALE_MAX.get();
                                            sendSuccess(ctx.getSource(), "配置项 mergeScaleMax 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("mergeResetLife")
                                        .then(
                                                Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(ctx -> {
                                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                                            ClientConfig.MERGE_RESET_LIFE.set(value);
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 mergeResetLife 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.MERGE_RESET_LIFE.get();
                                            sendSuccess(ctx.getSource(), "配置项 mergeResetLife 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("baseScaleLogBase")
                                        .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg(1.01, 32768.0))
                                                        .executes(ctx -> {
                                                            double value = DoubleArgumentType.getDouble(ctx, "value");
                                                            ClientConfig.BASE_SCALE_LOG_BASE.set(value);
                                                            ClientConfig.SPEC.save();
                                                            DamageString.refreshLogBase();
                                                            sendSuccess(ctx.getSource(), "配置项 baseScaleLogBase 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.BASE_SCALE_LOG_BASE.get();
                                            sendSuccess(ctx.getSource(), "配置项 baseScaleLogBase 值为 : " + value);
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
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 damageStringLife 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            int value = ClientConfig.DAMAGE_STRING_LIFE.get();
                                            sendSuccess(ctx.getSource(), "配置项 damageStringLife 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("showHealNumbers")
                                        .then(
                                                Commands.argument("value", BoolArgumentType.bool())
                                                        .executes(ctx -> {
                                                            boolean value = BoolArgumentType.getBool(ctx, "value");
                                                            ClientConfig.SHOW_HEAL_NUMBERS.set(value);
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 showHealNumbers 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.SHOW_HEAL_NUMBERS.get();
                                            sendSuccess(ctx.getSource(), "配置项 showHealNumbers 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("texture")
                                        .then(
                                                Commands.argument("value", STRING_ALLOWING_COLON)
                                                        .suggests(TEXTURE_SUGGESTIONS)
                                                        .executes(ctx -> {
                                                            String value = ctx.getArgument("value", String.class);
                                                            if (net.minecraft.resources.ResourceLocation.tryParse(value) == null) {
                                                                ctx.getSource().sendFailure(Component.literal("无效的资源路径 : " + value + "（需为 namespace:path 格式）"));
                                                                return 0;
                                                            }
                                                            ClientConfig.TEXTURE.set(value);
                                                            ClientConfig.SPEC.save();
                                                            sendSuccess(ctx.getSource(), "配置项 texture 设置为 : " + value);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            String value = ClientConfig.TEXTURE.get();
                                            sendSuccess(ctx.getSource(), "配置项 texture 值为 : " + value);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("setDamageColor")
                                        .then(
                                                Commands.literal("reload")
                                                        .executes(ctx -> {
                                                            DamageColorManager.getInstance().reload();
                                                            sendSuccess(ctx.getSource(), "已重载伤害颜色映射");
                                                            return 1;
                                                        })
                                        )
                                        .then(
                                                Commands.literal("remove")
                                                        .then(
                                                                Commands.argument("damageType", STRING_ALLOWING_COLON)
                                                                        .suggests(DAMAGE_TYPE_SUGGESTIONS)
                                                                        .executes(ctx -> {
                                                                            String key = ctx.getArgument("damageType", String.class);
                                                                            boolean removed = DamageColorManager.getInstance().remove(key);
                                                                            if (removed) {
                                                                                sendSuccess(ctx.getSource(), "已移除伤害颜色 : " + key);
                                                                                return 1;
                                                                            }
                                                                            ctx.getSource().sendFailure(Component.literal("未找到伤害类型 : " + key));
                                                                            return 0;
                                                                        })
                                                        )
                                        )
                                        .then(
                                                Commands.literal("list")
                                                        .executes(ctx -> {
                                                            var map = DamageColorManager.getInstance().getMap();
                                                            if (map.isEmpty()) {
                                                                sendSuccess(ctx.getSource(), "当前颜色映射为空");
                                                                return 1;
                                                            }
                                                            map.forEach((k, v) ->
                                                                    sendSuccess(ctx.getSource(), k + " : " + v.serialize()));
                                                            sendSuccess(ctx.getSource(), "共 " + map.size() + " 项");
                                                            return 1;
                                                        })
                                        )
                                        .then(
                                                Commands.argument("damageType", STRING_ALLOWING_COLON)
                                                        .suggests(DAMAGE_TYPE_SUGGESTIONS)
                                                        .then(
                                                                Commands.argument("color", STRING_ALLOWING_COLON)
                                                                        .executes(ctx -> {
                                                                            String key = ctx.getArgument("damageType", String.class);
                                                                            String colorInput = ctx.getArgument("color", String.class);
                                                                            TextColor color = parseColorInput(colorInput);
                                                                            if (color == null) {
                                                                                ctx.getSource().sendFailure(Component.literal("无效颜色 : " + colorInput + "（支持 #RRGGBB、颜色名、十进制整数）"));
                                                                                return 0;
                                                                            }
                                                                            DamageColorManager.getInstance().put(key, color);
                                                                            sendSuccess(ctx.getSource(), "伤害颜色 " + key + " 设置为 : " + color.serialize());
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
        );
    }

    private static void sendSuccess(CommandSourceStack source, String message) {
//? if =1.19.2 {
        /*source.sendSuccess(Component.literal(message), false);
*///?} else {
        source.sendSuccess(() -> Component.literal(message), false);
//?}
    }

    private static void syncShowDistance(int distance) {
        if (Minecraft.getInstance().getConnection() == null) return;
//? if forge {
        DamageRender.NETWORK.sendToServer(new UpdateConfigPacket(distance));
//?} else {
        /*Minecraft.getInstance().getConnection().send(new UpdateConfigPacket(distance));
*///?}
    }

    private static final ArgumentType<String> STRING_ALLOWING_COLON = new ArgumentType<>() {
        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
                reader.skip();
            }
            if (reader.getCursor() == start) {
                throw new SimpleCommandExceptionType(Component.literal("需要提供参数值")).create();
            }
            return reader.getString().substring(start, reader.getCursor());
        }
    };

    private static final SuggestionProvider<CommandSourceStack> DAMAGE_TYPE_SUGGESTIONS =
            (ctx, builder) -> {
                Set<String> candidates = new LinkedHashSet<>();
//? if >1.19.2 {
                try {
                    ctx.getSource().registryAccess()
                            .registry(Registries.DAMAGE_TYPE)
                            .ifPresent(reg -> reg.registryKeySet()
                                    .forEach(key -> candidates.add(key.location().toString())));
                } catch (Exception ignored) {
                }
//?}
                candidates.add("heal");
                candidates.addAll(DamageColorManager.getInstance().getMap().keySet());
                return SharedSuggestionProvider.suggest(candidates, builder);
            };

         // texture 参数的 Tab 补全：列出 mod 内置纹理（assets/damagerender/textures/*.png），
     // 形如 {@code damagerender:textures/damagefont/number.png}，便于切换皮肤。
     //
    private static final SuggestionProvider<CommandSourceStack> TEXTURE_SUGGESTIONS =
            (ctx, builder) -> {
                java.util.Set<String> candidates = new java.util.LinkedHashSet<>();
                try {
                    net.minecraft.server.packs.resources.ResourceManager rm =
                            net.minecraft.client.Minecraft.getInstance().getResourceManager();
                    java.util.Collection<net.minecraft.resources.ResourceLocation> textures =
                            rm.listResources("textures", rl ->
                                    rl.getNamespace().equals("damagerender") && rl.getPath().endsWith(".png"))
                                    .keySet();
                    textures.forEach(rl -> candidates.add(rl.toString()));
                } catch (Exception ignored) {
                }
                return SharedSuggestionProvider.suggest(candidates, builder);
            };

         // 解析颜色输入：支持 #RRGGBB、命名颜色（red/green…）、十进制整数（含 0x 前缀）。失败返回 null。
     //
    private static TextColor parseColorInput(String input) {
//? if =1.21.1 {
        /*return TextColor.parseColor(input).result().orElse(null);
*///?} else {
        TextColor color = TextColor.parseColor(input);
        if (color != null) return color;
        try {
            return TextColor.fromRgb(Integer.decode(input));
        } catch (NumberFormatException e) {
            return null;
        }
//?}
    }
}
