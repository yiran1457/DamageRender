package net.yiran.damagerender.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.network.chat.TextColor;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private final CopyOnWriteArrayList<DamageString> damageStringList = new CopyOnWriteArrayList<>();
    private Map<String, TextColor> damageColorMap = new HashMap<>();

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
        if(ClientConfig.ENABLE_COMBINE_STRING.get()) {
            for (DamageString existing : damageStringList) {
                if (!existing.getDamageType().equals(newString.getDamageType())) {
                    continue;
                }

                double dx = existing.getX() - newString.getX();
                double dy = existing.getY() - newString.getY();
                double dz = existing.getZ() - newString.getZ();
                if (dx * dx + dy * dy + dz * dz > ClientConfig.MERGE_DISTANCE_SQ.get()) {
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

    public CopyOnWriteArrayList<DamageString> getDamageStringList() {
        return damageStringList;
    }

    public void setDamageColorMap(Map<String, TextColor> damageColorMap) {
        this.damageColorMap = damageColorMap;
    }

    public Map<String, TextColor> getDamageColorMap() {
        return damageColorMap;
    }
}
