package com.mustangdoc.sgjpatch.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mustangdoc.sgjpatch.compat.LanteaJumperGlyphLights;
import com.mustangdoc.sgjpatch.compat.LanteaJumperGlyphModels;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws accepted native DHD symbols on the matching physical puddle-jumper keys. */
@Pseudo
@Mixin(targets = "io.github.aqueoxx.lantea.client.jumper.PuddleJumperRenderer", remap = false)
public abstract class LanteaJumperGlyphRendererMixin {
    private static final ThreadLocal<UUID> SGJPATCH_JUMPER = new ThreadLocal<>();
    private static Method sgjpatch$draw;

    @Inject(method = "render(Lio/github/aqueoxx/lantea/jumper/PuddleJumper;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), require = 1, remap = false)
    private void sgjpatch$captureJumper(@Coerce Object candidate, float yaw, float partial,
                                        PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (candidate instanceof Entity entity) SGJPATCH_JUMPER.set(entity.getUUID());
    }

    @Inject(method = "render(Lio/github/aqueoxx/lantea/jumper/PuddleJumper;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN"), require = 1, remap = false)
    private void sgjpatch$releaseJumper(@Coerce Object candidate, float yaw, float partial,
                                        PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
        SGJPATCH_JUMPER.remove();
    }

    @Inject(method = "lights", at = @At("RETURN"), require = 1, remap = false)
    private static void sgjpatch$drawSelectedKeys(PoseStack pose, float drives, boolean cabin,
                                                   @Coerce Object pass, CallbackInfo ci) {
        if (!cabin || pass == null) return;
        int[] lit = LanteaJumperGlyphLights.lit(SGJPATCH_JUMPER.get());
        if (lit.length == 0) return;
        try {
            if (sgjpatch$draw == null || sgjpatch$draw.getDeclaringClass() != pass.getClass()) {
                sgjpatch$draw = pass.getClass().getDeclaredMethod("draw", ResourceLocation.class, PoseStack.class);
                sgjpatch$draw.setAccessible(true);
            }
            for (int glyph : lit) {
                if (glyph >= 0 && glyph < LanteaJumperGlyphModels.GLYPHS.length)
                    sgjpatch$draw.invoke(pass, LanteaJumperGlyphModels.GLYPHS[glyph], pose);
            }
        } catch (ReflectiveOperationException ignored) {
            sgjpatch$draw = null;
        }
    }
}
