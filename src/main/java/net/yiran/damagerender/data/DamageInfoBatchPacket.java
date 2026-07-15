//? if forge {
package net.yiran.damagerender.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.client.ClientDamagePacketHandler;

import java.util.List;
import java.util.function.Supplier;

/** Forge 使用的单 tick 服务端到客户端伤害批次数据包。 */
public class DamageInfoBatchPacket {
    public List<DamageInfoData> entries;

    public DamageInfoBatchPacket() {}

    public DamageInfoBatchPacket(List<DamageInfoData> entries) {
        this.entries = entries;
    }

    public static void toBytes(DamageInfoBatchPacket packet, FriendlyByteBuf buf) {
        var typeIndexes = new Object2IntOpenHashMap<Object>();
        typeIndexes.defaultReturnValue(-1);
        var types = new ObjectArrayList<DamageInfoData>();
        for (DamageInfoData data : packet.entries) {
            Object type = DamageInfoData.typeToken(data);
            if (typeIndexes.getInt(type) == -1) {
                typeIndexes.put(type, types.size());
                types.add(data);
            }
        }
        buf.writeVarInt(types.size());
        for (DamageInfoData type : types) {
            DamageInfoData.writeType(type, buf);
        }
        buf.writeVarInt(packet.entries.size());
        for (DamageInfoData data : packet.entries) {
            buf.writeVarInt(typeIndexes.getInt(DamageInfoData.typeToken(data)));
            DamageInfoData.writePayload(data, buf);
        }
    }

    public static DamageInfoBatchPacket newInstance(FriendlyByteBuf buf) {
        int typeCount = buf.readVarInt();
        var types = new ObjectArrayList<DamageInfoData.TypeData>(typeCount);
        for (int i = 0; i < typeCount; i++) {
            types.add(DamageInfoData.readType(buf));
        }
        int size = buf.readVarInt();
        List<DamageInfoData> entries = new ObjectArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int typeIndex = buf.readVarInt();
            int entityId = buf.readVarInt();
            if (typeIndex < 0 || typeIndex >= types.size()) {
                throw new IllegalArgumentException("Invalid damage type index: " + typeIndex);
            }
            entries.add(DamageInfoData.readPayload(buf, entityId, types.get(typeIndex)));
        }
        return new DamageInfoBatchPacket(entries);
    }

    public static void handle(DamageInfoBatchPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> ClientDamagePacketHandler.handleBatch(packet.entries));
        ctx.setPacketHandled(true);
    }
}
//?} else {
/*package net.yiran.damagerender.data;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
//? if >1.21.1 {
/^import net.minecraft.resources.Identifier;
^///?} else {
import net.minecraft.resources.ResourceLocation;
//?}
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.client.ClientDamagePacketHandler;

import java.util.List;

// NeoForge 使用的单 tick 服务端到客户端伤害批次载荷。
public record DamageInfoBatchPacket(List<DamageInfoData> entries) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<DamageInfoBatchPacket> TYPE =
//? if >1.21.1 {
            /^new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(
^///?} else {
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(
//?}
                    DamageRender.MODID,
                    "damage_info_batch"
            ));

    public static final StreamCodec<RegistryFriendlyByteBuf, DamageInfoBatchPacket> STREAM_CODEC =
            StreamCodec.of(DamageInfoBatchPacket::write, DamageInfoBatchPacket::read);

    private static void write(RegistryFriendlyByteBuf buf, DamageInfoBatchPacket packet) {
        var typeIndexes = new Object2IntOpenHashMap<Object>();
        typeIndexes.defaultReturnValue(-1);
        var types = new ObjectArrayList<DamageInfoData>();
        for (DamageInfoData data : packet.entries) {
            Object type = DamageInfoData.typeToken(data);
            if (typeIndexes.getInt(type) == -1) {
                typeIndexes.put(type, types.size());
                types.add(data);
            }
        }
        buf.writeVarInt(types.size());
        for (DamageInfoData type : types) {
            DamageInfoData.writeType(type, buf);
        }
        buf.writeVarInt(packet.entries.size());
        for (DamageInfoData data : packet.entries) {
            buf.writeVarInt(typeIndexes.getInt(DamageInfoData.typeToken(data)));
            DamageInfoData.writePayload(data, buf);
        }
    }

    private static DamageInfoBatchPacket read(RegistryFriendlyByteBuf buf) {
        int typeCount = buf.readVarInt();
        var types = new ObjectArrayList<DamageInfoData.TypeData>(typeCount);
        for (int i = 0; i < typeCount; i++) {
            types.add(DamageInfoData.readType(buf));
        }
        int size = buf.readVarInt();
        List<DamageInfoData> entries = new ObjectArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int typeIndex = buf.readVarInt();
            int entityId = buf.readVarInt();
            if (typeIndex < 0 || typeIndex >= types.size()) {
                throw new IllegalArgumentException("Invalid damage type index: " + typeIndex);
            }
            entries.add(DamageInfoData.readPayload(buf, entityId, types.get(typeIndex)));
        }
        return new DamageInfoBatchPacket(List.copyOf(entries));
    }

    public static void handle(DamageInfoBatchPacket packet, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientDamagePacketHandler.handleBatch(packet.entries));
    }

    @Override
    public CustomPacketPayload.Type<? extends DamageInfoBatchPacket> type() {
        return TYPE;
    }
}
*///?}
