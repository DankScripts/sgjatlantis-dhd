package com.mustangdoc.sgjpatch.mixin.client;

import com.mustangdoc.sgjpatch.client.util.AtlantisKeypadSurface;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets="com.mustangdoc.sgjpatch.client.screens.AtlantisDHDScreenFixed$AtlantisTriangleSymbolButton", remap=false)
public abstract class AtlantisKeypadSurfaceMixin {
    @Shadow private boolean up;
    @Shadow private int size;
    @org.spongepowered.asm.mixin.Unique
    private net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity sgjpatch$glyphDhd;

    @org.spongepowered.asm.mixin.Unique
    private net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity sgjpatch$glyphDhd() {
        if (sgjpatch$glyphDhd == null) {
            try {
                var field = getClass().getDeclaredField("menu");
                field.setAccessible(true);
                Object menu = field.get(this);
                sgjpatch$glyphDhd = (net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity)
                        menu.getClass().getMethod("getDHD").invoke(menu);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Cannot resolve Atlantis DHD glyph source", e);
            }
        }
        return sgjpatch$glyphDhd;
    }
    @Shadow private int actionSymbol;

    @Inject(method="sgjpatch$renderTriangleBackground", at=@At("HEAD"), cancellable=true, remap=false)
    private void cleanCrystalFace(GuiGraphics graphics, ResourceLocation texture, int frame, CallbackInfo callback) {
        AbstractButton button=(AbstractButton)(Object)this;
        AtlantisKeypadSurface.crystal(graphics, button.getX(), button.getY(), size, up, frame);
        callback.cancel();
    }

    // The legacy glyph quad was centered in a square and could cross a sloping edge.
    // Keep its existing texture and shader tint, but fit it around the triangle centroid.
    @Redirect(method="sgjpatch$renderSymbolTexture", at=@At(value="INVOKE",
            target="Lnet/minecraft/client/gui/GuiGraphics;m_280163_(Lnet/minecraft/resources/ResourceLocation;IIFFIIII)V",
            remap=false), remap=false)
    private void fitGlyph(GuiGraphics graphics, ResourceLocation texture, int x, int y,
                          float u, float v, int width, int height, int textureWidth, int textureHeight) {
        AbstractButton button=(AbstractButton)(Object)this;
        int glyphSize=(int)Math.floor(size*.50);
        int glyphX=button.getX()+(size-glyphSize)/2;
        int glyphY=button.getY()+(int)Math.round(size*(up?2d/3:1d/3)-glyphSize/2d);
        ResourceLocation gateTexture = com.mustangdoc.sgjpatch.client.render.AtlantisDHDButtonStateRenderer
                .symbolTexture(sgjpatch$glyphDhd(), actionSymbol);
        if (gateTexture != null) {
            // Restore the brighter green Atlantis glyph glow used by the table.
            RenderSystem.setShaderColor(0.42F, 1.0F, 0.76F, 1.0F);
            graphics.blit(gateTexture, glyphX, glyphY, glyphSize, glyphSize,
                    0, 0, 1, 1, 1, 1);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }
}
