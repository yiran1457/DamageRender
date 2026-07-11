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

/**
 * 服务端 -> 客户端：一个 tick 内对同一玩家可见的多条伤害信息合批发送，减少发包次数。
 * 客户端收包时先按 (entityId, typeKey) 预合并，同实体同类型的伤害累加 amount，
 * 只把预合并后的条目加入 manager，避免上百条同类型伤害逐条 add 导致卡顿。
 */
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

    /**
     * 预合并中间结果：累加 amount，保留首次出现的位置和颜色。
     */
    private record MergedEntry(double amount, Vec3 pos, int color) {}

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
                        typeMap.put(typeKey, new MergedEntry(existing.amount() + amount, existing.pos(), existing.color()));
                    } else {
                        typeMap.put(typeKey, new MergedEntry(amount, data.pos(), DamageColorManager.getInstance().getColor(typeKey).getValue()));
                    }
                }
                // 把预合并结果加入 manager
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
                                typeEntry.getKey()
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