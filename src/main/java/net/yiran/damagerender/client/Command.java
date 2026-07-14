//? if forge {
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
//? if =1.20.1 {
import net.minecraft.core.registries.Registries;
//?}
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.ClientConfig;

import java.util.LinkedHashSet;
import java.util.Set;

 // 客户端命令。
 // 数值/布尔类配置项通过命令 set/get 调整；
 // 颜色映射：damagerender-damage-color.json 是自由格式 JSON，ModConfigSpec 不支持，
 // 命令是它的运行时入口：set/remove/list 单条操作并持久化，reload 整体重载。
 //
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
                                Commands.literal("baseScaleLogBase")
                                        .then(
                                                Commands.argument("value", DoubleArgumentType.doubleArg(1.01, 100.0))
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

         // sendSuccess 的签名在 1.19.2 是 (Component, boolean)，1.20.1 是 (Supplier<Component>, boolean)。
     // 用 helper 统一调用点，方法体按版本条件化。
     //
    private static void sendSuccess(CommandSourceStack source, String message) {
//? if =1.19.2 {
        /*source.sendSuccess(Component.literal(message), false);
*///?} else {
        source.sendSuccess(() -> Component.literal(message), false);
//?}
    }

         // 自定义参数类型：读取直到空白字符为止的任意字符串。
     // 替代 {@link com.mojang.brigadier.arguments.StringArgumentType#string()} ，解决后者 {@code readUnquotedString()}
     // 不允许 {@code :}、{@code #} 等字符的问题——如 {@code minecraft:void} 会报"紧邻的数据"。
     //
    private static final ArgumentType<String> STRING_ALLOWING_COLON = new ArgumentType<>() {
        @Override
        public String parse(StringReader reader) throws CommandSyntaxException {
            int start = reader.getCursor();
            while (reader.canRead() && !Character.isWhitespace(reader.peek())) {
                reader.skip();
            }
            if (reader.getCursor() == start) {
                throw new SimpleCommandExceptionType(
                        Component.literal("需要提供参数值")).create();
            }
            return reader.getString().substring(start, reader.getCursor());
        }
    };

         // damageType 参数的 Tab 补全：合并 DamageType 注册表 location、heal 兜底键、当前颜色映射已有键，去重。
     //
    private static final SuggestionProvider<CommandSourceStack> DAMAGE_TYPE_SUGGESTIONS =
            (ctx, builder) -> {
                Set<String> candidates = new LinkedHashSet<>();
//? if =1.20.1 {
                try {
                    // registryAccess 在单机未载入/未连接时可能不可用，静默兜底
                    ctx.getSource().registryAccess()
                            .registry(Registries.DAMAGE_TYPE)
                            .ifPresent(reg -> reg.registryKeySet()
                                    .forEach(rk -> candidates.add(rk.location().toString())));
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
        TextColor color = TextColor.parseColor(input);
        if (color != null) return color;
        try {
            return TextColor.fromRgb(Integer.decode(input));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
//?} else {
/*package net.yiran.damagerender.client;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import static net.yiran.damagerender.DamageRender.reloadDamageColorMap;

 // 客户端命令。
 // 数值/布尔类配置项已迁移到 Mods 列表的配置界面，这里仅保留
 // 颜色映射重载：damagerender-damage-color.json 是自由格式 JSON，ModConfigSpec 不支持，
 // 命令是它的唯一重载入口。
 //
public class Command {
    @SubscribeEvent
    public static void commandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("damagerender")
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
*///?}
