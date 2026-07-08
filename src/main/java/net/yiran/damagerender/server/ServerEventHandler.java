package net.yiran.damagerender.server;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.yiran.damagerender.data.DamageInfoData;
import net.yiran.damagerender.data.DamageInfoPacket;

public class ServerEventHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (event.getNewDamage() <= 0 || source.typeHolder().unwrapKey().isEmpty()) return;

        var data = new DamageInfoData(
                source.typeHolder(),
                null,
                entity.position().add(0, entity.getBbHeight(), 0),
                event.getNewDamage()
        );
        sendToNearbyPlayers(entity, data);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0) return;

        Vec3 pos = entity.position().add(0, entity.getBbHeight(), 0);
        // heal 不是注册的 DamageType，无合法 Holder，走 fallbackKey 旁路
        var data = new DamageInfoData(null, "heal", pos, event.getAmount());
        sendToNearbyPlayers(entity, data);
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerDamageInfoManager.instance.clearPlayerConfigs(event.getEntity().getStringUUID());
    }

    /**
     * 把伤害信息包发给同维度、距离在玩家配置可见范围内的所有玩家。
     */
    private static void sendToNearbyPlayers(LivingEntity entity, DamageInfoData data) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        var level = entity.level();
        server.getPlayerList()
                .getPlayers()
                .stream()
                .filter(serverPlayer -> serverPlayer.level().equals(level))
                .filter(serverPlayer -> serverPlayer.distanceTo(entity) < ServerDamageInfoManager.instance.getDistance(serverPlayer.getStringUUID()))
                .forEach(serverPlayer -> serverPlayer.connection.send(new DamageInfoPacket(data)));
    }
}
