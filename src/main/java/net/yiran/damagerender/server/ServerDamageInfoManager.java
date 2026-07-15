package net.yiran.damagerender.server;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
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

import java.util.UUID;

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

        ServerPlayer[] playerArr = new ServerPlayer[playerCount];
        Level[] playerLevels = new Level[playerCount];
        UUID[] playerUuids = new UUID[playerCount];
        int[] playerDistances = new int[playerCount];
        for (int i = 0; i < playerCount; i++) {
            ServerPlayer player = players.get(i);
            playerArr[i] = player;
//? if =1.19.2 {
            /*playerLevels[i] = player.getLevel();
*///?} else {
            playerLevels[i] = player.level();
//?}
            playerUuids[i] = player.getUUID();
            playerDistances[i] = getDistance(player.getStringUUID());
        }

        var perPlayer = new Object2ObjectOpenHashMap<UUID, ObjectArrayList<DamageInfoData>>();
        for (int i = 0, len = pending.size(); i < len; i++) {
            PendingEntry entry = pending.get(i);
            LivingEntity entity = entry.entity;
//? if =1.19.2 {
            /*Level level = entity.getLevel();
*///?} else {
            Level level = entity.level();
//?}
            for (int j = 0; j < playerCount; j++) {
                if (!playerLevels[j].equals(level)) continue;
                if (playerArr[j].distanceTo(entity) < playerDistances[j]) {
                    perPlayer.computeIfAbsent(playerUuids[j], key -> new ObjectArrayList<>())
                            .add(entry.data);
                }
            }
        }
        pending.clear();

        for (int i = 0; i < playerCount; i++) {
            ObjectArrayList<DamageInfoData> entries = perPlayer.get(playerUuids[i]);
            if (entries == null || entries.isEmpty()) continue;

            ServerPlayer target = playerArr[i];
            DamageInfoBatchPacket packet = new DamageInfoBatchPacket(entries);
//? if forge {
            DamageRender.NETWORK.send(PacketDistributor.PLAYER.with(() -> target), packet);
//?} else {
            /*target.connection.send(packet);
*///?}
        }
    }

    private record PendingEntry(LivingEntity entity, DamageInfoData data) {}
}
