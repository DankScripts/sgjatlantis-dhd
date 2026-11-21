package com.mustangdoc.sgjpatch.client.screens;

import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.povstalec.sgjourney.client.screens.SGJourneyContainerScreen;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.misc.ComponentHelper;

public final class AtlantisDHDCrystalScreen extends SGJourneyContainerScreen<AtlantisDHDCrystalMenu> {
   private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("sgjourney", "textures/gui/dhd/pegasus/pegasus_dhd_crystal_gui.png");

   public AtlantisDHDCrystalScreen(AtlantisDHDCrystalMenu menu, Inventory inventory, Component title) {
      super(menu, inventory, title);
      this.imageWidth = 176;
      this.imageHeight = 166;
   }

   protected boolean hasItem(int slotIndex) {
      return this.menu != null && ((AtlantisDHDCrystalMenu)this.menu).hasItem(slotIndex);
   }

   public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
      this.renderBackground(guiGraphics);
      super.render(guiGraphics, mouseX, mouseY, partialTick);
      this.renderTooltip(guiGraphics, mouseX, mouseY);
      this.energyTooltip(
         guiGraphics,
         mouseX,
         mouseY,
         162,
         17,
         6,
         52,
         "tooltip.sgjourney.energy_buffer",
         ((AtlantisDHDCrystalMenu)this.menu).getEnergy(),
         ((AtlantisDHDCrystalMenu)this.menu).getMaxEnergy()
      );
      this.crystalEffectTooltip(
         guiGraphics,
         14,
         22,
         mouseX,
         mouseY,
         Component.translatable("tooltip.sgjourney.dhd.advanced_protocols")
            .append(Component.literal(": "))
            .append(Component.literal(String.valueOf(((AtlantisDHDCrystalMenu)this.menu).enableAdvancedProtocols())))
            .withStyle(ChatFormatting.AQUA),
         ComponentHelper.description("tooltip.sgjourney.dhd.advanced_protocols.description"),
         ComponentHelper.usage("tooltip.sgjourney.dhd.advanced_protocols.usage"),
         ComponentHelper.tickTimer(
            "info.sgjourney.open_time",
            ((AtlantisDHDCrystalMenu)this.menu).getStargateOpenTime(),
            Math.max(1, ((AtlantisDHDCrystalMenu)this.menu).getStargateOpenTime()),
            ChatFormatting.DARK_AQUA
         ),
         ComponentHelper.tickTimer(
            "info.sgjourney.last_traveler_time", ((AtlantisDHDCrystalMenu)this.menu).getStargateTimeSinceLastTraveler(), 200, ChatFormatting.DARK_PURPLE
         )
      );
      this.crystalEffectTooltip(
         guiGraphics,
         14,
         34,
         mouseX,
         mouseY,
         Component.translatable("tooltip.sgjourney.energy_target")
            .append(Component.literal(": "))
            .append(Component.literal(SGJourneyEnergy.energyToString(((AtlantisDHDCrystalMenu)this.menu).getEnergyTarget())))
            .withStyle(ChatFormatting.DARK_RED),
         ComponentHelper.description("tooltip.sgjourney.dhd.energy_target.description"),
         ComponentHelper.usage("tooltip.sgjourney.dhd.energy_target.usage"),
         ComponentHelper.energy("tooltip.sgjourney.dhd.stargate_energy", ((AtlantisDHDCrystalMenu)this.menu).getStargateEnergy())
      );
      this.crystalEffectTooltip(
         guiGraphics,
         14,
         46,
         mouseX,
         mouseY,
         Component.translatable("tooltip.sgjourney.dhd.energy_transfer")
            .append(Component.literal(": "))
            .append(Component.literal(SGJourneyEnergy.energyToString(((AtlantisDHDCrystalMenu)this.menu).maxEnergyDeplete())))
            .withStyle(ChatFormatting.GOLD),
         ComponentHelper.description("tooltip.sgjourney.dhd.energy_transfer.description"),
         ComponentHelper.usage("tooltip.sgjourney.dhd.energy_transfer.usage")
      );
      MutableComponent range = Component.translatable("tooltip.sgjourney.dhd.communication_range_1")
         .append(Component.literal(": "))
         .append(Component.literal(String.valueOf(((AtlantisDHDCrystalMenu)this.menu).getMaxDistance())))
         .append(Component.literal(" "))
         .append(Component.translatable("tooltip.sgjourney.dhd.communication_range_2"))
         .withStyle(ChatFormatting.GRAY);
      this.crystalEffectTooltip(
         guiGraphics,
         14,
         58,
         mouseX,
         mouseY,
         range,
         ComponentHelper.description("tooltip.sgjourney.dhd.communication_range.description"),
         ComponentHelper.usage("tooltip.sgjourney.dhd.communication_range.usage")
      );
      this.itemTooltip(
         guiGraphics, mouseX, mouseY, 80, 35, 0, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.large_crystal_slot.description")}
      );
      this.itemTooltip(guiGraphics, mouseX, mouseY, 80, 17, 1, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 98, 17, 2, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 98, 35, 3, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 98, 53, 4, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 80, 53, 5, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 62, 53, 6, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 62, 35, 7, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 62, 17, 8, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.crystal_slot.description")});
      this.itemTooltip(guiGraphics, mouseX, mouseY, 134, 27, 9, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.energy_slot.description")});
      this.itemTooltip(
         guiGraphics, mouseX, mouseY, 134, 53, 10, new Component[]{ComponentHelper.description("tooltip.sgjourney.dhd.energy_fuel_slot.description")}
      );
   }

   protected void crystalEffectTooltip(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY, Component... tooltipLines) {
      this.tooltip(guiGraphics, mouseX, mouseY, x, y, 16, 6, tooltipLines);
   }

   protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
      int left = (this.width - this.imageWidth) / 2;
      int top = (this.height - this.imageHeight) / 2;
      guiGraphics.blit(TEXTURE, left, top, 0, 0, this.imageWidth, this.imageHeight + 1);
      this.renderEnergyVertical(
         guiGraphics,
         TEXTURE,
         left + 162,
         top + 17,
         6,
         52,
         176,
         0,
         ((AtlantisDHDCrystalMenu)this.menu).getEnergy(),
         ((AtlantisDHDCrystalMenu)this.menu).getMaxEnergy()
      );
      this.itemHint(guiGraphics, TEXTURE, left + 80, top + 35, 0, 168, 0);
      this.itemHint(guiGraphics, TEXTURE, left + 80, top + 17, 16, 168, 1);
      this.itemHint(guiGraphics, TEXTURE, left + 98, top + 17, 16, 168, 2);
      this.itemHint(guiGraphics, TEXTURE, left + 98, top + 35, 16, 168, 3);
      this.itemHint(guiGraphics, TEXTURE, left + 98, top + 53, 16, 168, 4);
      this.itemHint(guiGraphics, TEXTURE, left + 80, top + 53, 16, 168, 5);
      this.itemHint(guiGraphics, TEXTURE, left + 62, top + 53, 16, 168, 6);
      this.itemHint(guiGraphics, TEXTURE, left + 62, top + 35, 16, 168, 7);
      this.itemHint(guiGraphics, TEXTURE, left + 62, top + 17, 16, 168, 8);
      this.itemHint(guiGraphics, TEXTURE, left + 134, top + 27, 32, 168, 9);
   }
}
