package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Quaternionf;

public class DamageString {
    public float x;
    public float y;
    public float z;

    public float vX;
    public float vY;
    public float vZ;

    public float aX;
    public float aY;
    public float aZ;

    public String damage;

    public float life;
    public float scale;
    //public boolean isHeal;
    public int color;

    public DamageString(float x, float y, float z, float damage,int color) {
        this.x = x;
        this.y = y;
        this.z = z;

        this.vX = (float) (Math.random() - 0.5) * 0.04f;
        this.vY = 0.06f;
        this.vZ = (float) (Math.random() - 0.5) * 0.04f;

        this.aX = -vX * 0.001f;
        this.aY = -0.0012f;
        this.aZ = -vZ * 0.001f;

        life = 100;
        scale = 0.04f;
        this.color = color;
        //isHeal = damage > 0;
        //color = damage > 0 ? Color.green.getRGB() : Color.red.getRGB();
        float amount = Math.abs(damage);
        if (amount < 1) {
            this.damage = String.format("%.2f", amount);
        } else if (amount < 10) {
            this.damage = String.format("%.1f", amount);
        } else {
            this.damage = String.format("%.0f", amount);
        }
    }

    public static int setAlpha(int originalColor, int newAlpha) {
        newAlpha = Math.min(255, Math.max(5, newAlpha));
        int rgbOnly = originalColor & 0x00FFFFFF;
        int alphaShifted = newAlpha << 24;
        return rgbOnly | alphaShifted;
    }

    public void posUpdate(float partialTick) {
        life -= partialTick;
        x += vX * partialTick;
        y += vY * partialTick;
        z += vZ * partialTick;
        vX += aX * partialTick;
        vY += aY * partialTick;
        vZ += aZ * partialTick;
        if (life < 60) {
            //color = setAlpha(color, (int) (life/40*255));
            color = setAlpha(color, (int) ((Math.cos((double) (60f - life) / (double) 4f) + 2) / 4 * 255));
            if (life < 20) {
                scale = 0.04f * life / 20;
            }
        }

    }

    public void render(PoseStack poseStack, GuiGraphics guiGraphics, Minecraft mc, float partialTick) {
        posUpdate(partialTick);
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(scale, scale, 1);
        poseStack.mulPose(new Quaternionf().rotateZ((float) Math.PI));

        //guiGraphics.setColor(1, 1, 1, life < 40 ? life / 40 : 1);

        //RenderSystem.setShaderColor(1, 1, 1, life < 40 ? life / 40 : 1);
        guiGraphics.drawString(mc.font, damage, -mc.font.width(damage) / 2, -4, color, false);
        //RenderSystem.setShaderColor(1, 1, 1, 1);

        //guiGraphics.setColor(1, 1, 1, 1);
        poseStack.popPose();
    }
}
