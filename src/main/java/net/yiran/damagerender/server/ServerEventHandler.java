package net.yiran.damagerender.server;

import net.minecraft.world.entity.LivingEntity;
import net.yiran.damagerender.data.DamageInfoData;
//? if forge {
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
//?} else {
/*import net.minecraft.world.damagesource.DamageSource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
*///?}

public class ServerEventHandler {
//? if forge {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        LivingEntity entity = event.getEntity();
//? if =1.19.2 {
        /*if (event.getAmount() <= 0) return;

        var data = new DamageInfoData(
                entity.getId(),
                event.getSource().getMsgId(),
                entity.position().add(0, entity.getBbHeight(), 0),
                event.getAmount()
        );
*///?} else {
        var source = event.getSource();
        if (event.getAmount() <= 0 || source.typeHolder().unwrapKey().isEmpty()) return;

        var data = new DamageInfoData(
                entity.getId(),
                source.typeHolder().unwrapKey().get().location(),
                null,
                entity.position().add(0, entity.getBbHeight(), 0),
                event.getAmount()
        );
//?}
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }
//?}
//? if neoforge && =1.21.1 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        float damage = event.getNewDamage();
        if (damage <= 0 || source.typeHolder().unwrapKey().isEmpty()) return;

        var data = new DamageInfoData(
                entity.getId(),
                source.typeHolder(),
                null,
                entity.position().add(0, entity.getBbHeight(), 0),
                damage
        );
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }
*///?}
//? if neoforge && >1.21.1 {
    /*@SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        float damage = event.getHealthDamage();
        if (damage <= 0 || source.typeHolder().unwrapKey().isEmpty()) return;

        var data = new DamageInfoData(
                entity.getId(),
                source.typeHolder(),
                null,
                entity.position().add(0, entity.getBbHeight(), 0),
                damage
        );
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }
*///?}

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0) return;

        var pos = entity.position().add(0, entity.getBbHeight(), 0);
//? if =1.19.2 {
        /*var data = new DamageInfoData(entity.getId(), "heal", pos, event.getAmount());
*///?} else {
        var data = new DamageInfoData(entity.getId(), null, "heal", pos, event.getAmount());
//?}
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }

    /** 在服务端 tick 结束时发送本 tick 累积的伤害。 */
//? if forge {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            ServerDamageInfoManager.instance.flush(ServerLifecycleHooks.getCurrentServer());
        }
    }
//?} else {
    /*@SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerDamageInfoManager.instance.flush(event.getServer());
    }
*///?}

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerDamageInfoManager.instance.clearPlayerConfigs(event.getEntity().getStringUUID());
    }
}
