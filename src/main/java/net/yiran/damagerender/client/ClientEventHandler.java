package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.UpdateConfigPacket;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * 飘字渲染入口。
 *
 * <p>每帧在循环外预算一次共享的"相机旋转×缩放"矩阵 {@code baseRS}（所有飘字共用同一相机朝向与固定 scale）。
 * 外层 PoseStack 仍保留 {@code translate(-cameraPos)}（世界→相机相对，view 的一部分），
 * 循环内每个飘字用复用 Matrix4f 在 view 之上 post-multiply 自身平移与 baseRS，
 * 与原 {@code pushPose; translate; mulPose; scale} 同构，但只复制 4×4（无 normal）、无栈分配、无四元数转换。
 * 顶点发射由 {@link DamageNumberRenderer#renderNumber} 用复用向量就地变换完成，无逐顶点分配。
 */
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
        Vec3 cameraPos = camera.getPosition();
        Minecraft mc = Minecraft.getInstance();

        float partialTick = mc.getDeltaFrameTime();
        // 外层世界→相机相对平移（view 的一部分，原版同款）。顶点矩阵需继承此变换。
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        // viewMatrix = T(-cam)，循环内每个飘字在其基础上 post-multiply 自身变换
        Matrix4f viewMatrix = poseStack.last().pose();

        // baseRS = R(相机朝向) * S(Y翻转) * S(固定缩放)，对所有飘字共享，每帧算一次。
        // post-multiply 语义与原 DamageString 的 mulPose(cameraOrientation) + scale(-s,s,-s) 同构。
        Quaternionf cam = mc.getEntityRenderDispatcher().cameraOrientation();
        Matrix4f baseRS = new Matrix4f()
                .rotate(cam)
                .scale(-1f, 1f, -1f)
                .scale(DamageString.RENDER_SCALE);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(DamageNumberRenderer.getRenderType());
        ClientDamageInfoManager manager = ClientDamageInfoManager.getInstance();
        for (DamageString damageString : manager.getDamageStringList()) {
            damageString.render(viewMatrix, baseRS, consumer, partialTick);
        }
        bufferSource.endBatch(DamageNumberRenderer.getRenderType());
        manager.removeDead();

        poseStack.popPose();
    }
}
