package net.yiran.damagerender.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageString;

import java.util.List;

/**
 * 服务端 -> 客户端：一个 tick 内对同一玩家可见的多条伤害信息合批发送，减少发包次数。
 * 客户端处理逻辑与原单条 {@link DamageInfoPacket} 一致：逐条过滤最小值、取颜色、构造飘字。
 */
public record DamageInfoBatchPacket(List<DamageInfoData> entries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DamageInfoBatchPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DamageRender.MODID, "damage_info_batch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageInfoBatchPacket> STREAM_CODEC = StreamCodec.of(
            DamageInfoBatchPacket::write,
            DamageInfoBatchPacket::read
    );

    private static void write(RegistryFriendlyByteBuf buf, DamageInfoBatchPacket packet) {
        buf.writeVarInt(packet.entries.size());
        for (DamageInfoData data : packet.entries) {
            DamageInfoData.STREAM_CODEC.encode(buf, data);
        }
    }

    private static DamageInfoBatchPacket read(RegistryFriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<DamageInfoData> entries = new ObjectArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(DamageInfoData.STREAM_CODEC.decode(buf));
        }
        return new DamageInfoBatchPacket(List.copyOf(entries));
    }

    public static void handle(DamageInfoBatchPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var manager = ClientDamageInfoManager.getInstance();
            double minValue = ClientConfig.MIN_VALUE_DISPLAY.get();
            for (DamageInfoData data : packet.entries) {
                double amount = data.amount();
                if (Math.abs(amount) < minValue) continue;

                var vec3 = data.pos();
                int color = manager.getColor(data).getValue();
                String typeKey = data.damageTypeKey();

                DamageString damageString = new DamageString(
                        data.entityId(),
                        (float) vec3.x, (float) vec3.y, (float) vec3.z,
                        (float) amount,
                        color,
                        typeKey
                );
                manager.add(damageString);
            }
        });
    }

    @Override
    public CustomPacketPayload.Type<? extends DamageInfoBatchPacket> type() {
        return TYPE;
    }
}
