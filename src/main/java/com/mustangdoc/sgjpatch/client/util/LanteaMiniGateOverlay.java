package com.mustangdoc.sgjpatch.client.util;

import com.mustangdoc.sgjpatch.compat.LanteaEnergyIrisState;
import com.mustangdoc.sgjpatch.compat.LanteaIrisBridge;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shows a nearby Lantea gate's dial and iris state on the existing Atlantis DHD mini gate. */
public final class LanteaMiniGateOverlay {
    private static final int SIZE = 118;
    private LanteaMiniGateOverlay() { }

    public static boolean render(BlockEntity dhd, AbstractButton button, GuiGraphics graphics) {
        if (dhd == null || button == null || dhd.getLevel() == null) return false;
        BlockEntity candidate = LanteaIrisBridge.nearest(dhd.getLevel(), dhd.getBlockPos(), false);
        if (!(candidate instanceof LanteaEnergyIrisState iris)) return false;
        int x = button.getX() - 32, y = button.getY() - 32;
        blit(graphics, "textures/gui/mini_gate/early_port/gate_base.png", x, y, SIZE);
        blit(graphics, "textures/gui/mini_gate/early_port/idle_symbols.png", x, y, SIZE);
        int chevrons = 0;
        int addressLength = 0;
        try {
            Object snapshot = candidate.getClass().getMethod("snapshot").invoke(candidate);
            chevrons = (int) snapshot.getClass().getMethod("chevrons").invoke(snapshot);
            addressLength = (int) snapshot.getClass().getMethod("length").invoke(snapshot);
        } catch (ReflectiveOperationException ignored) { }
        int[] physicalOrder = {8, 7, 6, 3, 2, 1, 5, 4};
        for (int index = 0; index < Math.min(9, chevrons); index++) {
            int slot = addressLength >= 7 && index == addressLength - 1
                    ? 0 : physicalOrder[Math.min(index, physicalOrder.length - 1)];
            blit(graphics, "textures/gui/mini_gate/early_port/chevron_slot_" + slot + ".png", x, y, SIZE);
        }
        blit(graphics, iris.sgjpatch$irisClosed()
                ? "textures/gui/atlantis_shield_button_on_large.png"
                : "textures/gui/atlantis_shield_button_off_large.png",
                button.getX() - 10, button.getY() - 10, 74);
        return true;
    }

    private static void blit(GuiGraphics graphics, String path, int x, int y, int size) {
        graphics.blit(ResourceLocation.fromNamespaceAndPath("sgjpatch", path), x, y, 0F, 0F, size, size, size, size);
    }
}
