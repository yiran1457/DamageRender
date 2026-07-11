package net.yiran.damagerender.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.yiran.damagerender.data.DamageInfoData;

public class ServerEventHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0) return;

        var data = new DamageInfoData(
                entity.getId(),
                event.getSource().getMsgId(),
                entity.position().add(0, entity.getBbHeight(), 0),
                event.getAmount()
        );
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0) return;

        var data = new DamageInfoData(
                entity.getId(),
                "heal",
                entity.position().add(0, entity.getBbHeight(), 0),
                event.getAmount()
        );
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerDamageInfoManager.instance.flush(ServerLifecycleHooks.getCurrentServer());
        }
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerDamageInfoManager.instance.clearPlayerConfigs(event.getEntity().getStringUUID());
    }
}