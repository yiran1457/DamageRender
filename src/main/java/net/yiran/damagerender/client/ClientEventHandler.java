package net.yiran.damagerender.client;

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

/**
 * 飘字渲染入口。
 *
 * <p>单线程直绘：{@code Font.drawInBatch} 本身就是高度优化的批量操作（内层循环一次处理整串字符），
 * 每个 {@code DamageString.render} 仅做一次矩阵变换 + 一次 drawInBatch 调用，
 * 单次耗时约几百纳秒。多线程拆分这种粒度的任务，同步/调度/缓存一致性开销远大于计算本身，
 * 实测几千个飘字时多线程始终慢于单线程。
 */
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
