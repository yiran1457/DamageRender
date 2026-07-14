package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.data.UpdateConfigPacket;
//? if forge {
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.DamageRender;
//?} else {
/*import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
*///?}
//? if =1.19.2 {
/*import com.mojang.math.Matrix4f;

import java.nio.FloatBuffer;
*///?} else {
import org.joml.Matrix4f;
import org.joml.Quaternionf;
//?}

/** Client login synchronization and floating-number rendering. */
public class ClientEventHandler {
    private static final float[] VIEW_ARR = new float[16];
    private static final float[] BASE_RS_ARR = new float[16];
//? if =1.19.2 {
    /*private static final FloatBuffer VIEW_BUF = FloatBuffer.wrap(VIEW_ARR);
    private static final FloatBuffer BASE_RS_BUF = FloatBuffer.wrap(BASE_RS_ARR);
    private static final Matrix4f FLIP_SCALE = Matrix4f.createScaleMatrix(
            -DamageString.RENDER_SCALE, DamageString.RENDER_SCALE, -DamageString.RENDER_SCALE);
*///?}

    @SubscribeEvent
    public static void onLogging(ClientPlayerNetworkEvent.LoggingIn event) {
//? if forge {
        DamageRender.NETWORK.sendToServer(new UpdateConfigPacket(ClientConfig.SHOW_DISTANCE.get()));
//?} else {
        /*Minecraft.getInstance().getConnection().send(
                new UpdateConfigPacket(ClientConfig.SHOW_DISTANCE.get()));
*///?}
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
//? if =1.19.2 {
        /*if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();
        float partialTick = mc.getDeltaFrameTime();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f viewMatrix = poseStack.last().pose();

        com.mojang.math.Quaternion cam = mc.getEntityRenderDispatcher().cameraOrientation();
        Matrix4f baseRS = new Matrix4f();
        baseRS.setIdentity();
        baseRS.multiply(new Matrix4f(cam));
        baseRS.multiply(FLIP_SCALE);

        VIEW_BUF.clear();
        viewMatrix.store(VIEW_BUF);
        BASE_RS_BUF.clear();
        baseRS.store(BASE_RS_BUF);
*///?} else {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        PoseStack poseStack = event.getPoseStack();
        Camera camera = event.getCamera();
        Vec3 cameraPos = camera.getPosition();
        Minecraft mc = Minecraft.getInstance();
//? if forge {
        float partialTick = mc.getDeltaFrameTime();
//?} else {
        /*float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
*///?}

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        Matrix4f viewMatrix = poseStack.last().pose();

        Quaternionf cam = mc.getEntityRenderDispatcher().cameraOrientation();
        Matrix4f baseRS = new Matrix4f().rotate(cam)
//? if forge {
                .scale(-1f, 1f, -1f)
//?}
                .scale(DamageString.RENDER_SCALE);

        viewMatrix.get(VIEW_ARR);
        baseRS.get(BASE_RS_ARR);
//?}

        float drag = (float) Math.pow(DamageString.DRAG_FACTOR, partialTick);
        float bounceDecay = (float) Math.pow(DamageString.BOUNCE_DECAY, partialTick);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(DamageNumberRenderer.getRenderType());
        ClientDamageInfoManager manager = ClientDamageInfoManager.getInstance();
        for (DamageString damageString : manager.getDamageStringList()) {
            damageString.render(VIEW_ARR, BASE_RS_ARR, consumer, partialTick, drag, bounceDecay);
        }
        bufferSource.endBatch(DamageNumberRenderer.getRenderType());
        manager.removeDead();

        poseStack.popPose();
    }
}
