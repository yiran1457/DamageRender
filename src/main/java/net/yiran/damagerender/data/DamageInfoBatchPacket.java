package net.yiran.damagerender.data;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.client.ClientDamageInfoManager;
import net.yiran.damagerender.client.DamageString;

import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 -> 客户端：一个 tick 内对同一玩家可见的多条伤害信息合批发送，减少发包次数。
 * 客户端收包时先按 (entityId, damageType) 预合并，同实体同类型的伤害累加 amount，
 * 只把预合并后的条目加入 manager，避免上百条同类型伤害逐条 add 导致卡顿。
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

    /** 预合并中间结果：累加 amount，保留首次出现的位置和类型信息。 */
    private record MergedEntry(double amount, Vec3 pos, String typeKey, int color) {}

    public static void handle(DamageInfoBatchPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var manager = ClientDamageInfoManager.getInstance();
            double minValue = ClientConfig.MIN_VALUE_DISPLAY.get();

            // 预合并：按 (entityId, damageType) 累加 amount，O(n) 一次遍历
            Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, MergedEntry>> mergeMap = null;
            if (ClientConfig.ENABLE_COMBINE_STRING.get()) {
                mergeMap = new Int2ObjectOpenHashMap<>();
                for (DamageInfoData data : packet.entries) {
                    double amount = data.amount();
                    if (Math.abs(amount) < minValue) continue;

                    int entityId = data.entityId();
                    String typeKey = data.damageTypeKey();
                    var typeMap = mergeMap.computeIfAbsent(entityId, k -> new Object2ObjectOpenHashMap<>());
                    MergedEntry existing = typeMap.get(typeKey);
                    if (existing != null) {
                        // 累加 amount，保留首次位置
                        typeMap.put(typeKey, new MergedEntry(existing.amount + amount, existing.pos, existing.typeKey, existing.color));
                    } else {
                        var vec3 = data.pos();
                        int color = manager.getColor(data).getValue();
                        typeMap.put(typeKey, new MergedEntry(amount, vec3, typeKey, color));
                    }
                }
                // 把预合并结果加入 manager
                for (var entityEntry : mergeMap.int2ObjectEntrySet()) {
                    int entityId = entityEntry.getIntKey();
                    for (var typeEntry : entityEntry.getValue().object2ObjectEntrySet()) {
                        MergedEntry merged = typeEntry.getValue();
                        DamageString ds = new DamageString(
                                entityId,
                                (float) merged.pos.x, (float) merged.pos.y, (float) merged.pos.z,
                                (float) merged.amount,
                                merged.color,
                                merged.typeKey
                        );
                        manager.add(ds);
                    }
                }
            } else {
                // 未启用合并，直接逐条加入
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
            }
        });
        ctx.setPacketHandled(true);
    }
}
