//? if forge {
//? if =1.19.2 {
/*package net.yiran.damagerender.server;

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
*///?} else {
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
        var source = event.getSource();
        if (event.getAmount() <= 0 || source.typeHolder().unwrapKey().isEmpty()) return;

        var data = new DamageInfoData(
                entity.getId(),
                source.typeHolder().unwrapKey().get().location(),
                null,
                entity.position().add(0, entity.getBbHeight(), 0),
                event.getAmount()
        );
        // 攒入缓冲，本 tick 末尾合批发送
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0) return;

        var pos = entity.position().add(0, entity.getBbHeight(), 0);
        // heal 不是注册的 DamageType，无合法 location，走 fallbackKey 旁路
        var data = new DamageInfoData(entity.getId(), null, "heal", pos, event.getAmount());
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }

         // 每 tick 末尾：把本 tick 攒下的伤害信息按玩家就近过滤后合批发送。
     //
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
//?}
//?} else {
/*package net.yiran.damagerender.server;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.yiran.damagerender.data.DamageInfoData;

public class ServerEventHandler {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity entity = event.getEntity();
        DamageSource source = event.getSource();
        if (event.getNewDamage() <= 0 || source.typeHolder().unwrapKey().isEmpty()) return;

        var data = new DamageInfoData(
                entity.getId(),
                source.typeHolder(),
                null,
                entity.position().add(0, entity.getBbHeight(), 0),
                event.getNewDamage()
        );
        // 攒入缓冲，本 tick 末尾合批发送
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onHeal(LivingHealEvent event) {
        LivingEntity entity = event.getEntity();
        if (event.getAmount() <= 0) return;

        Vec3 pos = entity.position().add(0, entity.getBbHeight(), 0);
        // heal 不是注册的 DamageType，无合法 Holder，走 fallbackKey 旁路
        var data = new DamageInfoData(entity.getId(), null, "heal", pos, event.getAmount());
        ServerDamageInfoManager.instance.enqueue(entity, data);
    }

         // 每 tick 末尾：把本 tick 攒下的伤害信息按玩家就近过滤后合批发送。
     //
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        ServerDamageInfoManager.instance.flush(event.getServer());
    }

    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        ServerDamageInfoManager.instance.clearPlayerConfigs(event.getEntity().getStringUUID());
    }
}
*///?}