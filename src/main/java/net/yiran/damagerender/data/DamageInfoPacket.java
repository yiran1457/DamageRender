package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageColorManager;
import net.yiran.damagerender.client.DamageString;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：发送一次伤害信息用于渲染飘字。
 */
public class DamageInfoPacket {
    public DamageInfoData data;

    public DamageInfoPacket() {

    }

    public DamageInfoPacket(DamageInfoData damageInfoData) {
        this.data = damageInfoData;
    }

    public static void toBytes(DamageInfoPacket packet, FriendlyByteBuf buf) {
        DamageInfoData.toBytes(packet.data, buf);
    }

    public static DamageInfoPacket newInstance(FriendlyByteBuf buf) {
        return new DamageInfoPacket(DamageInfoData.fromBytes(buf));
    }

    public static void handle(DamageInfoPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            double amount = packet.data.amount();
            if (Math.abs(amount) < ClientConfig.MIN_VALUE_DISPLAY.get()) return;
            if (!ClientConfig.SHOW_HEAL_NUMBERS.get() && "heal".equals(packet.data.fallbackKey())) return;

            var vec3 = packet.data.pos();
            int color = DamageColorManager.getInstance().getColor(packet.data).getValue();

            String typeKey = packet.data.damageTypeKey();

            DamageString damageString = new DamageString(
                    packet.data.entityId(),
                    (float) vec3.x, (float) vec3.y, (float) vec3.z,
                    (float) amount,
                    color,
                    typeKey
            );
            ClientDamageInfoManager.getInstance().add(damageString);
        });
        ctx.setPacketHandled(true);
    }
}
