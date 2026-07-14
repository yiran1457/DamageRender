//? if forge {
package net.yiran.damagerender.data;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.client.ClientDamagePacketHandler;

import java.util.function.Supplier;

/** Server-to-client packet containing one damage entry. */
public class DamageInfoPacket {
    public DamageInfoData data;

    public DamageInfoPacket() {}

    public DamageInfoPacket(DamageInfoData data) {
        this.data = data;
    }

    public static void toBytes(DamageInfoPacket packet, FriendlyByteBuf buf) {
        DamageInfoData.toBytes(packet.data, buf);
    }

    public static DamageInfoPacket newInstance(FriendlyByteBuf buf) {
        return new DamageInfoPacket(DamageInfoData.fromBytes(buf));
    }

    public static void handle(DamageInfoPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientDamagePacketHandler.handle(packet.data));
        ctx.setPacketHandled(true);
    }
}
//?} else {
/*package net.yiran.damagerender.data;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.client.ClientDamagePacketHandler;

// Server-to-client payload containing one damage entry.
public record DamageInfoPacket(DamageInfoData data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DamageInfoPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(DamageRender.MODID, "damage_info"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageInfoPacket> STREAM_CODEC =
            StreamCodec.composite(
                    DamageInfoData.STREAM_CODEC, DamageInfoPacket::data,
                    DamageInfoPacket::new
            );

    public static void handle(DamageInfoPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientDamagePacketHandler.handle(packet.data));
    }

    @Override
    public CustomPacketPayload.Type<? extends DamageInfoPacket> type() {
        return TYPE;
    }
}
*///?}
