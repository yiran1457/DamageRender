package net.yiran.damagerender.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.yiran.damagerender.ClientConfig;

public class ClientDamageInfoManager {
    private static final ClientDamageInfoManager INSTANCE = new ClientDamageInfoManager();

    /** 飘字列表：ObjectArrayList 比 java.util.ArrayList 省 range check 和 modCount，每帧遍历数千次有收益。 */
    private final ObjectArrayList<DamageString> damageStringList = new ObjectArrayList<>();

    /**
     * 合并查找索引：entityId → (damageType → DamageString)。
     * 将 add 的合并查找从 O(n) 降到 O(1)，大量飘字时不再卡顿。
     */
    private final Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, DamageString>> mergeIndex = new Int2ObjectOpenHashMap<>();

    public static ClientDamageInfoManager getInstance() {
        return INSTANCE;
    }

    public void add(DamageString newString) {
        boolean combineEnabled = ClientConfig.ENABLE_COMBINE_STRING.get();
        if (combineEnabled) {
            int entityId = newString.getEntityId();
            String damageType = newString.getDamageType();

            // O(1) 合并查找：通过索引直接定位同实体同类型的飘字
            var typeMap = mergeIndex.get(entityId);
            if (typeMap != null) {
                DamageString existing = typeMap.get(damageType);
                if (existing != null) {
                    float age = existing.getMaxLife() - existing.getLife();
                    if (age <= ClientConfig.MERGE_MAX_AGE.get()) {
                        existing.mergeDamage(newString.getAmount());
                        return;
                    }
                    // 已超龄，从索引移除（下面会重新添加新条目）
                    typeMap.remove(damageType);
                    if (typeMap.isEmpty()) {
                        mergeIndex.remove(entityId);
                    }
                }
            }
        }

        damageStringList.add(newString);
        if (combineEnabled) {
            var typeMap = mergeIndex.computeIfAbsent(newString.getEntityId(), k -> new Object2ObjectOpenHashMap<>());
            typeMap.put(newString.getDamageType(), newString);
        }
        if (damageStringList.size() > ClientConfig.MAX_SHOW_RENDER.get()) {
            DamageString removed = damageStringList.remove(0);
            removeFromIndex(removed);
        }
    }

    /**
     * 从飘字列表和合并索引中移除已死亡的条目。由渲染帧末尾调用。
     */
    public void removeDead() {
        damageStringList.removeIf(ds -> {
            if (ds.isDead()) {
                removeFromIndex(ds);
                return true;
            }
            return false;
        });
    }

    /**
     * 从合并索引中移除指定飘字（仅当索引仍指向该条目时才移除，避免误删已替换的新条目）。
     */
    private void removeFromIndex(DamageString ds) {
        var typeMap = mergeIndex.get(ds.getEntityId());
        if (typeMap != null) {
            // 仅当索引仍指向该条目时才移除，避免误删已替换的新条目
            if (typeMap.get(ds.getDamageType()) == ds) {
                typeMap.remove(ds.getDamageType());
                if (typeMap.isEmpty()) {
                    mergeIndex.remove(ds.getEntityId());
                }
            }
        }
    }

    public ObjectArrayList<DamageString> getDamageStringList() {
        return damageStringList;
    }
}
