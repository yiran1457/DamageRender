package net.yiran.damagerender.server;


import java.util.HashMap;

public class ServerDamageInfoManager {
    public static ServerDamageInfoManager instance = new ServerDamageInfoManager();
    public HashMap<String, Integer> PlayerDistanceConfig = new HashMap<>();

    public int getDistance(String uuid) {
        return PlayerDistanceConfig.getOrDefault(uuid, 16);
    }

    public void addDistanceConfig(String uuid, int distance) {
        PlayerDistanceConfig.put(uuid, distance);
    }

    public void clearPlayerConfigs(String uuid) {
        PlayerDistanceConfig.remove(uuid);
    }
}
