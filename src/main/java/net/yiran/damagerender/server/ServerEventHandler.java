package net.yiran.damagerender.server;

import net.minecraft.world.damagesource.DamageSource;
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
        var pos = event.getEntity().position();
        var level = event.getEntity().level();
        DamageSource source = event.getSource();
        if (event.getNewDamage() <= 0 || source.typeHolder().unwrapKey().isEmpty()) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList()
                .getPlayers()
                .stream()
                .filter(serverPlayer -> serverPlayer.level().equals(level))
                .filter(serverPlayer -> serverPlayer.distanceTo(event.getEntity()) < ServerDamageInfoManager.instance.getDistance(serverPlayer.getStringUUID()))
                .forEach(serverPlayer -> {
                    var data = new DamageInfoData(
                            source.typeHolder().unwrapKey().get().location().toString(),
                            source.getMsgId(),
                            pos.add(0, event.getEntity().getBbHeight(), 0),
                            event.getNewDamage()
                    );
                    serverPlayer.connection.send(new DamageInfoPacket(data));
                });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        var pos = event.getEntity().position();
        var level = event.getEntity().level();
        if (event.getAmount() <= 0) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.getPlayerList()
                .getPlayers()
                .stream()
                .filter(serverPlayer -> serverPlayer.level().equals(level))
                .filter(serverPlayer -> serverPlayer.distanceTo(event.getEntity()) < ServerDamageInfoManager.instance.getDistance(serverPlayer.getStringUUID()))
                .forEach(serverPlayer -> {
                    var data = new DamageInfoData(
                            "heal",
                            "heal",
                            pos.add(0, event.getEntity().getBbHeight(), 0),
                            event.getAmount()
                    );
                    serverPlayer.connection.send(new DamageInfoPacket(data));
                });
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerDamageInfoManager.instance.clearPlayerConfigs(event.getEntity().getStringUUID());
    }
}
