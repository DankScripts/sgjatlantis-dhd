package com.mustangdoc.sgjpatch.client.util;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/** Draws the locked decorative table controls without changing menu behavior. */
public final class AtlantisGuiOverlay {
    private static final ResourceLocation CRYSTAL = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_control_crystal.png");
    private static final ResourceLocation BAR = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_control_bar.png");
    private static final int[][] CONTROLS = {
            {70, 70, 44, 34}, {123, 70, 44, 34}, {176, 70, 44, 34},
            {70, 117, 44, 34}, {123, 117, 44, 34}, {176, 117, 44, 34},
            {66, 252, 68, 26}, {148, 252, 68, 26}
    };

    private AtlantisGuiOverlay() {}

    public static void renderControls(
            GuiGraphics graphics, int left, int top, boolean powered) {
        graphics.flush();
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 100);
        RenderSystem.setShaderColor(powered ? 0.42F : 0.21F,
                powered ? 0.90F : 0.45F, powered ? 0.68F : 0.34F, 1F);
        for (int index = 0; index < CONTROLS.length; index++) {
            if (index >= 6 && !powered) continue;
            int[] control = CONTROLS[index];
            ResourceLocation texture = index < 6 ? CRYSTAL : BAR;
            if (index >= 6) {
                if (index == 6) {
                    graphics.flush();
                    RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                }
                graphics.pose().pushPose();
                graphics.pose().translate(left + control[0], top + control[1], 0);
                graphics.pose().scale(68F / 52F, 26F / 18F, 1F);
                graphics.blit(texture, 0, 0, 52, 18, 0F, 0F, 52, 18, 52, 18);
                graphics.pose().popPose();
            } else graphics.blit(texture, left + control[0], top + control[1],
                    control[2], control[3], 0F, 0F,
                    control[2], control[3], control[2], control[3]);
        }
        graphics.flush();
        graphics.pose().popPose();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }
}
