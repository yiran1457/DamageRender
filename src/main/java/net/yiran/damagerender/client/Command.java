package net.yiran.damagerender.client;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import static net.yiran.damagerender.DamageRender.reloadDamageColorMap;

/**
 * 客户端命令。
 * 数值/布尔类配置项已迁移到 Mods 列表的配置界面，这里仅保留
 * 颜色映射重载：damagerender-damage-color.json 是自由格式 JSON，ModConfigSpec 不支持，
 * 命令是它的唯一重载入口。
 */
public class Command {
    @SubscribeEvent
    public static void commandRegister(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("damagerender")
                        .then(
                                Commands.literal("setDamageColor")
                                        .then(
                                                Commands.literal("reload")
                                                        .executes(ctx -> {
                                                            reloadDamageColorMap();
                                                            ctx.getSource().sendSuccess(() -> Component.literal("已重载伤害颜色映射"), false);
                                                            return 1;
                                                        })
                                        )
                        )
        );
    }
}
