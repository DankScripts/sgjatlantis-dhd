package com.mustangdoc.sgjpatch.mixin.client;

import com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed;
import java.lang.reflect.Field;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed$AtlantisTriangleSymbolButton")
public abstract class AtlantisTriangleSymbolButtonRenderMixin extends AbstractButton {
    @Shadow
    private AbstractDHDMenu<?> menu;
    @Shadow
    private int displaySymbol;
    @Shadow
    private int actionSymbol;
    @Shadow
    private boolean up;
    @Shadow
    private int size;
    @Shadow
    private AtlantisDHDScreenFixed screen;

    protected AtlantisTriangleSymbolButtonRenderMixin(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void sgjpatch$renderConstellationMode(
            GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callbackInfo) {
        if (Screen.hasControlDown()) {
            return;
        }

        boolean engaged = false;
        if (this.actionSymbol >= 0 && this.actionSymbol <= 38) {
            try {
                engaged = this.menu.isSymbolEngaged(this.actionSymbol);
            } catch (Throwable ignored) {
            }

            if (!engaged && this.screen != null) {
                try {
                    Field field = AtlantisDHDScreenFixed.class.getDeclaredField("engagedLatch");
                    field.setAccessible(true);
                    Object value = field.get(this.screen);
                    if (value instanceof boolean[] latch && this.actionSymbol < latch.length) {
                        engaged = latch[this.actionSymbol];
                    }
                } catch (Throwable ignored) {
                }
            }
        }

        int frame = engaged ? 2 : (this.isHoveredOrFocused() ? 1 : 0);
        boolean tiny = this.size <= 10;
        int frameSize = tiny ? 10 : 20;
        ResourceLocation triangle = ResourceLocation.fromNamespaceAndPath(
                "sgjpatch",
                tiny
                        ? (this.up ? "textures/gui/atlantis_dhd_triangle_tiny_up.png" : "textures/gui/atlantis_dhd_triangle_tiny_down.png")
                        : (this.up ? "textures/gui/atlantis_dhd_crystal_outer_green_up.png" : "textures/gui/atlantis_dhd_crystal_outer_green_down.png"));
        graphics.blit(triangle, this.getX(), this.getY(), 0, frame * frameSize,
                frameSize, frameSize, frameSize, frameSize * 3);

        if (this.displaySymbol >= 0 && this.displaySymbol <= 38) {
            int symbolSize = tiny ? 6 : 14;
            int columns = 7;
            int rows = 6;
            int u = this.displaySymbol % columns * symbolSize;
            int v = this.displaySymbol / columns * symbolSize;
            ResourceLocation symbols = ResourceLocation.fromNamespaceAndPath(
                    "sgjpatch",
                    engaged
                            ? (tiny ? "textures/gui/green_constellation_sheet_tiny.png" : "textures/gui/green_constellation_sheet.png")
                            : (tiny ? "textures/gui/red_constellation_sheet_tiny.png" : "textures/gui/red_constellation_sheet.png"));
            int x = this.getX() + (this.size - symbolSize) / 2;
            int y = this.getY() + (this.size - symbolSize) / 2;
            graphics.blit(symbols, x, y, u, v, symbolSize, symbolSize,
                    columns * symbolSize, rows * symbolSize);
        }

        callbackInfo.cancel();
    }
}