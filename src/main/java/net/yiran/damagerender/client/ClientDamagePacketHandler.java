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
        String typeKey = data.damageTypeKey();
        if (!shouldRender(data.amount(), typeKey, minValue, showHeal)) return;

        ClientDamageInfoManager.getInstance().add(toDamageString(data, typeKey, resolveColor(data, typeKey)));
    }

    public static void handleBatch(List<DamageInfoData> entries) {
        double minValue = ClientConfig.MIN_VALUE_DISPLAY.get();
        boolean showHeal = ClientConfig.SHOW_HEAL_NUMBERS.get();
        ClientDamageInfoManager manager = ClientDamageInfoManager.getInstance();

        if (!ClientConfig.ENABLE_COMBINE_STRING.get()) {
            for (DamageInfoData data : entries) {
                String typeKey = data.damageTypeKey();
                if (shouldRender(data.amount(), typeKey, minValue, showHeal)) {
                    manager.add(toDamageString(data, typeKey, resolveColor(data, typeKey)));
                }
            }
            return;
        }

        var mergeMap = new Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, MergedEntry>>();
        for (DamageInfoData data : entries) {
            String typeKey = data.damageTypeKey();
            if (!shouldRender(data.amount(), typeKey, minValue, showHeal)) continue;

            int entityId = data.entityId();
            var typeMap = mergeMap.computeIfAbsent(entityId, key -> new Object2ObjectOpenHashMap<>());
            MergedEntry existing = typeMap.get(typeKey);
            if (existing == null) {
                typeMap.put(typeKey, new MergedEntry(
                        data.amount(),
                        data.pos(),
                        resolveColor(data, typeKey),
                        0
                ));
            } else {
                existing.add(data.amount());
            }
        }

        for (var entityEntry : mergeMap.int2ObjectEntrySet()) {
            int entityId = entityEntry.getIntKey();
            for (var typeEntry : entityEntry.getValue().object2ObjectEntrySet()) {
                MergedEntry merged = typeEntry.getValue();
                Vec3 pos = merged.pos;
                manager.add(new DamageString(
                        entityId,
                        (float) pos.x, (float) pos.y, (float) pos.z,
                        (float) merged.amount,
                        merged.color,
                        typeEntry.getKey(),
                        merged.count
                ));
            }
        }
    }

    private static boolean shouldRender(double amount, String typeKey, double minValue, boolean showHeal) {
        return Math.abs(amount) >= minValue && (showHeal || !"heal".equals(typeKey));
    }

    private static DamageString toDamageString(DamageInfoData data, String typeKey, int color) {
        Vec3 pos = data.pos();
        return new DamageString(
                data.entityId(),
                (float) pos.x, (float) pos.y, (float) pos.z,
                (float) data.amount(),
                color,
                typeKey
        );
    }

    private static int resolveColor(DamageInfoData data, String typeKey) {
        DamageColorManager manager = DamageColorManager.getInstance();
        TextColor color = manager.getColor(typeKey);
        if (color == DamageColorManager.DEFAULT_COLOR) {
            color = manager.getColor(data.msgId());
        }
        return color.getValue();
    }

    private static final class MergedEntry {
        private double amount;
        private final Vec3 pos;
        private final int color;
        private int count;

        private MergedEntry(double amount, Vec3 pos, int color, int count) {
            this.amount = amount;
            this.pos = pos;
            this.color = color;
            this.count = count;
        }

        private void add(double additional) {
            amount += additional;
            count++;
        }
    }
}
