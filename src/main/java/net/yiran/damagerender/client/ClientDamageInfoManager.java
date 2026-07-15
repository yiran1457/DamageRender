package net.yiran.damagerender.client;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.yiran.damagerender.ClientConfig;

/** 维护活跃飘字及其常数时间合并索引。 */
public class ClientDamageInfoManager {
    private static final ClientDamageInfoManager INSTANCE = new ClientDamageInfoManager();
    private static final String COMBINED_KEY = "combined";

    private final ObjectArrayList<DamageString> damageStringList = new ObjectArrayList<>();
    private final Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, DamageString>> mergeIndex =
            new Int2ObjectOpenHashMap<>();
    private final Int2ObjectOpenHashMap<DamageString> entityMergeIndex = new Int2ObjectOpenHashMap<>();

    public static ClientDamageInfoManager getInstance() {
        return INSTANCE;
    }

    public void add(DamageString newString) {
        boolean combineType = ClientConfig.ENABLE_COMBINE_STRING.get();
        boolean combineEntity = combineType && ClientConfig.ENABLE_COMBINE_ENTITY.get();

        if (combineEntity && !"heal".equals(newString.getDamageType())) {
            int entityId = newString.getEntityId();
            DamageString existing = entityMergeIndex.get(entityId);
            if (existing != null) {
                float age = existing.getMaxLife() - existing.getLife();
                if (age <= ClientConfig.MERGE_MAX_AGE.get()) {
                    existing.mergeDamage(newString.getAmount(), newString.getX(), newString.getZ());
                    return;
                }
                entityMergeIndex.remove(entityId);
            }

            DamageString combined = new DamageString(
                    entityId,
                    newString.getX(), newString.getY(), newString.getZ(),
                    newString.getAmount(),
                    DamageColorManager.DEFAULT_COLOR.getValue(),
                    COMBINED_KEY
            );
            combined.setMergeScale(newString.getMergeScale());
            damageStringList.add(combined);
            entityMergeIndex.put(entityId, combined);
            trimExcess();
            return;
        }

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
                    if (typeMap.isEmpty()) mergeIndex.remove(entityId);
                }
            }
        }

        damageStringList.add(newString);
        if (combineType) {
            mergeIndex.computeIfAbsent(newString.getEntityId(), key -> new Object2ObjectOpenHashMap<>())
                    .put(newString.getDamageType(), newString);
        }
        trimExcess();
    }

    private void trimExcess() {
        if (damageStringList.size() <= ClientConfig.MAX_SHOW_RENDER.get()) return;

        DamageString removed = damageStringList.remove(0);
        removeFromIndexes(removed);
    }

    public void removeDead() {
        damageStringList.removeIf(damageString -> {
            if (!damageString.isDead()) return false;
            removeFromIndexes(damageString);
            return true;
        });
    }

    private void removeFromIndexes(DamageString damageString) {
        int entityId = damageString.getEntityId();
        var typeMap = mergeIndex.get(entityId);
        if (typeMap != null && typeMap.get(damageString.getDamageType()) == damageString) {
            typeMap.remove(damageString.getDamageType());
            if (typeMap.isEmpty()) mergeIndex.remove(entityId);
        }
        if (entityMergeIndex.get(entityId) == damageString) {
            entityMergeIndex.remove(entityId);
        }
    }

    public ObjectArrayList<DamageString> getDamageStringList() {
        return damageStringList;
    }
}
