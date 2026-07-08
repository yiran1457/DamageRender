package net.yiran.damagerender.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.server.ServerDamageInfoManager;

/**
 * 客户端 -> 服务端：客户端上报自己的渲染可见距离。
 */
public record UpdateConfigPacket(int distance) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateConfigPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DamageRender.MODID, "update_config"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateConfigPacket> STREAM_CODEC =
            StreamCodec.of(UpdateConfigPacket::write, UpdateConfigPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, UpdateConfigPacket packet) {
        buf.writeInt(packet.distance);
    }

    private static UpdateConfigPacket read(RegistryFriendlyByteBuf buf) {
        return new UpdateConfigPacket(buf.readInt());
    }

    @Override
    public CustomPacketPayload.Type<? extends UpdateConfigPacket> type() {
        return TYPE;
    }

    public static void handle(UpdateConfigPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer serverPlayer) {
                ServerDamageInfoManager.instance.addDistanceConfig(serverPlayer.getStringUUID(), packet.distance);
            }
        });
    }
}
