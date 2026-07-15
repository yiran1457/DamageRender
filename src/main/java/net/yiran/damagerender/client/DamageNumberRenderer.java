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

/**
 * 使用 11 格数字纹理图集渲染伤害值，避免依赖字体绘制。
 * 纹理由 {@link ClientConfig#TEXTURE} 决定，非法配置会回退到默认纹理。
 */
public class DamageNumberRenderer {

    /** 默认数字纹理；使用 {@code tryParse} 兼容各版本的资源标识类型。 */
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

    /** ASCII 字符到图集索引的映射；{@code -1} 表示不支持。 */
    private static final byte[] CHAR_TO_INDEX = new byte[128];

    /** 复用的顶点坐标缓冲。 */
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

    /** 解析配置中的纹理；格式无效时回退到默认纹理。 */
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

    /** 返回当前纹理对应的文字渲染类型。 */
    public static RenderType getRenderType() {
//? if >1.21.1 {
        /*return RenderTypes.text(getTexture());
*///?} else {
        return RenderType.text(getTexture());
//?}
    }

    /** 为每个支持的字符写入一个纹理四边形，并返回总宽度。 */
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

    /** 返回图集中所有可渲染字符的总宽度。 */
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
