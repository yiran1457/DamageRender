//? if forge {
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
//?} else {
/*package net.yiran.damagerender.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >1.21.1 {
import net.minecraft.resources.Identifier;
//?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.server.ServerDamageInfoManager;

 // 客户端 -> 服务端：客户端上报自己的渲染可见距离。
 //
public record UpdateConfigPacket(int distance) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<UpdateConfigPacket> TYPE =
//? if >1.21.1 {
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(DamageRender.MODID, "update_config"));
//?} else {
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DamageRender.MODID, "update_config"));
//?}

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
*///?}
