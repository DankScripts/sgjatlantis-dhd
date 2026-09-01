package com.mustangdoc.sgjpatch.client.util;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;

/**
 * Draws the locked mini Pegasus gate around the shield control.
 */
public final class AtlantisMiniGateOverlay {
    private static final int GATE_SIZE = 118;
    private static final int GATE_OFFSET = 32;
    private static final int ORB_SIZE = 74;
    private static final int ORB_OFFSET = 10;
    private static final ResourceLocation GATE_BASE = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/mini_gate/early_port/gate_base.png");
    private static final ResourceLocation IDLE_SYMBOLS = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/mini_gate/early_port/idle_symbols.png");
    private static final ResourceLocation SHIELD_ORB = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_shield_button_off_large.png");

    private AtlantisMiniGateOverlay() {}

    public static void render(AbstractButton button, GuiGraphics graphics) {
        if (button == null)
            return;

        int x = button.getX();
        int y = button.getY();
        blit(graphics, GATE_BASE, x - GATE_OFFSET, y - GATE_OFFSET, GATE_SIZE);
        blit(graphics, IDLE_SYMBOLS, x - GATE_OFFSET, y - GATE_OFFSET, GATE_SIZE);
        blit(graphics, SHIELD_ORB, x - ORB_OFFSET, y - ORB_OFFSET, ORB_SIZE);
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        graphics.blit(texture, x, y, 0F, 0F, size, size, size, size);
    }
}
