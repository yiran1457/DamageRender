package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;

public class DamageString {
    private float x;
    private float y;
    private float z;

    private float vX;
    private float vY;
    private float vZ;

    private String displayText;
    private float halfWidth;
    private float life;
    private float maxLife;
    private float scale;
    private int color;
    private int colorRgb;

    private float amount;
    private String damageType;

    private static final float INITIAL_UPWARD_SPEED = 0.15f;
    private static final float DRAG_FACTOR = 0.9f;
    private static final float HOVER_THRESHOLD = 0.001f;
    private static final float FADE_START_RATIO = 0.4f;
    private static final float SHRINK_DURATION = 20f;

    public DamageString(float x, float y, float z, float damage, int color, String damageType) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.vX = (float) (Math.random() - 0.5) * 0.25f;
        this.vZ = (float) (Math.random() - 0.5) * 0.25f;
        this.vY = INITIAL_UPWARD_SPEED;

        this.life = 80;
        this.maxLife = this.life;
        this.scale = 0.04f;
        this.colorRgb = color & 0x00FFFFFF;
        this.color = (0xFF << 24) | this.colorRgb;
        this.damageType = damageType;

        this.amount = Math.abs(damage);
        formatDamage();
    }

    private static int setAlpha(int rgb, int alpha) {
        alpha = Math.clamp(alpha, 0, 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    public void mergeDamage(float additional) {
        this.amount += Math.abs(additional);
        formatDamage();
        this.life = this.maxLife;
    }

    public String getDamageType() {
        return damageType;
    }

    public float getAmount() {
        return amount;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }

    public float getLife() {
        return life;
    }

    public float getMaxLife() {
        return maxLife;
    }

    public boolean isDead() {
        return life <= 0;
    }

    private void formatDamage() {
        if (amount < 1) {
            this.displayText = String.format("%.2f", amount);
        } else if (amount < 10) {
            this.displayText = String.format("%.1f", amount);
        } else {
            this.displayText = String.format("%.0f", amount);
        }
        // 缓存文字水平居中偏移，避免每帧 render 重复调用 font.width
        this.halfWidth = -Minecraft.getInstance().font.width(this.displayText) / 2f;
    }

    private void update(float partialTick) {
        this.life -= partialTick;

        if (this.life > 0) {
            this.vY *= DRAG_FACTOR;
            this.vX *= DRAG_FACTOR;
            this.vZ *= DRAG_FACTOR;

            if (Math.abs(this.vY) < HOVER_THRESHOLD) this.vY = 0;
            if (Math.abs(this.vX) < HOVER_THRESHOLD) this.vX = 0;
            if (Math.abs(this.vZ) < HOVER_THRESHOLD) this.vZ = 0;

            this.x += this.vX * partialTick;
            this.y += this.vY * partialTick;
            this.z += this.vZ * partialTick;
        }

        float fadeStartLife = this.maxLife * FADE_START_RATIO;
        int alpha = 255;
        if (this.life < fadeStartLife) {
            alpha = (int) ((this.life / fadeStartLife) * 255);
        }
        alpha = Math.clamp(alpha, 0, 255);

        this.color = setAlpha(this.colorRgb, alpha);
    }

    public void render(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, Minecraft mc, float partialTick) {
        update(partialTick);

        poseStack.pushPose();
        poseStack.translate(x, y, z);
        // 用相机朝向四元数让文字面朝玩家；与官方 EntityRenderer.renderNameTag 一致，
        // 配合 scale(x,-x,x) 的负 Y 缩放把文字翻正（cameraOrientation 会上下颠倒文字）。
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());

        float renderScale = scale;
        if (life < SHRINK_DURATION) {
            renderScale = scale * (life / SHRINK_DURATION);
        }
        poseStack.scale(renderScale, -renderScale, renderScale);

        Font font = mc.font;
        font.drawInBatch(
                displayText,
                halfWidth,
                -4f,
                this.color,
                true,
                poseStack.last().pose(),
                bufferSource,
                Font.DisplayMode.SEE_THROUGH,
                LightTexture.FULL_BRIGHT,
                0
        );

        poseStack.popPose();
    }
}
