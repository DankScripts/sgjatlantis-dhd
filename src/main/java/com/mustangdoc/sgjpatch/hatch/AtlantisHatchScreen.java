package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** One Atlantis-styled control screen with independent upper and lower hatches. */
public final class AtlantisHatchScreen extends AbstractContainerScreen<AtlantisHatchMenu> {
    private final Button[] buttons = new Button[2];

    public AtlantisHatchScreen(AtlantisHatchMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 258;
        imageHeight = 154;
        titleLabelX = 14;
        titleLabelY = 13;
        inventoryLabelY = 1000;
    }

    @Override
    protected void init() {
        super.init();
        for (int door = 0; door < 2; door++) {
            final int selected = door;
            buttons[door] = addRenderableWidget(Button.builder(label(door), button -> {
                if (minecraft == null || minecraft.player == null || minecraft.gameMode == null) return;
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, selected);
            }).bounds(leftPos + 153, topPos + 52 + door * 49, 87, 22).build());
        }
    }

    private Component label(int door) {
        return Component.literal(switch (menu.doorStatus(door)) {
            case 0 -> "OPEN";
            case 1 -> "CLOSE";
            case 2 -> "RESET / CLOSE";
            case 4 -> "OPENING...";
            case 5 -> "CLOSING...";
            default -> "UNAVAILABLE";
        });
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        for (int door = 0; door < 2; door++) {
            buttons[door].setMessage(label(door));
            buttons[door].active = menu.doorStatus(door) < 3;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xEF07141B);
        graphics.fill(leftPos + 1, topPos + 1, leftPos + imageWidth - 1, topPos + 35, 0xDD164955);
        graphics.renderOutline(leftPos, topPos, imageWidth, imageHeight, 0xFF67D4E1);
        for (int door = 0; door < 2; door++) {
            int y = topPos + 43 + door * 49;
            graphics.fill(leftPos + 12, y, leftPos + 246, y + 40, 0xFF142B32);
            graphics.renderOutline(leftPos + 12, y, 234, 40, 0xFF356C76);
            int color = menu.doorStatus(door) == 0 ? 0xFF79D7DF
                    : menu.doorStatus(door) == 1 ? 0xFFD7B973 : 0xFFE16F65;
            graphics.fill(leftPos + 23, y + 28, leftPos + 130, y + 31, color);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xE7FFFF, false);
        graphics.drawString(font, "POWER " + menu.storedEnergy() + "/"
                        + AtlantisHatchTableBlockEntity.MAX_ENERGY + " FE  (50 FE/action)",
                14, 29, 0x86D5E2, false);
        graphics.drawString(font, "GATE ROOM CEILING", 23, 51, 0xBDDCE0, false);
        graphics.drawString(font, "JUMPER ROOM CEILING", 23, 100, 0xBDDCE0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }
}
