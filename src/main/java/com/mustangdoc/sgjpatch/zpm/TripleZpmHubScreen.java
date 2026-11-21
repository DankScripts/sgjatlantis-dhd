package com.mustangdoc.sgjpatch.zpm;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/** Ported from SG Mobs with permission from Mike; retains SGJourney ZPM behavior. */
public final class TripleZpmHubScreen extends AbstractContainerScreen<TripleZpmHubMenu> {
   public TripleZpmHubScreen(TripleZpmHubMenu menu, Inventory inv, Component title) {
      super(menu, inv, title);
      this.imageWidth = 176;
      this.imageHeight = 166;
      this.inventoryLabelY = 72;
   }

   protected void renderBg(GuiGraphics g, float partial, int mx, int my) {
      int x = this.leftPos;
      int y = this.topPos;
      g.fill(x, y, x + 176, y + 166, -15263977);
      g.fill(x + 1, y + 1, x + 175, y + 165, -1);
      g.fill(x + 3, y + 3, x + 175, y + 165, -11184811);
      g.fill(x + 3, y + 3, x + 173, y + 163, -3750202);

      for (Slot slot : ((TripleZpmHubMenu)this.menu).slots) {
         int sx = x + slot.x;
         int sy = y + slot.y;
         g.fill(sx - 1, sy - 1, sx + 17, sy + 17, -1);
         g.fill(sx - 1, sy - 1, sx + 16, sy + 16, -13158601);
         g.fill(sx, sy, sx + 16, sy + 16, -7631989);
      }
   }

   public void render(GuiGraphics g, int mx, int my, float partial) {
      super.render(g, mx, my, partial);
      this.renderTooltip(g, mx, my);
   }
}
