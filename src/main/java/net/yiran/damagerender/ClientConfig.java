package net.yiran.damagerender;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

    public static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<Integer> SHOW_DISTANCE;
    public static final ForgeConfigSpec.ConfigValue<Integer> MAX_SHOW_RENDER;
    public static final ForgeConfigSpec.ConfigValue<Double> MIN_VALUE_DISPLAY;
    static {
        //BUILDER.push("Client Setting");

        SHOW_DISTANCE = BUILDER
                .comment("最远的渲染距离，超出此半径的不渲染")
                .define("showDistance", 32);
        MAX_SHOW_RENDER = BUILDER
                .comment("最多渲染的数字量")
                .define("maxShowRender", 128);
        MIN_VALUE_DISPLAY = BUILDER
                .comment("启用渲染的最小数值")
                .define("minValueDisplay", 0.5);
        //BUILDER.pop();

        SPEC = BUILDER.build();

    }
}
