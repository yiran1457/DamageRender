package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.UpdateConfigPacket;

public class ClientEventHandler {
    @SubscribeEvent
    public static void onLogging(ClientPlayerNetworkEvent.LoggingIn event) {
        DamageRender.NETWORK.sendToServer(new UpdateConfigPacket(ClientConfig.SHOW_DISTANCE.get()));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 vec3 = camera.getPosition();
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics guiGraphics = new GuiGraphics(mc, poseStack, mc.renderBuffers().bufferSource());

        float partialTick = event.getPartialTick();
        {
            poseStack.pushPose();
            poseStack.translate(-vec3.x, -vec3.y, -vec3.z);
            ClientDamageInfoManager.instance.damageStringList.removeIf(damageString -> {
                damageString.render(poseStack, guiGraphics, mc, partialTick);
                return damageString.life < 0 ;
            });
            poseStack.popPose();
        }
    }
}
