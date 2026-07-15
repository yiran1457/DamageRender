package net.yiran.damagerender.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.phys.Vec3;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.List;

/** 与加载器无关的客户端伤害数据处理入口。 */
public final class ClientDamagePacketHandler {
    private ClientDamagePacketHandler() {}

    public static void handle(DamageInfoData data) {
        double minValue = ClientConfig.MIN_VALUE_DISPLAY.get();
        boolean showHeal = ClientConfig.SHOW_HEAL_NUMBERS.get();
        if (!shouldRender(data, minValue, showHeal)) return;

        ClientDamageInfoManager.getInstance().add(toDamageString(data));
    }

    public static void handleBatch(List<DamageInfoData> entries) {
        double minValue = ClientConfig.MIN_VALUE_DISPLAY.get();
        boolean showHeal = ClientConfig.SHOW_HEAL_NUMBERS.get();
        ClientDamageInfoManager manager = ClientDamageInfoManager.getInstance();

        if (!ClientConfig.ENABLE_COMBINE_STRING.get()) {
            for (DamageInfoData data : entries) {
                if (shouldRender(data, minValue, showHeal)) {
                    manager.add(toDamageString(data));
                }
            }
            return;
        }

        var mergeMap = new Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, MergedEntry>>();
        for (DamageInfoData data : entries) {
            if (!shouldRender(data, minValue, showHeal)) continue;

            int entityId = data.entityId();
            String typeKey = data.damageTypeKey();
            var typeMap = mergeMap.computeIfAbsent(entityId, key -> new Object2ObjectOpenHashMap<>());
            MergedEntry existing = typeMap.get(typeKey);
            if (existing == null) {
                typeMap.put(typeKey, new MergedEntry(
                        data.amount(),
                        data.pos(),
                        resolveColor(data),
                        0
                ));
            } else {
                typeMap.put(typeKey, new MergedEntry(
                        existing.amount() + data.amount(),
                        existing.pos(),
                        existing.color(),
                        existing.count() + 1
                ));
            }
        }

        for (var entityEntry : mergeMap.int2ObjectEntrySet()) {
            int entityId = entityEntry.getIntKey();
            for (var typeEntry : entityEntry.getValue().object2ObjectEntrySet()) {
                MergedEntry merged = typeEntry.getValue();
                Vec3 pos = merged.pos();
                manager.add(new DamageString(
                        entityId,
                        (float) pos.x, (float) pos.y, (float) pos.z,
                        (float) merged.amount(),
                        merged.color(),
                        typeEntry.getKey(),
                        merged.count()
                ));
            }
        }
    }

    private static boolean shouldRender(DamageInfoData data, double minValue, boolean showHeal) {
        return Math.abs(data.amount()) >= minValue
                && (showHeal || !"heal".equals(data.damageTypeKey()));
    }

    private static DamageString toDamageString(DamageInfoData data) {
        Vec3 pos = data.pos();
        return new DamageString(
                data.entityId(),
                (float) pos.x, (float) pos.y, (float) pos.z,
                (float) data.amount(),
                resolveColor(data),
                data.damageTypeKey()
        );
    }

    private static int resolveColor(DamageInfoData data) {
        DamageColorManager manager = DamageColorManager.getInstance();
        TextColor color = manager.getColor(data.damageTypeKey());
        if (color == DamageColorManager.DEFAULT_COLOR) {
            color = manager.getColor(data.msgId());
        }
        return color.getValue();
    }

    private record MergedEntry(double amount, Vec3 pos, int color, int count) {}
}
