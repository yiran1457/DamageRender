package net.yiran.damagerender.server;

import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.yiran.damagerender.DamageRender;
import net.yiran.damagerender.data.DamageInfoData;
import net.yiran.damagerender.data.DamageInfoPacket;

public class ServerEventHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        var pos = event.getEntity().position();
        var level = event.getEntity().level();
        if (event.getAmount() <= 0) return;
        ServerLifecycleHooks.getCurrentServer()
                .getPlayerList()
                .getPlayers()
                .stream()
                .filter(serverPlayer -> serverPlayer.level().equals(level))
                .filter(serverPlayer -> serverPlayer.distanceTo(event.getEntity()) < ServerDamageInfoManager.instance.getDistance(serverPlayer.getStringUUID()))
                .forEach(serverPlayer -> {
                    var data = new DamageInfoData(
                            event.getSource().typeHolder().unwrapKey().get().location().toString(),
                            event.getSource().getMsgId(),
                            pos.add(0, event.getEntity().getBbHeight(), 0),
                            event.getAmount()
                    );
                    DamageRender.NETWORK.sendTo(new DamageInfoPacket(data), serverPlayer);
                });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        var pos = event.getEntity().position();
        var level = event.getEntity().level();
        if (event.getAmount() <= 0) return;
        ServerLifecycleHooks.getCurrentServer()
                .getPlayerList()
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
                    DamageRender.NETWORK.sendTo(new DamageInfoPacket(data), serverPlayer);
                });
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerDamageInfoManager.instance.clearPlayerConfigs(event.getEntity().getStringUUID());
    }
}
