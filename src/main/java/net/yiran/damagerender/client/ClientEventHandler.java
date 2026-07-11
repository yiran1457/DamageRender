package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
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

/**
 * 飘字渲染入口。
 *
 * <p>每帧循环外预算一次共享的 {@code baseRS}（相机旋转×缩放），并把 view 与 baseRS 经
 * {@link com.mojang.math.Matrix4f#store(java.nio.FloatBuffer)} 一次性转为列主序 {@code float[16]}。
 * 循环内每个飘字由 {@link Mat4Util#mulViewTranslateBaseScale} 手写 fma 完成平移与缩放折叠，
 * 再由 {@link DamageNumberRenderer#renderNumber} 逐顶点 fma 变换，全程无 mojang 矩阵乘法与对象分配。
 */
public class ClientEventHandler {
    @SubscribeEvent
    public static void onLogging(ClientPlayerNetworkEvent.LoggingIn event) {
        DamageRender.NETWORK.sendToServer(new UpdateConfigPacket(ClientConfig.SHOW_DISTANCE.get()));
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) return;

        PoseStack poseStack = event.getPoseStack();
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cameraPos = camera.getPosition();

        float partialTick = mc.getDeltaFrameTime();
        // 外层世界→相机相对平移（view 的一部分，原版同款）。顶点矩阵需继承此变换。
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        // viewMatrix = T(-cam)，循环内每个飘字在其基础上 post-multiply 自身变换
        Matrix4f viewMatrix = new Matrix4f(poseStack.last().pose());

        // baseRS = R(相机朝向) * S(Y翻转) * S(固定缩放)，对所有飘字共享，每帧算一次。
        Quaternion cam = mc.getEntityRenderDispatcher().cameraOrientation();
        Matrix4f baseRS = new Matrix4f();
        baseRS.setIdentity();
        // 旋转矩阵 R = 相机朝向
        baseRS.multiply(new Matrix4f(cam));
        // 缩放：Y翻转 * 固定缩放
        float s = DamageString.RENDER_SCALE;
        baseRS.multiply(Matrix4f.createScaleMatrix(-s, s, -s));

        // mojang Matrix4f → 列主序 float[16]，每帧循环外一次性转换
        float[] viewArr = new float[16];
        float[] baseRSArr = new float[16];
        viewMatrix.store(java.nio.FloatBuffer.wrap(viewArr));
        baseRS.store(java.nio.FloatBuffer.wrap(baseRSArr));

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(DamageNumberRenderer.getRenderType());
        ClientDamageInfoManager manager = ClientDamageInfoManager.getInstance();
        for (DamageString damageString : manager.getDamageStringList()) {
            damageString.render(viewArr, baseRSArr, consumer, partialTick);
        }
        bufferSource.endBatch(DamageNumberRenderer.getRenderType());
        manager.removeDead();

        poseStack.popPose();
    }
}