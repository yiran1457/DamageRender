package net.yiran.damagerender.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageString;

/**
 * 服务端 -> 客户端：发送一次伤害信息用于渲染飘字。
 */
public record DamageInfoPacket(DamageInfoData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DamageInfoPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DamageRender.MODID, "damage_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageInfoPacket> STREAM_CODEC =
            StreamCodec.composite(
                    DamageInfoData.STREAM_CODEC, DamageInfoPacket::data,
                    DamageInfoPacket::new
            );

    @Override
    public CustomPacketPayload.Type<? extends DamageInfoPacket> type() {
        return TYPE;
    }

    public static void handle(DamageInfoPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            double amount = packet.data.amount();
            if (Math.abs(amount) < ClientConfig.MIN_VALUE_DISPLAY.get()) return;

            var vec3 = packet.data.pos();
            int color = ClientDamageInfoManager.getInstance().getColor(packet.data).getValue();

            String typeKey = packet.data.damageTypeKey();

            DamageString damageString = new DamageString(
                    (float) vec3.x, (float) vec3.y, (float) vec3.z,
                    (float) amount,
                    color,
                    typeKey
            );
            ClientDamageInfoManager.getInstance().add(damageString);
        });
    }
}
