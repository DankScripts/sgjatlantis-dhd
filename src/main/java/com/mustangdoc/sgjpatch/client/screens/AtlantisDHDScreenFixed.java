package com.mustangdoc.sgjpatch.client.screens;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mustangdoc.sgjpatch.client.widgets.AtlantisTriangleEngageButton;
import com.mustangdoc.sgjpatch.client.widgets.AtlantisTriangleSymbolButton;
import com.mustangdoc.sgjpatch.client.util.AtlantisGuiOverlay;
import com.mustangdoc.sgjpatch.client.util.AtlantisMiniGateOverlay;
import com.mustangdoc.sgjpatch.client.util.AtlantisAddressGuide;
import com.mustangdoc.sgjpatch.client.util.AtlantisRecentAddresses;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.povstalec.sgjourney.client.screens.dhd.AbstractDHDScreen;
import net.povstalec.sgjourney.client.widgets.dhd.PegasusDHDBigButton;
import net.povstalec.sgjourney.common.config.ClientDHDConfig;


/**
 * NeoForge 1.21.1 port of the locked 720x360 Atlantis table GUI.
 * Gameplay behavior remains the existing 1.21.1 implementation; only the
 * accepted GUI presentation and button positions are applied here.
 */
public class AtlantisDHDScreenFixed extends AbstractDHDScreen<AtlantisDHDMenu> {
    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_clean.png"
    );
    private static final ResourceLocation OUTER_UP = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_outer_green_up.png"
    );
    private static final ResourceLocation OUTER_DOWN = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_outer_green_down.png"
    );
    private static final ResourceLocation CENTER_UP = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_up.png"
    );
    private static final ResourceLocation CENTER_DOWN = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_dhd_crystal_down.png"
    );
    private AbstractButton circleButton;
    private final AtlantisMiniGateOverlay miniGate = new AtlantisMiniGateOverlay();

    public AtlantisDHDScreenFixed(AtlantisDHDMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, BACKGROUND);
        this.imageWidth = 720;
        this.imageHeight = 360;
    }

    @Override
    public void init() {
        super.init();
        addAtlantisTableLayout();
        AtlantisAddressGuide.begin(menu.getDHD());
    }

    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(
                BACKGROUND,
                leftPos,
                topPos,
                0F,
                0F,
                imageWidth,
                imageHeight,
                imageWidth,
                imageHeight
        );
        graphics.blit(BACKGROUND, leftPos+83, topPos+198, 283, 198, 170, 69, imageWidth, imageHeight);
        com.mustangdoc.sgjpatch.client.util.AtlantisKeypadSurface.backplate(graphics,leftPos-46,topPos);
        AtlantisRecentAddresses.render(graphics,menu.getDHD(),leftPos,topPos,mouseX,mouseY);
    }

    private void addAtlantisTableLayout() {
        for (int symbol = -1; symbol <= 38; symbol++) {
            var shape = com.mustangdoc.sgjpatch.client.util.AtlantisReferencePlate.keyShape(symbol);
            int x = leftPos - 46 + (int)Math.round(shape.x()), y = topPos + (int)Math.round(shape.y());
            int w = (int)Math.round(shape.width()), h = (int)Math.round(shape.height());
            AbstractButton button;
            if (symbol == -1) button = new AtlantisTriangleEngageButton(x,y,w,w,false,CENTER_DOWN,() -> {
                if (minecraft != null && minecraft.gameMode != null)
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AtlantisDHDMenu.BUTTON_ENGAGE);
            });
            else button = new AtlantisTriangleSymbolButton(x,y,w,w,menu,symbol,shape.up(),
                    shape.up()?OUTER_UP:OUTER_DOWN,w*.50F);
            button.setHeight(h);
            addRenderableWidget(button);
        }

        // Locked R13 shield/minigate anchor.
        PegasusDHDBigButton<AtlantisDHDMenu> shieldButton = new PegasusDHDBigButton<>(
                leftPos + 535,
                topPos + 66,
                menu,
                button -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    if (minecraft.gameMode != null)
                        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, AtlantisDHDMenu.BUTTON_TOGGLE_SHIELD);
                }
        ) {};
        shieldButton.setTooltip(Tooltip.create(Component.literal("Toggle Shield")));
        circleButton = shieldButton;
        addRenderableWidget(shieldButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        renderBg(graphics, partialTick, mouseX, mouseY);
        RenderSystem.disableDepthTest();
        for (Renderable renderable : renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
        graphics.drawCenteredString(font,
                ClientDHDConfig.dhd_symbols_numbers.get() ? SYMBOLS_TO_NUMBERS : NUMBERS_TO_SYMBOLS,
                leftPos + imageWidth / 2, topPos + imageHeight + 1, 0xFFFFFF);
        RenderSystem.enableDepthTest();
        AtlantisGuiOverlay.renderControls(
            graphics,
            leftPos,
            topPos,
            menu.getDHD().getEnergyStorage().hasEnergy(1L));
        miniGate.render(menu, circleButton, graphics, partialTick);

    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        else super.onClose();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isPaste(keyCode)) return AtlantisAddressGuide.paste(menu.getDHD());
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && AtlantisRecentAddresses.click(menu.getDHD(),leftPos,topPos,mouseX,mouseY)) return true;
        return super.mouseClicked(mouseX,mouseY,button);
    }

}
