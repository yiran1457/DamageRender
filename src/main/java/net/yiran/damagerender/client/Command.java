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
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.ClientConfig;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 客户端命令。
 * 数值/布尔类配置项通过命令 set/get 调整；
 * 颜色映射：damagerender-damage-color.json 是自由格式 JSON，ModConfigSpec 不支持，
 * 命令是它的运行时入口：set/remove/list 单条操作并持久化，reload 整体重载。
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
                                                            ClientConfig.SPEC.save();
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 minValueDisplay 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MIN_VALUE_DISPLAY.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 minValueDisplay 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 maxShowRender 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            int value = ClientConfig.MAX_SHOW_RENDER.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 maxShowRender 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 enableCombineString 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.ENABLE_COMBINE_STRING.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 enableCombineString 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 enableCombineEntity 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.ENABLE_COMBINE_ENTITY.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 enableCombineEntity 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 mergeMaxAge 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MERGE_MAX_AGE.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 mergeMaxAge 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 mergeScaleMax 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.MERGE_SCALE_MAX.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 mergeScaleMax 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 mergeResetLife 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.MERGE_RESET_LIFE.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 mergeResetLife 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 baseScaleLogBase 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            double value = ClientConfig.BASE_SCALE_LOG_BASE.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 baseScaleLogBase 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 damageStringLife 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            int value = ClientConfig.DAMAGE_STRING_LIFE.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 damageStringLife 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 showHealNumbers 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            boolean value = ClientConfig.SHOW_HEAL_NUMBERS.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 showHealNumbers 值为 : " + value), false);
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
                                                            ctx.getSource().sendSuccess(Component.literal("配置项 texture 设置为 : " + value), false);
                                                            return 1;
                                                        })
                                        )
                                        .executes(ctx -> {
                                            String value = ClientConfig.TEXTURE.get();
                                            ctx.getSource().sendSuccess(Component.literal("配置项 texture 值为 : " + value), false);
                                            return 1;
                                        })
                        )
                        .then(
                                Commands.literal("setDamageColor")
                                        .then(
                                                Commands.literal("reload")
                                                        .executes(ctx -> {
                                                            DamageColorManager.getInstance().reload();
                                                            ctx.getSource().sendSuccess(Component.literal("已重载伤害颜色映射"), false);
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
                                                                                ctx.getSource().sendSuccess(Component.literal("已移除伤害颜色 : " + key), false);
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
                                                                ctx.getSource().sendSuccess(Component.literal("当前颜色映射为空"), false);
                                                                return 1;
                                                            }
                                                            map.forEach((k, v) ->
                                                                    ctx.getSource().sendSuccess(Component.literal(k + " : " + v.serialize()), false));
                                                            ctx.getSource().sendSuccess(Component.literal("共 " + map.size() + " 项"), false);
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
                                                                            ctx.getSource().sendSuccess(Component.literal("伤害颜色 " + key + " 设置为 : " + color.serialize()), false);
                                                                            return 1;
                                                                        })
                                                        )
                                        )
                        )
        );
    }

    /**
     * 自定义参数类型：读取直到空白字符为止的任意字符串。
     * 替代 {@link com.mojang.brigadier.arguments.StringArgumentType#string()} ，解决后者 {@code readUnquotedString()}
     * 不允许 {@code :}、{@code #} 等字符的问题——如 {@code minecraft:void} 会报"紧邻的数据"。
     */
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

    /**
     * damageType 参数的 Tab 补全：当前颜色映射已有键 + heal 兜底键，去重。
     * 1.19.2 中没有 DamageType 注册表，无法从注册表动态获取。
     */
    private static final SuggestionProvider<CommandSourceStack> DAMAGE_TYPE_SUGGESTIONS =
            (ctx, builder) -> {
                Set<String> candidates = new LinkedHashSet<>();
                candidates.add("heal");
                candidates.addAll(DamageColorManager.getInstance().getMap().keySet());
                return SharedSuggestionProvider.suggest(candidates, builder);
            };

    /**
     * texture 参数的 Tab 补全：列出 mod 内置纹理（assets/damagerender/textures/*.png），
     * 形如 {@code damagerender:textures/damagefont/number.png}，便于切换皮肤。
     */
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

    /**
     * 解析颜色输入：支持 #RRGGBB、命名颜色（red/green…）、十进制整数（含 0x 前缀）。失败返回 null。
     */
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