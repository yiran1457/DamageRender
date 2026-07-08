package net.yiran.damagerender.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.TextColor;
import net.yiran.damagerender.ClientConfig;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientDamageInfoManager {
    public static final Codec<Map<String, TextColor>> COLOR_CODEC = Codec.unboundedMap(Codec.STRING, TextColor.CODEC);
    private static final ClientDamageInfoManager INSTANCE = new ClientDamageInfoManager();
    public static final TextColor DEFAULT_COLOR = TextColor.fromRgb(0xFF5533);

    private final CopyOnWriteArrayList<DamageString> damageStringList = new CopyOnWriteArrayList<>();
    private Map<String, TextColor> damageColorMap = new HashMap<>();

    public static ClientDamageInfoManager getInstance() {
        return INSTANCE;
    }

    public TextColor getColor(DamageInfoData damageInfo) {
        TextColor color = damageColorMap.get(damageInfo.damageType());
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
