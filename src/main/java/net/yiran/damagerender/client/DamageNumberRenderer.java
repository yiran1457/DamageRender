package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
//? if >1.21.1 {
/*import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
*///?} else {
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
//?}
import net.yiran.damagerender.ClientConfig;

 // 使用纹理图集渲染伤害数字，替代 Font.drawInBatch。
 //
 // <p>纹理布局：0123456789. 共 11 个字符，每个 6×9 像素，总宽 66px。
 // 渲染时按字符查表获取 UV，逐字符发射四边形顶点。
 //
 // <p>使用的纹理由 {@link ClientConfig#TEXTURE} 决定，运行时可通过指令切换。
 // {@link RenderType#text(ResourceLocation)} 内部用 {@code Util.memoize} 按 ResourceLocation
 // 缓存 RenderType，因此每个纹理路径对应一个独立缓存实例，切换纹理只需换入不同的 ResourceLocation。
 //
public class DamageNumberRenderer {

     // 默认纹理路径（配置缺失或非法时兜底）。用 tryParse 而非构造，兼容 1.21.1（构造已 private）。
//? if >1.21.1 {
    /*public static final Identifier DEFAULT_TEXTURE =
            Identifier.tryParse("damagerender:textures/damagefont/number_0.png");
*///?} else {
    public static final ResourceLocation DEFAULT_TEXTURE =
            ResourceLocation.tryParse("damagerender:textures/damagefont/number_0.png");
//?}

    public static final int CHAR_WIDTH = 6;
    public static final int CHAR_HEIGHT = 9;
    public static final int CHAR_COUNT = 11; // 0-9 + '.'

     // 字符 → 图集索引查找表，-1 表示无法渲染的字符。
    private static final byte[] CHAR_TO_INDEX = new byte[128];

     // 顶点变换复用缓冲 [x,y,z]，由 {@link Mat4Util#transformVertex} 写入，避免逐顶点分配。
    private static final float[] TMP_VERTEX = new float[3];

    static {
        for (int i = 0; i < CHAR_TO_INDEX.length; i++) {
            CHAR_TO_INDEX[i] = -1;
        }
        for (int i = 0; i <= 9; i++) {
            CHAR_TO_INDEX['0' + i] = (byte) i;
        }
        CHAR_TO_INDEX['.'] = 10;
    }

         // 从配置解析当前伤害数字纹理 ResourceLocation。
     //
     // <p>配置值非法（如格式错误）时回退到 {@link #DEFAULT_TEXTURE}，避免运行时抛异常导致渲染崩溃。
     //
     // @return 当前生效的纹理 ResourceLocation
     //
//? if >1.21.1 {
    /*public static Identifier getTexture() {
*///?} else {
    public static ResourceLocation getTexture() {
//?}
        String value = ClientConfig.TEXTURE.get();
//? if >1.21.1 {
        /*Identifier parsed = Identifier.tryParse(value);
*///?} else {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
//?}
        return parsed != null ? parsed : DEFAULT_TEXTURE;
    }

         // 获取当前纹理对应的文字 RenderType。
     //
     // @return 与当前纹理绑定的 RenderType（POSITION_COLOR_TEX_LIGHTMAP，支持半透明和光照）
     //
    public static RenderType getRenderType() {
//? if >1.21.1 {
        /*return RenderTypes.text(getTexture());
*///?} else {
        return RenderType.text(getTexture());
//?}
    }

         // 使用纹理图集渲染一串伤害数字，顶点经 {@code matrix} 手写 fma 变换。
     //
     // @param matrix     16 元素列主序变换矩阵（由 {@link Mat4Util#mulViewTranslateBaseScale} 产出）
     // @param consumer   顶点消费者（通过 bufferSource.getBuffer(RENDER_TYPE) 获取）
     // @param text       要渲染的文本（仅支持 0-9 和 '.'）
     // @param x          起始 X 偏移（像素，通常为居中偏移）
     // @param y          起始 Y 偏移（像素）
     // @param color      ARGB 颜色（含 alpha）
     // @param packedLight 光照贴图值（通常为 LightTexture.FULL_BRIGHT）
     // @return 渲染总宽度（像素），用于外部居中计算
     //
    public static float renderNumber(float[] matrix, VertexConsumer consumer,
                                     String text, float x, float y,
                                     int color, int packedLight) {
        float r = (color >> 16 & 0xFF) / 255f;
        float g = (color >> 8 & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = (color >> 24 & 0xFF) / 255f;

        float cursorX = x;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= CHAR_TO_INDEX.length || CHAR_TO_INDEX[ch] < 0) continue;

            int index = CHAR_TO_INDEX[ch];
            float uMin = (float) index / CHAR_COUNT;
            float uMax = (float) (index + 1) / CHAR_COUNT;

            // 四边形四个顶点：左下 → 右下 → 右上 → 左上
            float x0 = cursorX;
            float x1 = cursorX + CHAR_WIDTH;
            float y0 = y;
            float y1 = y + CHAR_HEIGHT;

            Mat4Util.transformVertex(matrix, x0, y0, TMP_VERTEX);
//? if >1.20.1 {
            /*consumer.addVertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).setColor(r, g, b, a).setUv(uMin, 1f).setLight(packedLight);
*///?} else {
            consumer.vertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).color(r, g, b, a).uv(uMin, 1f).uv2(packedLight).endVertex();
//?}
            Mat4Util.transformVertex(matrix, x1, y0, TMP_VERTEX);
//? if >1.20.1 {
            /*consumer.addVertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).setColor(r, g, b, a).setUv(uMax, 1f).setLight(packedLight);
*///?} else {
            consumer.vertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).color(r, g, b, a).uv(uMax, 1f).uv2(packedLight).endVertex();
//?}
            Mat4Util.transformVertex(matrix, x1, y1, TMP_VERTEX);
//? if >1.20.1 {
            /*consumer.addVertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).setColor(r, g, b, a).setUv(uMax, 0f).setLight(packedLight);
*///?} else {
            consumer.vertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).color(r, g, b, a).uv(uMax, 0f).uv2(packedLight).endVertex();
//?}
            Mat4Util.transformVertex(matrix, x0, y1, TMP_VERTEX);
//? if >1.20.1 {
            /*consumer.addVertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).setColor(r, g, b, a).setUv(uMin, 0f).setLight(packedLight);
*///?} else {
            consumer.vertex(TMP_VERTEX[0], TMP_VERTEX[1], TMP_VERTEX[2]).color(r, g, b, a).uv(uMin, 0f).uv2(packedLight).endVertex();
//?}

            cursorX += CHAR_WIDTH;
        }

        return cursorX - x;
    }

         // 计算文本在纹理图集中的像素宽度。
     //
     // @param text 要测量的文本
     // @return 像素宽度（仅统计可渲染字符）
     //
    public static float getTextWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch < CHAR_TO_INDEX.length && CHAR_TO_INDEX[ch] >= 0) {
                width += CHAR_WIDTH;
            }
        }
        return width;
    }
}
