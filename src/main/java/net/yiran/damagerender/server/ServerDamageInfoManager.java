//? if forge {
//? if =1.19.2 {
/*package net.yiran.damagerender.server;


import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.DamageInfoBatchPacket;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.UUID;

 // 服务端伤害信息缓冲：伤害/治疗事件里把原始 {@link DamageInfoData} 攒入待发队列，
 // 在 {@link net.minecraftforge.event.TickEvent.ServerTickEvent} (Phase.END) 时按玩家就近过滤后
 // 合批发送（{@link DamageInfoBatchPacket}），减少发包次数。
 //
 // <p>伤害/治疗事件与 server tick 均在主线程触发，缓冲集合无需加锁。
 // 使用 fastutil 的 {@link Object2IntOpenHashMap}（原始 int 存储，无 Integer 装箱）
 // 和 {@link ObjectArrayList}（无 range check / modCount）替代标准库集合。
 //
public class ServerDamageInfoManager {
    public static ServerDamageInfoManager instance = new ServerDamageInfoManager();

    private static final int DEFAULT_DISTANCE = 16;

     // 玩家 UUID → 可见距离。Object2IntOpenHashMap 原始 int 存储，无 Integer 装箱开销。 
    private final Object2IntOpenHashMap<String> playerDistance = new Object2IntOpenHashMap<>(DEFAULT_DISTANCE);

     // 待 flush 的原始伤害信息。ObjectArrayList 无 range check，比 ArrayList 快。 
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

         // 事件里调用：把一条伤害/治疗信息排队，等本 tick 末尾合批发送。
     //
    public void enqueue(LivingEntity entity, DamageInfoData data) {
        pending.add(new PendingEntry(entity, data));
    }

         // ServerTickEvent (Phase.END) 调用：按玩家就近过滤，合批发送，并清空缓冲。
     //
    public void flush(MinecraftServer server) {
        if (pending.isEmpty()) return;

        var players = server.getPlayerList().getPlayers();
        int playerCount = players.size();
        if (playerCount == 0) {
            pending.clear();
            return;
        }

        // 预取每个玩家的不变属性（level / uuid / stringUUID / 可见距离），避免内层 O(pending×players) 循环里重复调用
        ServerPlayer[] playerArr = new ServerPlayer[playerCount];
        Level[] playerLevels = new Level[playerCount];
        UUID[] playerUuids = new UUID[playerCount];
        int[] playerDistances = new int[playerCount];
        for (int i = 0; i < playerCount; i++) {
            ServerPlayer p = players.get(i);
            playerArr[i] = p;
            playerLevels[i] = p.getLevel();
            playerUuids[i] = p.getUUID();
            playerDistances[i] = getDistance(p.getStringUUID());
        }

        // 按玩家分组收集本 tick 内对其可见的伤害信息
        // Object2ObjectOpenHashMap: open addressing 比 HashMap chaining 快 ~20-30%
        var perPlayer = new Object2ObjectOpenHashMap<UUID, ObjectArrayList<DamageInfoData>>();
        for (int i = 0, len = pending.size(); i < len; i++) {
            PendingEntry entry = pending.get(i);
            LivingEntity entity = entry.entity;
            Level level = entity.getLevel();
            for (int j = 0; j < playerCount; j++) {
                if (!playerLevels[j].equals(level)) continue;
                double dist = playerArr[j].distanceTo(entity);
                if (dist < playerDistances[j]) {
                    perPlayer.computeIfAbsent(playerUuids[j], k -> new ObjectArrayList<>()).add(entry.data);
                }
            }
        }
        pending.clear();

        for (int j = 0; j < playerCount; j++) {
            ObjectArrayList<DamageInfoData> list = perPlayer.get(playerUuids[j]);
            if (list != null && !list.isEmpty()) {
                ServerPlayer target = playerArr[j];
                DamageRender.NETWORK.send(PacketDistributor.PLAYER.with(() -> target), new DamageInfoBatchPacket(list));
            }
        }
    }

    private record PendingEntry(LivingEntity entity, DamageInfoData data) {}
}
*///?} else {
package net.yiran.damagerender.server;


import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PacketDistributor;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.DamageInfoBatchPacket;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.UUID;

 // 服务端伤害信息缓冲：伤害/治疗事件里把原始 {@link DamageInfoData} 攒入待发队列，
 // 在 {@link net.minecraftforge.event.TickEvent.ServerTickEvent} (Phase.END) 时按玩家就近过滤后
 // 合批发送（{@link DamageInfoBatchPacket}），减少发包次数。
 // 
 // <p>伤害/治疗事件与 server tick 均在主线程触发，缓冲集合无需加锁。
 // 使用 fastutil 的 {@link Object2IntOpenHashMap}（原始 int 存储，无 Integer 装箱）
 // 和 {@link ObjectArrayList}（无 range check / modCount）替代标准库集合。
 //
public class ServerDamageInfoManager {
    public static ServerDamageInfoManager instance = new ServerDamageInfoManager();

    private static final int DEFAULT_DISTANCE = 16;

     // 玩家 UUID → 可见距离。Object2IntOpenHashMap 原始 int 存储，无 Integer 装箱开销。 
    private final Object2IntOpenHashMap<String> playerDistance = new Object2IntOpenHashMap<>(DEFAULT_DISTANCE);

     // 待 flush 的原始伤害信息。ObjectArrayList 无 range check，比 ArrayList 快。 
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

         // 事件里调用：把一条伤害/治疗信息排队，等本 tick 末尾合批发送。
     //
    public void enqueue(LivingEntity entity, DamageInfoData data) {
        pending.add(new PendingEntry(entity, data));
    }

         // ServerTickEvent (Phase.END) 调用：按玩家就近过滤，合批发送，并清空缓冲。
     //
    public void flush(MinecraftServer server) {
        if (pending.isEmpty()) return;

        var players = server.getPlayerList().getPlayers();
        int playerCount = players.size();
        if (playerCount == 0) {
            pending.clear();
            return;
        }

        // 预取每个玩家的不变属性（level / uuid / stringUUID / 可见距离），避免内层 O(pending×players) 循环里重复调用
        ServerPlayer[] playerArr = new ServerPlayer[playerCount];
        Level[] playerLevels = new Level[playerCount];
        UUID[] playerUuids = new UUID[playerCount];
        int[] playerDistances = new int[playerCount];
        for (int i = 0; i < playerCount; i++) {
            ServerPlayer p = players.get(i);
            playerArr[i] = p;
            playerLevels[i] = p.level();
            playerUuids[i] = p.getUUID();
            playerDistances[i] = getDistance(p.getStringUUID());
        }

        // 按玩家分组收集本 tick 内对其可见的伤害信息
        // Object2ObjectOpenHashMap: open addressing 比 HashMap chaining 快 ~20-30%
        var perPlayer = new Object2ObjectOpenHashMap<UUID, ObjectArrayList<DamageInfoData>>();
        for (int i = 0, len = pending.size(); i < len; i++) {
            PendingEntry entry = pending.get(i);
            LivingEntity entity = entry.entity;
            Level level = entity.level();
            for (int j = 0; j < playerCount; j++) {
                if (!playerLevels[j].equals(level)) continue;
                double dist = playerArr[j].distanceTo(entity);
                if (dist < playerDistances[j]) {
                    perPlayer.computeIfAbsent(playerUuids[j], k -> new ObjectArrayList<>()).add(entry.data);
                }
            }
        }
        pending.clear();

        for (int j = 0; j < playerCount; j++) {
            ObjectArrayList<DamageInfoData> list = perPlayer.get(playerUuids[j]);
            if (list != null && !list.isEmpty()) {
                ServerPlayer target = playerArr[j];
                DamageRender.NETWORK.send(PacketDistributor.PLAYER.with(() -> target), new DamageInfoBatchPacket(list));
            }
        }
    }

    private record PendingEntry(LivingEntity entity, DamageInfoData data) {}
}
//?}
//?} else {
/*package net.yiran.damagerender.server;


import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.LivingEntity;
import net.yiran.damagerender.data.DamageInfoBatchPacket;
import net.yiran.damagerender.data.DamageInfoData;

import java.util.UUID;

 // 服务端伤害信息缓冲：伤害/治疗事件里把原始 {@link DamageInfoData} 攒入待发队列，
 // 在 {@link net.neoforged.neoforge.event.tick.ServerTickEvent.Post} 时按玩家就近过滤后
 // 合批发送（{@link DamageInfoBatchPacket}），减少发包次数。
 //
 // <p>伤害/治疗事件与 server tick 均在主线程触发，缓冲集合无需加锁。
 // 使用 fastutil 的 {@link Object2IntOpenHashMap}（原始 int 存储，无 Integer 装箱）
 // 和 {@link ObjectArrayList}（无 range check / modCount）替代标准库集合。
 //
public class ServerDamageInfoManager {
    public static ServerDamageInfoManager instance = new ServerDamageInfoManager();

    private static final int DEFAULT_DISTANCE = 16;

     // 玩家 UUID → 可见距离。Object2IntOpenHashMap 原始 int 存储，无 Integer 装箱开销。 
    private final Object2IntOpenHashMap<String> playerDistance = new Object2IntOpenHashMap<>(DEFAULT_DISTANCE);

     // 待 flush 的原始伤害信息。ObjectArrayList 无 range check，比 ArrayList 快。 
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

         // 事件里调用：把一条伤害/治疗信息排队，等本 tick 末尾合批发送。
     //
    public void enqueue(LivingEntity entity, DamageInfoData data) {
        pending.add(new PendingEntry(entity, data));
    }

         // ServerTickEvent.Post 调用：按玩家就近过滤，合批发送，并清空缓冲。
     //
    public void flush(MinecraftServer server) {
        if (pending.isEmpty()) return;

        // 按玩家分组收集本 tick 内对其可见的伤害信息
        // Object2ObjectOpenHashMap: open addressing 比 HashMap chaining 快 ~20-30%
        var perPlayer = new Object2ObjectOpenHashMap<UUID, ObjectArrayList<DamageInfoData>>();
        var players = server.getPlayerList().getPlayers();
        for (int i = 0, len = pending.size(); i < len; i++) {
            PendingEntry entry = pending.get(i);
            LivingEntity entity = entry.entity;
            var level = entity.level();
            for (var serverPlayer : players) {
                if (!serverPlayer.level().equals(level)) continue;
                double dist = serverPlayer.distanceTo(entity);
                if (dist < getDistance(serverPlayer.getStringUUID())) {
                    perPlayer.computeIfAbsent(serverPlayer.getUUID(), k -> new ObjectArrayList<>()).add(entry.data);
                }
            }
        }
        pending.clear();

        for (var serverPlayer : players) {
            ObjectArrayList<DamageInfoData> list = perPlayer.get(serverPlayer.getUUID());
            if (list != null && !list.isEmpty()) {
                serverPlayer.connection.send(new DamageInfoBatchPacket(list));
            }
        }
    }

    private record PendingEntry(LivingEntity entity, DamageInfoData data) {}
}
*///?}