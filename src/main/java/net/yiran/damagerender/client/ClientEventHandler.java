//? if forge {
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
//? if =1.19.2 {
/*import com.mojang.math.Matrix4f;
import java.nio.FloatBuffer;
*///?} else {
import org.joml.Matrix4f;
import org.joml.Quaternionf;
//?}

 // 飘字渲染入口。
 //
 // <p>每帧循环外预算一次共享的 {@code baseRS}（相机旋转×缩放），并把 view 与 baseRS 经
 // {@link Matrix4f#get(float[])} 一次性转为列主序 {@code float[16]}。
 // 循环内每个飘字由 {@link Mat4Util#mulViewTranslateBaseScale} 手写 fma 完成平移与缩放折叠，
 // 再由 {@link DamageNumberRenderer#renderNumber} 逐顶点 fma 变换，全程无 mojang 矩阵乘法与对象分配。
 //
public class ClientEventHandler {
         // 每帧复用的列主序 float[16] 矩阵缓冲，避免逐帧 new float[16] 分配。
     // 渲染在客户端主线程单线程执行，复用安全。
     //
    private static final float[] VIEW_ARR = new float[16];
    private static final float[] BASE_RS_ARR = new float[16];
//? if =1.19.2 {
    /* // view 矩阵转 float[] 复用的 FloatBuffer 包装，避免逐帧 wrap 分配。
    private static final FloatBuffer VIEW_BUF = FloatBuffer.wrap(VIEW_ARR);
    private static final FloatBuffer BASE_RS_BUF = FloatBuffer.wrap(BASE_RS_ARR);
     // baseRS 构造复用：Y 翻转×固定缩放矩阵，scale 固定无需每帧 new/createScaleMatrix。
    private static final Matrix4f FLIP_SCALE = Matrix4f.createScaleMatrix(
            -DamageString.RENDER_SCALE, DamageString.RENDER_SCALE, -DamageString.RENDER_SCALE);
*///?}

    @SubscribeEvent
    public static void onLogging(ClientPlayerNetworkEvent.LoggingIn event) {
        DamageRender.NETWORK.sendToServer(new UpdateConfigPacket(ClientConfig.SHOW_DISTANCE.get()));
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
        // 外层世界→相机相对平移（view 的一部分，原版同款）。顶点矩阵需继承此变换。
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        // viewMatrix = T(-cam)，循环内每个飘字在其基础上 post-multiply 自身变换。
        // 直接取 PoseStack 内部矩阵引用，store 后只读不写，无需拷贝。
        Matrix4f viewMatrix = poseStack.last().pose();

        // baseRS = R(相机朝向) * S(Y翻转) * S(固定缩放)，对所有飘字共享，每帧算一次。
        com.mojang.math.Quaternion cam = mc.getEntityRenderDispatcher().cameraOrientation();
        Matrix4f baseRS = new Matrix4f();
        baseRS.setIdentity();
        // 旋转矩阵 R = 相机朝向（1.19.2 Matrix4f 无 set(Quaternion)，只能经构造函数构造）
        baseRS.multiply(new Matrix4f(cam));
        // 缩放：Y翻转 * 固定缩放（FLIP_SCALE 固定不变，直接复用，免每帧 createScaleMatrix）
        baseRS.multiply(FLIP_SCALE);

        // mojang Matrix4f → 列主序 float[16]，每帧循环外一次性转换（复用缓冲与 FloatBuffer 包装）
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

        float partialTick = mc.getDeltaFrameTime();
        // 外层世界→相机相对平移（view 的一部分，原版同款）。顶点矩阵需继承此变换。
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);
        // viewMatrix = T(-cam)，循环内每个飘字在其基础上 post-multiply 自身变换
        Matrix4f viewMatrix = poseStack.last().pose();

        // baseRS = R(相机朝向) * S(Y翻转) * S(固定缩放)，对所有飘字共享，每帧算一次。
        Quaternionf cam = mc.getEntityRenderDispatcher().cameraOrientation();
        Matrix4f baseRS = new Matrix4f()
                .rotate(cam)
                .scale(-1f, 1f, -1f)
                .scale(DamageString.RENDER_SCALE);

        // org.joml Matrix4f → 列主序 float[16]，每帧循环外一次性转换
        viewMatrix.get(VIEW_ARR);
        baseRS.get(BASE_RS_ARR);
//?}

        // 帧内常量：drag 与 bounceDecay 仅依赖 partialTick，循环外算一次供所有飘字复用
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
//?} else {
/*package net.yiran.damagerender.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.data.UpdateConfigPacket;

import java.util.List;

 // 飘字渲染入口。
 //
 // <p>单线程直绘：{@code Font.drawInBatch} 本身就是高度优化的批量操作（内层循环一次处理整串字符），
 // 每个 {@code DamageString.render} 仅做一次矩阵变换 + 一次 drawInBatch 调用，
 // 单次耗时约几百纳秒。多线程拆分这种粒度的任务，同步/调度/缓存一致性开销远大于计算本身，
 // 实测几千个飘字时多线程始终慢于单线程。
 //
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

        float partialTick = event.getPartialTick().getGameTimeDeltaTicks();
        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        ClientDamageInfoManager manager = ClientDamageInfoManager.getInstance();
        List<DamageString> list = manager.getDamageStringList();
        list.removeIf(damageString -> damageString.render(poseStack,bufferSource,mc,partialTick));

        poseStack.popPose();
    }
}
*///?}
