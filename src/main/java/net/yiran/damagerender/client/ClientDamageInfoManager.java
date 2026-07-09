package net.yiran.damagerender.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
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

    /**
     * 默认伤害类型 -> 颜色映射（十六进制字符串），供首次生成 damagerender-damage-color.json 与 reload 兜底复用。
     */
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

    /** 飘字列表：ObjectArrayList 比 java.util.ArrayList 省 range check 和 modCount，每帧遍历数千次有收益。 */
    private final ObjectArrayList<DamageString> damageStringList = new ObjectArrayList<>();
    /** 颜色映射：open addressing 比 HashMap chaining 快 ~20-30% on get，getColor 每条伤害调两次。 */
    private final Object2ObjectOpenHashMap<String, TextColor> damageColorMap = new Object2ObjectOpenHashMap<>();

    /**
     * 合并查找索引：entityId → (damageType → DamageString)。
     * 将 add 的合并查找从 O(n) 降到 O(1)，大量飘字时不再卡顿。
     */
    private final Int2ObjectOpenHashMap<Object2ObjectOpenHashMap<String, DamageString>> mergeIndex = new Int2ObjectOpenHashMap<>();

    public static ClientDamageInfoManager getInstance() {
        return INSTANCE;
    }

    /**
     * 构造默认颜色映射的 JSON 对象（键为伤害类型，值为 "#RRGGBB"）。
     */
    public static JsonObject defaultColorJson() {
        JsonObject json = new JsonObject();
        DEFAULT_DAMAGE_COLORS.forEach(json::addProperty);
        return json;
    }

    /**
     * 把颜色映射 JSON 解析并应用到当前实例。解析失败时不抛异常、保留旧映射（返回是否成功）。
     */
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
        if (ClientConfig.ENABLE_COMBINE_STRING.get()) {
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
        if (ClientConfig.ENABLE_COMBINE_STRING.get()) {
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

    public Object2ObjectOpenHashMap<String, TextColor> getDamageColorMap() {
        return damageColorMap;
    }

    public void setDamageColorMap(Map<String, TextColor> map) {
        this.damageColorMap.clear();
        this.damageColorMap.putAll(map);
    }
}
