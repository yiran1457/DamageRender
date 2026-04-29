package net.yiran.damagerender;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<Integer> SHOW_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_SHOW_RENDER;
    public static final ForgeConfigSpec.ConfigValue<Double> MIN_VALUE_DISPLAY;
    public static final ForgeConfigSpec.ConfigValue<Boolean> ENABLE_COMBINE_STRING;
    public static final ForgeConfigSpec.ConfigValue<Double> MERGE_DISTANCE_SQ;
    public static final ForgeConfigSpec.ConfigValue<Float> MERGE_MAX_AGE;

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
                .comment("是否启用渲染数字合并")
                .define("enableCombineString", true);
        MERGE_DISTANCE_SQ = BUILDER
                .comment("合并显示的半径")
                .define("mergeDistanceSQ", 1.5);
        MERGE_MAX_AGE = BUILDER
                .comment("合并生成在多少tick以内的文字")
                .define("mergeMaxAge", 40.0f);
        SPEC = BUILDER.build();

    }
}
