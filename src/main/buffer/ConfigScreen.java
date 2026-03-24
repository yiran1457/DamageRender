package net.yiran.damagerender;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigScreen extends Screen {
    @Nullable
    public Screen lastScreen;
    public int yOffset;
    public Set<String> damageTypes;

    public ConfigScreen(@Nullable Screen lastScreen) {
        super(Component.literal("Config"));
        this.lastScreen = lastScreen;
        damageTypes = new HashSet<>();
        if (Minecraft.getInstance().level != null) {
            for (DamageType damageType : Minecraft.getInstance().level.damageSources().damageTypes) {
                damageTypes.add(damageType.msgId());
            }
        }else {
            damageTypes.addAll(DamageRender.CONFIG.damageColorMap.keySet());
        }
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (Minecraft.getInstance().level == null) {
            renderDirtBackground(pGuiGraphics);
        }
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        for (GuiEventListener child : children()) {
            if (child instanceof Button button) {
                button.setY((int) (button.getY() + pDelta * 10));
                yOffset = (int) (pDelta * 10);
            }
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    @Override
    protected void init() {
        super.init();
        int halfWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth()/2;
        int halfHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight()/2;
        int x = 0;
        int y = 0;
        for (String id : damageTypes){
            addRenderableWidget(
                    Button.builder(Component.literal(id), button -> {})
                            .bounds(halfWidth-50,y+=23,100,20)
                            .build()
            );
        }/*
        if (Minecraft.getInstance().level != null) {
            for (DamageType damageType : Minecraft.getInstance().level.damageSources().damageTypes) {
                addRenderableWidget(
                        Button.builder(Component.literal(damageType.msgId()),button -> {})
                                .bounds(x,y,50,20)
                                .build()
                );
                y+=23;
            }
        }*/
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(lastScreen);
    }

    //添加按钮
    @Mod.EventBusSubscriber(modid = DamageRender.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class Handle {
        @SubscribeEvent
        public static void screenInit(ScreenEvent.Init.Post event) {
            if (event.getScreen() instanceof OptionsScreen optionsScreen) {
                optionsScreen.addRenderableWidget(Button.builder(Component.translatable("damagerender.configscreen.button"), button -> Minecraft.getInstance().setScreen(new ConfigScreen(optionsScreen))).bounds(0, 0, 60, 20).tooltip(Tooltip.create(Component.translatable("damagerender.configscreen.button.tooltip"))).build());
            }
        }
    }
}
