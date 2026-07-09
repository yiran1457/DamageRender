package net.yiran.damagerender.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageString;

import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：一个 tick 内对同一玩家可见的多条伤害信息合批发送，减少发包次数。
 * 客户端处理逻辑与原单条 {@link DamageInfoPacket} 一致：逐条过滤最小值、取颜色、构造飘字。
 */
public class DamageInfoBatchPacket {
    public List<DamageInfoData> entries;

    public DamageInfoBatchPacket() {

    }

    public DamageInfoBatchPacket(List<DamageInfoData> entries) {
        this.entries = entries;
    }

    public static void toBytes(DamageInfoBatchPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.entries.size());
        for (DamageInfoData data : packet.entries) {
            DamageInfoData.toBytes(data, buf);
        }
    }

    public static DamageInfoBatchPacket newInstance(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        ObjectArrayList<DamageInfoData> entries = new ObjectArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(DamageInfoData.fromBytes(buf));
        }
        return new DamageInfoBatchPacket(entries);
    }

    public static void handle(DamageInfoBatchPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
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
        ctx.setPacketHandled(true);
    }
}
