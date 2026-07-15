package net.yiran.damagerender.server;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.yiran.damagerender.data.DamageInfoBatchPacket;
import net.yiran.damagerender.data.DamageInfoData;
//? if forge {
import net.minecraftforge.network.PacketDistributor;
import net.yiran.damagerender.DamageRender;
//?}

/**
 * 缓冲一个服务端 tick 内的伤害事件，并按玩家可见范围批量发送。
 * 所有访问都在服务端线程执行，因此 fastutil 容器无需额外同步。
 */
public class ServerDamageInfoManager {
    public static final ServerDamageInfoManager instance = new ServerDamageInfoManager();

    private static final int DEFAULT_DISTANCE = 16;

    private final Object2IntOpenHashMap<String> playerDistance =
            new Object2IntOpenHashMap<>(DEFAULT_DISTANCE);
    private final ObjectArrayList<PendingEntry> pending = new ObjectArrayList<>();

    public void clearPlayerConfigs(String uuid) {
        playerDistance.removeInt(uuid);
    }

    public int getDistance(String uuid) {
        return playerDistance.getOrDefault(uuid, DEFAULT_DISTANCE);
    }

    public void addDistanceConfig(String uuid, int distance) {
        playerDistance.put(uuid, distance);
    }

    public void enqueue(LivingEntity entity, DamageInfoData data) {
        pending.add(new PendingEntry(entity, data));
    }

    public void flush(MinecraftServer server) {
        if (pending.isEmpty()) return;

        var players = server.getPlayerList().getPlayers();
        int playerCount = players.size();
        if (playerCount == 0) {
            pending.clear();
            return;
        }

        var playersByLevel = new Object2ObjectOpenHashMap<Level, ObjectArrayList<PlayerTarget>>();
        for (int i = 0; i < playerCount; i++) {
            ServerPlayer player = players.get(i);
//? if =1.19.2 {
            /*Level level = player.getLevel();
*///?} else {
            Level level = player.level();
//?}
            int distance = getDistance(player.getStringUUID());
            playersByLevel.computeIfAbsent(level, key -> new ObjectArrayList<>())
                    .add(new PlayerTarget(player, (double) distance * distance));
        }

        for (int i = 0, len = pending.size(); i < len; i++) {
            PendingEntry entry = pending.get(i);
            LivingEntity entity = entry.entity;
//? if =1.19.2 {
            /*Level level = entity.getLevel();
*///?} else {
            Level level = entity.level();
//?}
            var targets = playersByLevel.get(level);
            if (targets == null) continue;
            for (int j = 0, targetCount = targets.size(); j < targetCount; j++) {
                PlayerTarget target = targets.get(j);
                if (target.player.distanceToSqr(entity) < target.distanceSqr) {
                    target.add(entry.data);
                }
            }
        }
        pending.clear();

        for (var targets : playersByLevel.values()) {
            for (int i = 0, targetCount = targets.size(); i < targetCount; i++) {
                PlayerTarget target = targets.get(i);
                if (target.entries == null) continue;

                DamageInfoBatchPacket packet = new DamageInfoBatchPacket(target.entries);
//? if forge {
                DamageRender.NETWORK.send(PacketDistributor.PLAYER.with(target::player), packet);
//?} else {
                /*target.player.connection.send(packet);
*///?}
            }
        }
    }

    private record PendingEntry(LivingEntity entity, DamageInfoData data) {}

    /** Per-tick recipient state; the payload list is allocated only when it receives an event. */
    private static final class PlayerTarget {
        private final ServerPlayer player;
        private final double distanceSqr;
        private ObjectArrayList<DamageInfoData> entries;

        private PlayerTarget(ServerPlayer player, double distanceSqr) {
            this.player = player;
            this.distanceSqr = distanceSqr;
        }

        private void add(DamageInfoData data) {
            if (entries == null) entries = new ObjectArrayList<>();
            entries.add(data);
        }

        private ServerPlayer player() {
            return player;
        }
    }
}
