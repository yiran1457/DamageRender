package net.yiran.damagerender;

import net.neoforged.neoforge.common.ModConfigSpec;

public class ClientConfig {

    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.ConfigValue<Integer> SHOW_DISTANCE;
    public static final ModConfigSpec.ConfigValue<Integer> MAX_SHOW_RENDER;
    public static final ModConfigSpec.ConfigValue<Double> MIN_VALUE_DISPLAY;
    public static final ModConfigSpec.ConfigValue<Boolean> ENABLE_COMBINE_STRING;
    public static final ModConfigSpec.ConfigValue<Double> MERGE_DISTANCE_SQ;
    public static final ModConfigSpec.ConfigValue<Double> MERGE_MAX_AGE;

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
                .define("mergeMaxAge", 40.0);
        SPEC = BUILDER.build();

    }
}
