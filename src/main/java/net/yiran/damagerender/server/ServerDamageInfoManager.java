package net.yiran.damagerender.server;


import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.yiran.damagerender.data.DamageInfoBatchPacket;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端伤害信息缓冲：伤害/治疗事件里把原始 {@link DamageInfoData} 攒入待发队列，
 * 在 {@link net.neoforged.neoforge.event.tick.ServerTickEvent.Post} 时按玩家就近过滤后
 * 合批发送（{@link DamageInfoBatchPacket}），减少发包次数。
 *
 * <p>伤害/治疗事件与 server tick 均在主线程触发，缓冲集合无需加锁；仍用
 * {@link ConcurrentHashMap} 仅为防御性（避免极端时序下潜在的跨线程访问问题）。
 */
public class ServerDamageInfoManager {
    public static ServerDamageInfoManager instance = new ServerDamageInfoManager();

    private static final int DEFAULT_DISTANCE = 16;

    private final Map<String, Integer> playerDistance = new ConcurrentHashMap<>();

    /** 待 flush 的原始伤害信息（事件线程写入、tick 线程读取并清空）。 */
    private final List<PendingEntry> pending = new ArrayList<>();

    public void clearPlayerConfigs(String uuid) {
        playerDistance.remove(uuid);
    }

    public int getDistance(String uuid) {
        return playerDistance.getOrDefault(uuid, DEFAULT_DISTANCE);
    }

    public void addDistanceConfig(String uuid, int distance) {
        playerDistance.put(uuid, distance);
    }

    /**
     * 事件里调用：把一条伤害/治疗信息排队，等本 tick 末尾合批发送。
     */
    public void enqueue(LivingEntity entity, DamageInfoData data) {
        pending.add(new PendingEntry(entity, data));
    }

    /**
     * ServerTickEvent.Post 调用：按玩家就近过滤，合批发送，并清空缓冲。
     */
    public void flush(MinecraftServer server) {
        if (pending.isEmpty()) return;

        // 按玩家分组收集本 tick 内对其可见的伤害信息
        Map<UUID, List<DamageInfoData>> perPlayer = new java.util.HashMap<>();
        var players = server.getPlayerList().getPlayers();
        for (PendingEntry entry : pending) {
            LivingEntity entity = entry.entity;
            var level = entity.level();
            for (var serverPlayer : players) {
                if (!serverPlayer.level().equals(level)) continue;
                double dist = serverPlayer.distanceTo(entity);
                if (dist < getDistance(serverPlayer.getStringUUID())) {
                    perPlayer.computeIfAbsent(serverPlayer.getUUID(), k -> new ArrayList<>()).add(entry.data);
                }
            }
        }
        pending.clear();

        for (var serverPlayer : players) {
            List<DamageInfoData> list = perPlayer.get(serverPlayer.getUUID());
            if (list != null && !list.isEmpty()) {
                serverPlayer.connection.send(new DamageInfoBatchPacket(list));
            }
        }
    }

    private record PendingEntry(LivingEntity entity, DamageInfoData data) {}
}
