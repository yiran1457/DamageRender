package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageString;

import java.util.function.Supplier;

public class DamageInfoPacket {
    public DamageInfoData data;

    public DamageInfoPacket() {

    }

    public DamageInfoPacket(DamageInfoData damageInfoData) {
        this.data = damageInfoData;
    }

    public static void toBytes(DamageInfoPacket packet, FriendlyByteBuf buf) {
        buf.writeJsonWithCodec(DamageInfoData.CODEC, packet.data);
    }

    public static DamageInfoPacket newInstance(FriendlyByteBuf buf) {
        DamageInfoPacket packet = new DamageInfoPacket();
        packet.data = buf.readJsonWithCodec(DamageInfoData.CODEC);
        return packet;
    }

    public static void handle(DamageInfoPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            double amount = packet.data.amount();
            if (Math.abs(amount) < ClientConfig.MIN_VALUE_DISPLAY.get()) return;

            var vec3 = packet.data.pos();
            int color = ClientDamageInfoManager.getInstance().getColor(packet.data).getValue();

            String typeKey = packet.data.damageType() != null ? packet.data.damageType() : packet.data.msgId();

            DamageString damageString = new DamageString(
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
