package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.server.ServerDamageInfoManager;

import java.util.function.Supplier;

public class UpdateConfigPacket {
    public int distance;

    public UpdateConfigPacket() {

    }

    public UpdateConfigPacket(int distance) {
        this.distance = distance;
    }

    public static void toBytes(UpdateConfigPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.distance);
    }

    public static UpdateConfigPacket newInstance(FriendlyByteBuf buf) {
        return new UpdateConfigPacket(buf.readInt());
    }

    public static void handle(UpdateConfigPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerDamageInfoManager.instance.addDistanceConfig(ctx.getSender().getStringUUID(), packet.distance);
        });
        ctx.setPacketHandled(true);
    }
}
