package net.yiran.damagerender.server;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerDamageInfoManager {
    public static ServerDamageInfoManager instance = new ServerDamageInfoManager();

    private static final int DEFAULT_DISTANCE = 16;

    private final Map<String, Integer> playerDistance = new ConcurrentHashMap<>();

    public int getDistance(String uuid) {
        return playerDistance.getOrDefault(uuid, DEFAULT_DISTANCE);
    }

    public void addDistanceConfig(String uuid, int distance) {
        playerDistance.put(uuid, distance);
    }

    public void clearPlayerConfigs(String uuid) {
        playerDistance.remove(uuid);
    }
}
