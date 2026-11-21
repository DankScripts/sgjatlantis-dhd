package com.mustangdoc.sgjpatch.client.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.povstalec.sgjourney.client.resourcepack.symbols.ClientPointOfOrigin;
import net.povstalec.sgjourney.client.resourcepack.symbols.ClientSymbols;
import net.povstalec.sgjourney.client.screens.SGJourneyContainerScreen;
import net.povstalec.sgjourney.client.widgets.dhd.DHDSymbolButton;
import net.povstalec.sgjourney.common.config.ClientDHDConfig;
import net.povstalec.sgjourney.common.misc.ColorUtil;
import net.povstalec.sgjourney.common.packets.ServerboundDHDUpdatePacket;
import org.joml.Matrix4f;

public final class AtlantisTriangleSymbolButton extends DHDSymbolButton<AtlantisDHDMenu> {
    private static final ColorUtil.RGBA HOVER = new ColorUtil.RGBA(255, 255, 255);
    private static final ColorUtil.RGBA DISENGAGED = new ColorUtil.RGBA(65, 65, 65);
    private static final ColorUtil.RGBA ENGAGED = new ColorUtil.RGBA(0, 242, 255);

    private final AtlantisDHDMenu atlantisMenu;
    private final boolean up;
    private final int sourceSize;
    private final float symbolSize;

    public AtlantisTriangleSymbolButton(
            int x,
            int y,
            int displaySize,
            int sourceSize,
            AtlantisDHDMenu menu,
            int symbol,
            boolean up,
            ResourceLocation texture,
            float symbolSize) {
        super(x, y, displaySize, displaySize, menu, symbol, texture, texture, HOVER, DISENGAGED, ENGAGED,
            button -> PacketDistributor.sendToServer(new ServerboundDHDUpdatePacket(
                menu.getDHD().getBlockPos(), ((AtlantisTriangleSymbolButton) button).getSymbol())));
        this.atlantisMenu = menu;
        this.up = up;
        this.sourceSize = sourceSize;
        this.symbolSize = symbolSize;
        setTooltip(Tooltip.create(symbolComponent()));
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        return this.active && this.visible && insideTriangle(mouseX, mouseY);
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.active && this.visible && insideTriangle(mouseX, mouseY);
    }

    private boolean insideTriangle(double mouseX, double mouseY) {
        double nx = (mouseX - getX()) / (double) width;
        double ny = (mouseY - getY()) / (double) height;
        if (nx < 0.0 || nx > 1.0 || ny < 0.0 || ny > 1.0)
            return false;
        double edge = 2.0 * Math.abs(nx - 0.5);
        return up ? ny >= edge : ny <= 1.0 - edge;
    }

    @Override
    public void renderSymbol(GuiGraphics graphics) {
        ResourceLocation texture = com.mustangdoc.sgjpatch.client.render.AtlantisDHDButtonStateRenderer
                .symbolTexture(atlantisMenu.getDHD(), getSymbol());
        if (texture != null) {
            RenderSystem.setShaderColor(0.42F, 1.0F, 0.76F, 1F);
            int size = Math.round(symbolSize);
            graphics.blit(texture, Math.round(getX() + width / 2F - size / 2F),
                    Math.round(getY() + height * (up ? 2F/3 : 1F/3) - size / 2F),
                    size, size, 0, 0, 1, 1, 1, 1);
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        }
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        updateRemapping();
        this.isHovered = isMouseOver(mouseX, mouseY);

        boolean engaged = isEngaged();
        boolean guided = com.mustangdoc.sgjpatch.client.util.AtlantisAddressGuide
                .highlights(atlantisMenu.getDHD(), getSymbol());
        int frame = isHoveredOrFocused() || guided ? 1 : 0;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1F, 1F, 1F, this.alpha);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();

        graphics.pose().pushPose();
        graphics.pose().translate(getX(),getY(),0);
        graphics.pose().scale(1F,height/(float)width,1F);
        com.mustangdoc.sgjpatch.client.util.AtlantisKeypadSurface.crystal(graphics,0,0,width,up,frame);
        graphics.pose().popPose();

        if (engaged) {
            graphics.flush();
            renderGoldOverlay(graphics, false);
        } else if (guided) {
            graphics.flush();
            renderGoldOverlay(graphics, true);
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (ClientDHDConfig.dhd_symbols_numbers.get() == SGJourneyContainerScreen.isShiftDown())
            renderNumber(graphics, minecraft);
        else
            renderSymbol(graphics);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private void renderGoldOverlay(GuiGraphics graphics, boolean guide) {
        float inset = 1F;
        float x0 = getX() + inset;
        float y0 = getY() + inset;
        float x1 = getX() + width - inset;
        float y1 = getY() + height - inset;
        float centerX = (x0 + x1) * 0.5F;
        Matrix4f matrix = graphics.pose().last().pose();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR
        );
        if (up) {
            buffer.addVertex(matrix, centerX, y0, 0F).setColor(255, guide ? 235 : 170, guide ? 70 : 0, guide ? 185 : 140);
            buffer.addVertex(matrix, x0, y1, 0F).setColor(255, guide ? 235 : 170, guide ? 70 : 0, guide ? 185 : 140);
            buffer.addVertex(matrix, x1, y1, 0F).setColor(255, guide ? 235 : 170, guide ? 70 : 0, guide ? 185 : 140);
        } else {
            buffer.addVertex(matrix, x0, y0, 0F).setColor(255, guide ? 235 : 170, guide ? 70 : 0, guide ? 185 : 140);
            buffer.addVertex(matrix, x1, y0, 0F).setColor(255, guide ? 235 : 170, guide ? 70 : 0, guide ? 185 : 140);
            buffer.addVertex(matrix, centerX, y1, 0F).setColor(255, guide ? 235 : 170, guide ? 70 : 0, guide ? 185 : 140);
        }
        BufferUploader.drawWithShader(buffer.buildOrThrow());
        RenderSystem.enableCull();
    }
}
