package net.yiran.damagerender;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<Integer> SHOW_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_SHOW_RENDER;
    public static final ForgeConfigSpec.ConfigValue<Double> MIN_VALUE_DISPLAY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_COMBINE_STRING;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_COMBINE_ENTITY;
    public static final ForgeConfigSpec.ConfigValue<Double> MERGE_MAX_AGE;
    public static final ForgeConfigSpec.ConfigValue<Integer> DAMAGE_STRING_LIFE;
    public static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_HEAL_NUMBERS;
    public static final ForgeConfigSpec.ConfigValue<String> TEXTURE;

    static {

        SHOW_DISTANCE = BUILDER
                .comment("最远的渲染距离，超出此半径的不渲染")
                .define("showDistance", 32);
        MAX_SHOW_RENDER = BUILDER
                .comment("最多渲染的数字量")
                .define("maxShowRender", 128);
        MIN_VALUE_DISPLAY = BUILDER
                .comment("启用渲染的最小数值")
                .define("minValueDisplay", 0.5);
        ENABLE_COMBINE_STRING = BUILDER
                .comment("是否启用同类型伤害数字合并")
                .define("enableCombineString", true);
        ENABLE_COMBINE_ENTITY = BUILDER
                .comment("是否将同一实体的所有伤害合并为一个数字（不合并治疗），颜色使用默认色")
                .define("enableCombineEntity", false);
        MERGE_MAX_AGE = BUILDER
                .comment("合并生成在多少tick以内的文字")
                .define("mergeMaxAge", 40.0);
        DAMAGE_STRING_LIFE = BUILDER
                .comment("伤害数字存活时间（tick）")
                .defineInRange("damageStringLife", 30, 5, 600);
        SHOW_HEAL_NUMBERS = BUILDER
                .comment("是否显示治疗数字")
                .define("showHealNumbers", true);
        TEXTURE = BUILDER
                .comment("伤害数字使用的纹理资源路径（namespace:path 格式，如 damagerender:textures/damagefont/number.png）",
                        "纹理需为 0-9 与 '.' 共 11 个字符的水平图集，每字符 6×9 像素",
                        "切换后立即生效，无需重启游戏")
                .define("texture", "damagerender:textures/damagefont/number_0.png");
        SPEC = BUILDER.build();

    }
}