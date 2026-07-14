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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.chat.TextColor;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.Map;

public class ClientDamageInfoManager {
    public static final Codec<Map<String, TextColor>> COLOR_CODEC = Codec.unboundedMap(Codec.STRING, TextColor.CODEC);
    private static final ClientDamageInfoManager INSTANCE = new ClientDamageInfoManager();
    public static final TextColor DEFAULT_COLOR = TextColor.fromRgb(0xFF5533);

         // 默认伤害类型 -> 颜色映射（十六进制字符串），供首次生成 damagerender-damage-color.json 与 reload 兜底复用。
     //
    private static final Map<String, String> DEFAULT_DAMAGE_COLORS = Map.ofEntries(
            Map.entry("magic", DamageRender.getHexColor(-7722014)),
            Map.entry("lightningBolt", DamageRender.getHexColor(-256)),
            Map.entry("lava", DamageRender.getHexColor(-65536)),
            Map.entry("indirectMagic", DamageRender.getHexColor(-7722014)),
            Map.entry("freeze", DamageRender.getHexColor(-16711681)),
            Map.entry("witherSkull", DamageRender.getHexColor(-14221237)),
            Map.entry("inFire", DamageRender.getHexColor(-65536)),
            Map.entry("onFire", DamageRender.getHexColor(-65536)),
            Map.entry("wither", DamageRender.getHexColor(-14221237)),
            Map.entry("heal", "#00FF00")
    );

     // 飘字列表：ObjectArrayList 比 java.util.ArrayList 省 range check 和 modCount，每帧遍历数千次有收益。
    private final ObjectArrayList<DamageString> damageStringList = new ObjectArrayList<>();
     // 颜色映射：open addressing 比 HashMap chaining 快 ~20-30% on get，getColor 每条伤害调两次。
    private final Object2ObjectOpenHashMap<String, TextColor> damageColorMap = new Object2ObjectOpenHashMap<>();

    public static ClientDamageInfoManager getInstance() {
        return INSTANCE;
    }

         // 构造默认颜色映射的 JSON 对象（键为伤害类型，值为 "#RRGGBB"）。
     //
    public static JsonObject defaultColorJson() {
        JsonObject json = new JsonObject();
        DEFAULT_DAMAGE_COLORS.forEach(json::addProperty);
        return json;
    }

         // 把颜色映射 JSON 解析并应用到当前实例。解析失败时不抛异常、保留旧映射（返回是否成功）。
     //
    public boolean parseAndApply(JsonElement json) {
        if (json == null) return false;
        var parsed = COLOR_CODEC.parse(JsonOps.INSTANCE, json).result();
        if (parsed.isEmpty()) return false;
        setDamageColorMap(parsed.get());
        return true;
    }

    public TextColor getColor(DamageInfoData damageInfo) {
        TextColor color = damageColorMap.get(damageInfo.damageTypeKey());
        if (color != null) return color;
        color = damageColorMap.get(damageInfo.msgId());
        return color != null ? color : DEFAULT_COLOR;
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
                    existing.mergeDamage(newString.getAmount());
                    return;
                }
            }
        }

        damageStringList.add(newString);
        if (damageStringList.size() > ClientConfig.MAX_SHOW_RENDER.get()) {
            damageStringList.remove(0);
        }
    }

    public ObjectArrayList<DamageString> getDamageStringList() {
        return damageStringList;
    }

    public Object2ObjectOpenHashMap<String, TextColor> getDamageColorMap() {
        return damageColorMap;
    }

    public void setDamageColorMap(Map<String, TextColor> map) {
        this.damageColorMap.clear();
        this.damageColorMap.putAll(map);
    }
}
*///?}
