package com.mustangdoc.sgjpatch.client.util;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mustangdoc.sgjpatch.client.sound.IrisSoundState;
import com.mustangdoc.sgjpatch.mixin.AbstractDHDEntityAccessor;
import com.mustangdoc.sgjpatch.mixin.client.StargateRendererModelAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.povstalec.sgjourney.client.models.block_entity.AbstractStargateModel;
import net.povstalec.sgjourney.client.models.block_entity.ClassicStargateModel;
import net.povstalec.sgjourney.client.models.block_entity.MilkyWayStargateModel;
import net.povstalec.sgjourney.client.models.block_entity.UniverseStargateModel;
import net.povstalec.sgjourney.client.render.SGJourneyRenderTypes;
import net.povstalec.sgjourney.client.resourcepack.stargate_variant.ClientStargateVariant;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.RotatingStargateEntity;
import net.povstalec.sgjourney.common.misc.CoordinateHelper;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;

public final class LinkedMiniGate {
    private static final long LINK_GRACE_MS = 250;
    private static final MultiBufferSource.BufferSource BUFFER = MultiBufferSource.immediate(new BufferBuilder(2048));
    private static final MultiBufferSource.BufferSource CHEVRON_LIGHTS = MultiBufferSource.immediate(new BufferBuilder(2048));
    private static final ResourceLocation SHIELD_ON = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_shield_button_on_large.png");
    private static final ResourceLocation SHIELD_OFF = ResourceLocation.fromNamespaceAndPath("sgjpatch", "textures/gui/atlantis_shield_button_off_large.png");

    private AbstractStargateEntity<?> lastGate;
    private long linkLostAt = -1;

    public void render(GuiGraphics graphics, AbstractButton button, AbstractDHDEntity<?> dhd) {
        if (button == null || dhd == null || dhd.getLevel() == null) return;
        var relative = ((AbstractDHDEntityAccessor) dhd).sgjpatch$getStargateRelativePos();
        BlockPos target = relative == null ? null
                : CoordinateHelper.Relative.getOffsetPos(dhd.getDirection(), dhd.getBlockPos(), relative);
        AbstractStargateEntity<?> gate = resolveGate(dhd, target);
        if (gate != null) {
            Minecraft minecraft = Minecraft.getInstance();
            var renderer = minecraft.getBlockEntityRenderDispatcher().getRenderer(gate);
            if (renderer instanceof StargateRendererModelAccessor accessor) {
                renderGate(graphics, button, gate, accessor.sgjpatch$getStargateModel(), minecraft.getFrameTime());
            }
        } else if (target == null && LanteaMiniGateOverlay.render(dhd, button, graphics)) {
            return;
        }
        renderShieldButton(graphics, button,
                gate instanceof IrisInfo.Interface irisGate && IrisSoundState.active(irisGate.irisInfo()));
    }

    private AbstractStargateEntity<?> resolveGate(AbstractDHDEntity<?> dhd, BlockPos target) {
        var level = dhd.getLevel();
        if (target != null) {
            linkLostAt = -1;
            lastGate = level.hasChunkAt(target) && level.getBlockEntity(target) instanceof AbstractStargateEntity<?> gate
                    && !gate.isRemoved() ? gate : null;
        } else if (lastGate != null) {
            // A DHD update can briefly omit its link. Keep reading the live entity, not a saved dial state.
            long now = Util.getMillis();
            if (linkLostAt < 0) linkLostAt = now;
            BlockPos pos = lastGate.getBlockPos();
            if (now - linkLostAt >= LINK_GRACE_MS || lastGate.isRemoved() || lastGate.getLevel() != level
                    || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != lastGate) {
                lastGate = null;
            }
        }
        return lastGate;
    }

    private static void renderGate(GuiGraphics graphics, AbstractButton button, AbstractStargateEntity<?> gate,
                                   AbstractStargateModel<?, ?> model, float partialTick) {
        PoseStack pose = graphics.pose();
        graphics.flush();
        pose.pushPose();
        try {
            // Render the front of the ring in GUI coordinates, without terrain covers or world effects.
            pose.translate(button.getX() + 27, button.getY() + 27, 100);
            pose.scale(16, -16, 16);
            if (gate instanceof RotatingStargateEntity<?> rotating) {
                float rotation = rotating.getRotationDegrees(partialTick);
                if (model instanceof MilkyWayStargateModel milkyWay) milkyWay.setRotation(rotation);
                else if (model instanceof ClassicStargateModel classic) classic.setRotation(rotation);
                else if (model instanceof UniverseStargateModel universe) universe.setRotation(rotation);
            }
            renderRing(model, gate, partialTick, pose);
        } finally {
            pose.popPose();
        }
    }

    private static void renderShieldButton(GuiGraphics graphics, AbstractButton button, boolean shieldActive) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(0, 0, 150);
            graphics.blit(shieldActive ? SHIELD_ON : SHIELD_OFF,
                    button.getX() - 10, button.getY() - 10, 0F, 0F, 74, 74, 74, 74);
        } finally {
            pose.popPose();
        }
    }

    private static <G extends AbstractStargateEntity<?>, V extends ClientStargateVariant<G>> void renderRing(
            AbstractStargateModel<G, V> model, AbstractStargateEntity<?> gate, float partialTick, PoseStack pose) {
        // The dispatcher selected the renderer (and therefore the model) for this entity's registered type.
        @SuppressWarnings("unchecked")
        G modelGate = (G) gate;
        V variant = model.getClientVariant(modelGate);
        RenderType engaged = SGJourneyRenderTypes.engagedChevron(variant.getOverlayTexture(gate.isConnected()));
        // SGJourney draws lights first with Oculus installed; the GUI's solid chevrons cover that pass.
        MultiBufferSource preview = layer -> (layer == engaged ? CHEVRON_LIGHTS : BUFFER).getBuffer(layer);
        model.renderStargate(modelGate, variant, partialTick,
                pose, preview, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        BUFFER.endBatch();
        CHEVRON_LIGHTS.endBatch();
    }
}
