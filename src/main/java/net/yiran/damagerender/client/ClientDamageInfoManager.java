package net.yiran.damagerender.client;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.TextColor;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ClientDamageInfoManager {
    public static Codec<Map<String, TextColor>> COLOR_CODEC = Codec.unboundedMap(Codec.STRING, TextColor.CODEC);
    public static ClientDamageInfoManager instance = new ClientDamageInfoManager();
    public static TextColor DEFAULT_COLOR = TextColor.fromRgb(16733525);
    public CopyOnWriteArrayList<DamageString> damageStringList = new CopyOnWriteArrayList<>();
    public Map<String, TextColor> damageColorMap = new HashMap<>();
    public int maxListSize = 128;

    public TextColor getColor(DamageInfoData damageInfo) {
        if(damageColorMap.containsKey(damageInfo.damageType())){
            return damageColorMap.get(damageInfo.damageType());
        }
        if(damageColorMap.containsKey(damageInfo.msgId())){
            return damageColorMap.get(damageInfo.msgId());
        }
        return DEFAULT_COLOR;
    }

    public void add(DamageString damageString) {
        damageStringList.add(damageString);
        if (damageStringList.size() > maxListSize) {
            damageStringList.remove(0);
        }
    }

    public void setDamageColorMap(Map<String, TextColor> damageColorMap) {
        this.damageColorMap = damageColorMap;
    }
}
