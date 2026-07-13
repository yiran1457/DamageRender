//? if forge {
//? if =1.19.2 {
/*package net.yiran.damagerender.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageColorManager;
import net.yiran.damagerender.client.DamageString;

import java.util.List;
import java.util.function.Supplier;

 // 服务端 -> 客户端：一个 tick 内对同一玩家可见的多条伤害信息合批发送，减少发包次数。
 // 客户端收包时先按 (entityId, typeKey) 预合并，同实体同类型的伤害累加 amount，
 // 只把预合并后的条目加入 manager，避免上百条同类型伤害逐条 add 导致卡顿。
 //
public class DamageInfoBatchPacket {
    public List<DamageInfoData> entries;

    public DamageInfoBatchPacket() {}

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

         // 预合并中间结果：累加 amount，保留首次出现的位置和颜色，并记录合并段数（用于放大因子）。
     //
    private record MergedEntry(double amount, Vec3 pos, int color, int count) {}

    public static void handle(DamageInfoBatchPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var manager = ClientDamageInfoManager.getInstance();
            double minValue = ClientConfig.MIN_VALUE_DISPLAY.get();
            boolean showHeal = ClientConfig.SHOW_HEAL_NUMBERS.get();

            if (ClientConfig.ENABLE_COMBINE_STRING.get()) {
                // 预合并：按 (entityId, typeKey) 累加 amount，O(n) 一次遍历
                var mergeMap = new Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, MergedEntry>>();
                for (DamageInfoData data : packet.entries) {
                    double amount = data.amount();
                    if (Math.abs(amount) < minValue) continue;
                    if (!showHeal && "heal".equals(data.typeKey())) continue;

                    int entityId = data.entityId();
                    String typeKey = data.typeKey();
                    var typeMap = mergeMap.computeIfAbsent(entityId, k -> new Object2ObjectOpenHashMap<>());
                    MergedEntry existing = typeMap.get(typeKey);
                    if (existing != null) {
                        // 合并段数 +1，对应 DamageString 构造时的放大因子
                        typeMap.put(typeKey, new MergedEntry(existing.amount() + amount, existing.pos(), existing.color(), existing.count() + 1));
                    } else {
                        typeMap.put(typeKey, new MergedEntry(amount, data.pos(), DamageColorManager.getInstance().getColor(typeKey).getValue(), 0));
                    }
                }
                // 把预合并结果加入 manager，count 作为初始合并次数驱动放大
                for (var entityEntry : mergeMap.int2ObjectEntrySet()) {
                    int entityId = entityEntry.getIntKey();
                    for (var typeEntry : entityEntry.getValue().object2ObjectEntrySet()) {
                        MergedEntry merged = typeEntry.getValue();
                        Vec3 pos = merged.pos();
                        manager.add(new DamageString(
                                entityId,
                                (float) pos.x(), (float) pos.y(), (float) pos.z(),
                                (float) merged.amount(),
                                merged.color(),
                                typeEntry.getKey(),
                                merged.count()
                        ));
                    }
                }
            } else {
                // 未启用合并，直接逐条加入
                for (DamageInfoData data : packet.entries) {
                    double amount = data.amount();
                    if (Math.abs(amount) < minValue) continue;
                    if (!showHeal && "heal".equals(data.typeKey())) continue;

                    Vec3 pos = data.pos();
                    manager.add(new DamageString(
                            data.entityId(),
                            (float) pos.x(), (float) pos.y(), (float) pos.z(),
                            (float) amount,
                            DamageColorManager.getInstance().getColor(data.typeKey()).getValue(),
                            data.typeKey()
                    ));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
*///?} else {
package net.yiran.damagerender.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageColorManager;
import net.yiran.damagerender.client.DamageString;

import java.util.List;
import java.util.function.Supplier;

 // 服务端 -> 客户端：一个 tick 内对同一玩家可见的多条伤害信息合批发送，减少发包次数。
 // 客户端收包时先按 (entityId, damageType) 预合并，同实体同类型的伤害累加 amount，
 // 只把预合并后的条目加入 manager，避免上百条同类型伤害逐条 add 导致卡顿。
 //
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

         // 预合并中间结果：累加 amount，保留首次出现的位置和颜色，并记录合并段数（用于放大因子）。
     //
    private record MergedEntry(double amount, Vec3 pos, int color, int count) {}

    public static void handle(DamageInfoBatchPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var manager = ClientDamageInfoManager.getInstance();
            double minValue = ClientConfig.MIN_VALUE_DISPLAY.get();
            boolean showHeal = ClientConfig.SHOW_HEAL_NUMBERS.get();

            if (ClientConfig.ENABLE_COMBINE_STRING.get()) {
                // 预合并：按 (entityId, damageType) 累加 amount，O(n) 一次遍历。
                // 同一 tick 内的伤害无条件累加（不受 MERGE_MAX_AGE 限制），是 manager.add 合并之外的补充。
                var mergeMap = new Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, MergedEntry>>();
                for (DamageInfoData data : packet.entries) {
                    double amount = data.amount();
                    if (Math.abs(amount) < minValue) continue;
                    if (!showHeal && "heal".equals(data.fallbackKey())) continue;

                    int entityId = data.entityId();
                    String typeKey = data.damageTypeKey();
                    var typeMap = mergeMap.computeIfAbsent(entityId, k -> new Object2ObjectOpenHashMap<>());
                    MergedEntry existing = typeMap.get(typeKey);
                    if (existing != null) {
                        // 合并段数 +1，对应 DamageString 构造时的放大因子
                        typeMap.put(typeKey, new MergedEntry(existing.amount() + amount, existing.pos(), existing.color(), existing.count() + 1));
                    } else {
                        typeMap.put(typeKey, new MergedEntry(amount, data.pos(), DamageColorManager.getInstance().getColor(typeKey).getValue(), 0));
                    }
                }
                // 把预合并结果加入 manager，count 作为初始合并次数驱动放大
                for (var entityEntry : mergeMap.int2ObjectEntrySet()) {
                    int entityId = entityEntry.getIntKey();
                    for (var typeEntry : entityEntry.getValue().object2ObjectEntrySet()) {
                        MergedEntry merged = typeEntry.getValue();
                        Vec3 pos = merged.pos();
                        manager.add(new DamageString(
                                entityId,
                                (float) pos.x(), (float) pos.y(), (float) pos.z(),
                                (float) merged.amount(),
                                merged.color(),
                                typeEntry.getKey(),
                                merged.count()
                        ));
                    }
                }
            } else {
                // 未启用合并，直接逐条加入
                for (DamageInfoData data : packet.entries) {
                    double amount = data.amount();
                    if (Math.abs(amount) < minValue) continue;
                    if (!showHeal && "heal".equals(data.fallbackKey())) continue;

                    Vec3 pos = data.pos();
                    manager.add(new DamageString(
                            data.entityId(),
                            (float) pos.x(), (float) pos.y(), (float) pos.z(),
                            (float) amount,
                            DamageColorManager.getInstance().getColor(data.damageTypeKey()).getValue(),
                            data.damageTypeKey()
                    ));
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
//?}
//?} else {
/*package net.yiran.damagerender.data;

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

 // 服务端 -> 客户端：一个 tick 内对同一玩家可见的多条伤害信息合批发送，减少发包次数。
 // 客户端处理逻辑与原单条 {@link DamageInfoPacket} 一致：逐条过滤最小值、取颜色、构造飘字。
 //
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
*///?}