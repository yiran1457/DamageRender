package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.UpdateConfigPacket;

public class ClientEventHandler {
    @SubscribeEvent
    public static void onLogging(ClientPlayerNetworkEvent.LoggingIn event) {
        Minecraft.getInstance().getConnection().send(new UpdateConfigPacket(ClientConfig.SHOW_DISTANCE.get()));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Minecraft mc = Minecraft.getInstance();

        float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        ClientDamageInfoManager manager = ClientDamageInfoManager.getInstance();
        for (DamageString damageString : manager.getDamageStringList()) {
            damageString.render(poseStack, bufferSource, mc, partialTick);
        }
        manager.getDamageStringList().removeIf(DamageString::isDead);

        poseStack.popPose();
    }
}
