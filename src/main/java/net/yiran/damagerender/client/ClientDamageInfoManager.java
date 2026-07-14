//? if forge {
package net.yiran.damagerender.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.yiran.damagerender.ClientConfig;

public class ClientDamageInfoManager {
    private static final ClientDamageInfoManager INSTANCE = new ClientDamageInfoManager();

         // 实体合并时使用的固定 typeKey，颜色用默认色。
     //
    private static final String COMBINED_KEY = "combined";

         // 飘字列表：ObjectArrayList 比 java.util.ArrayList 省 range check 和 modCount，每帧遍历数千次有收益。
     //
    private final ObjectArrayList<DamageString> damageStringList = new ObjectArrayList<>();

         // 合并查找索引：entityId → (damageType → DamageString)。
     // 将 add 的合并查找从 O(n) 降到 O(1)，大量飘字时不再卡顿。
     //
    private final Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, DamageString>> mergeIndex = new Int2ObjectOpenHashMap<>();

         // 实体级合并索引：entityId → DamageString（仅非 heal 走此索引）。
     // 开启 enableCombineEntity 时使用，同实体所有伤害合并为一个条目。
     //
    private final Int2ObjectOpenHashMap<DamageString> entityMergeIndex = new Int2ObjectOpenHashMap<>();

    public static ClientDamageInfoManager getInstance() {
        return INSTANCE;
    }

    public void add(DamageString newString) {
        boolean combineEntity = ClientConfig.ENABLE_COMBINE_STRING.get() && ClientConfig.ENABLE_COMBINE_ENTITY.get();

        // 实体级合并：同实体非 heal 伤害合并，颜色用默认色
        if (combineEntity && !"heal".equals(newString.getDamageType())) {
            int entityId = newString.getEntityId();
            DamageString existing = entityMergeIndex.get(entityId);
            if (existing != null) {
                float age = existing.getMaxLife() - existing.getLife();
                if (age <= ClientConfig.MERGE_MAX_AGE.get()) {
                    existing.mergeDamage(newString.getAmount(), newString.getX(), newString.getZ());
                    return;
                }
                // 超龄：移出索引，下面会创建新条目
                entityMergeIndex.remove(entityId);
            }
            // 创建新条目，typeKey 固定为 "combined"，颜色用默认色
            DamageString combined = new DamageString(
                    entityId,
                    newString.getX(), newString.getY(), newString.getZ(),
                    newString.getAmount(),
                    DamageColorManager.DEFAULT_COLOR.getValue(),
                    COMBINED_KEY
            );
            // 继承来源飘字的预合并放大（combined 本身是聚合体，不额外 +1%）
            combined.setMergeScale(newString.getMergeScale());
            damageStringList.add(combined);
            entityMergeIndex.put(entityId, combined);
            trimExcess();
            return;
        }

        // 原有的 type-level 合并逻辑（仅当实体合并未开启或当前为 heal 时走此分支）
        boolean combineType = ClientConfig.ENABLE_COMBINE_STRING.get();
        if (combineType) {
            int entityId = newString.getEntityId();
            String damageType = newString.getDamageType();

            var typeMap = mergeIndex.get(entityId);
            if (typeMap != null) {
                DamageString existing = typeMap.get(damageType);
                if (existing != null) {
                    float age = existing.getMaxLife() - existing.getLife();
                    if (age <= ClientConfig.MERGE_MAX_AGE.get()) {
                        existing.mergeDamage(newString.getAmount(), newString.getX(), newString.getZ());
                        return;
                    }
                    typeMap.remove(damageType);
                    if (typeMap.isEmpty()) {
                        mergeIndex.remove(entityId);
                    }
                }
            }
        }

        damageStringList.add(newString);
        if (combineType) {
            var typeMap = mergeIndex.computeIfAbsent(newString.getEntityId(), k -> new Object2ObjectOpenHashMap<>());
            typeMap.put(newString.getDamageType(), newString);
        }
        trimExcess();
    }

         // 超出上限时移除最早的条目。
     //
    private void trimExcess() {
        if (damageStringList.size() > ClientConfig.MAX_SHOW_RENDER.get()) {
            DamageString removed = damageStringList.remove(0);
            removeFromIndex(removed);
            // 实体合并索引也需清理
            if (entityMergeIndex.get(removed.getEntityId()) == removed) {
                entityMergeIndex.remove(removed.getEntityId());
            }
        }
    }

    public void removeDead() {
        damageStringList.removeIf(ds -> {
            if (ds.isDead()) {
                removeFromIndex(ds);
                if (entityMergeIndex.get(ds.getEntityId()) == ds) {
                    entityMergeIndex.remove(ds.getEntityId());
                }
                return true;
            }
            return false;
        });
    }

    private void removeFromIndex(DamageString ds) {
        var typeMap = mergeIndex.get(ds.getEntityId());
        if (typeMap != null) {
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
//?} else {
/*package net.yiran.damagerender.client;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.TextColor;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.data.DamageInfoData;

public class ClientDamageInfoManager {
    private static final ClientDamageInfoManager INSTANCE = new ClientDamageInfoManager();

     // 飘字列表：ObjectArrayList 比 java.util.ArrayList 省 range check 和 modCount，每帧遍历数千次有收益。
    private final ObjectArrayList<DamageString> damageStringList = new ObjectArrayList<>();

    public static ClientDamageInfoManager getInstance() {
        return INSTANCE;
    }

         // 取伤害颜色：委托 {@link DamageColorManager}，先查 damageTypeKey 再 fallback msgId。
     //
    public TextColor getColor(DamageInfoData damageInfo) {
        DamageColorManager m = DamageColorManager.getInstance();
        TextColor color = m.getColor(damageInfo.damageTypeKey());
        return color != DamageColorManager.DEFAULT_COLOR ? color : m.getColor(damageInfo.msgId());
    }

    public void add(DamageString newString) {
        if(ClientConfig.ENABLE_COMBINE_STRING.get()) {
            int newEntityId = newString.getEntityId();
            String newDamageType = newString.getDamageType();
            for (DamageString existing : damageStringList) {
                // 合并条件：同一实体 + 同一伤害类型（不再检测距离）
                if (existing.getEntityId() != newEntityId) {
                    continue;
                }
                if (!existing.getDamageType().equals(newDamageType)) {
                    continue;
                }

                float age = existing.getMaxLife() - existing.getLife();
                if (age <= ClientConfig.MERGE_MAX_AGE.get()) {
                    existing.mergeDamage(newString.getAmount(), newString.getX(), newString.getZ());
                    return;
                }
            }
        }

        damageStringList.add(newString);
        if (damageStringList.size() > ClientConfig.MAX_SHOW_RENDER.get()) {
            damageStringList.remove(0);
        }
    }

    public void removeDead() {
        damageStringList.removeIf(DamageString::isDead);
    }

    public ObjectArrayList<DamageString> getDamageStringList() {
        return damageStringList;
    }
}
*///?}
