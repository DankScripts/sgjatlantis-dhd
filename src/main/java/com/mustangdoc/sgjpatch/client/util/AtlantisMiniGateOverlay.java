package com.mustangdoc.sgjpatch.client.util;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mustangdoc.sgjpatch.mixin.client.StargateRendererModelAccessor;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDApi;
import com.mustangdoc.sgjpatch.client.sound.IrisSoundState;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.povstalec.sgjourney.client.models.block_entity.AbstractStargateModel;
import net.povstalec.sgjourney.client.models.block_entity.ClassicStargateModel;
import net.povstalec.sgjourney.client.models.block_entity.MilkyWayStargateModel;
import net.povstalec.sgjourney.client.models.block_entity.UniverseStargateModel;
import net.povstalec.sgjourney.client.resourcepack.stargate_variant.ClientStargateVariant;
import net.povstalec.sgjourney.client.render.SGJourneyRenderTypes;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.RotatingStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.Address;

import java.util.Locale;

/**
 * Draws the linked gate's live model and variant around the shield control.
 */
public final class AtlantisMiniGateOverlay {
    private static final MultiBufferSource.BufferSource PREVIEW =
            MultiBufferSource.immediate(new ByteBufferBuilder(2048));
    private static final MultiBufferSource.BufferSource CHEVRON_LIGHTS =
            MultiBufferSource.immediate(new ByteBufferBuilder(2048));
    private static final int GATE_SIZE = 118;
    private static final int GATE_OFFSET = 32;
    private static final int ORB_SIZE = 74;
    private static final int ORB_OFFSET = 10;
    private static final ResourceLocation GATE_BASE = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/mini_gate/early_port/gate_base.png");
    private static final ResourceLocation IDLE_SYMBOLS = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/mini_gate/early_port/idle_symbols.png");
    private static final ResourceLocation SHIELD_ORB = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_shield_button_off_large.png");
    private static final ResourceLocation SHIELD_ORB_ON = ResourceLocation.fromNamespaceAndPath(
            "sgjpatch", "textures/gui/atlantis_shield_button_on_large.png");

    private AbstractStargateEntity<?> lastGate;
    private long linkLostAt = -1;

    public void render(
            AtlantisDHDMenu menu, AbstractButton button, GuiGraphics graphics, float partialTick) {
        if (button == null)
            return;

        int x = button.getX();
        int y = button.getY();
        var dhd = menu.getDHD();
        var level = dhd.getLevel();
        var target = dhd.linkedGatePosition();
        var liveGate = resolveGate(level, target);
        if (liveGate != null
                && Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(liveGate)
                        instanceof StargateRendererModelAccessor renderer) {
            renderRing(graphics, renderer.sgjpatch$getStargateModel(), liveGate, x + 27, y + 27, partialTick);
            graphics.pose().pushPose();
            try {
                graphics.pose().translate(0, 0, 150);
                boolean shieldActive = liveGate instanceof IrisInfo.Interface iris && IrisSoundState.active(iris.irisInfo());
                blit(graphics, shieldActive ? SHIELD_ORB_ON : SHIELD_ORB, x - ORB_OFFSET, y - ORB_OFFSET, ORB_SIZE);
            } finally {
                graphics.pose().popPose();
            }
            return;
        }
        int gateX = x - GATE_OFFSET;
        int gateY = y - GATE_OFFSET;
        boolean hasStargate = AtlantisDHDApi.connectedStargate(menu.getDHD()) != null;
        if (!hasStargate
                && LanteaMiniGateOverlay.render(menu.getDHD(), button, graphics)) return;
        if (!hasStargate && menu.getDHD().getLevel() != null) {
            var mobsGate = com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.nearest(
                    menu.getDHD().getLevel(), menu.getDHD().getBlockPos());
            if (mobsGate instanceof net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.Interface irisGate) {
                blit(graphics, GATE_BASE, gateX, gateY, GATE_SIZE);
                blit(graphics, IDLE_SYMBOLS, gateX, gateY, GATE_SIZE);
                int mask = com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.litMask(mobsGate);
                for (int slot = 0; slot < 9; slot++)
                    if ((mask & (1 << slot)) != 0)
                        blit(graphics, chevronTexture(slot), gateX, gateY, GATE_SIZE);
                blit(graphics, IrisSoundState.active(irisGate.irisInfo()) ? SHIELD_ORB_ON : SHIELD_ORB,
                        x - ORB_OFFSET, y - ORB_OFFSET, ORB_SIZE);
                return;
            }
        }
        if (!(AtlantisDHDApi.connectedStargateEntity(menu.getDHD()) instanceof PegasusStargateEntity gate)) {
            blit(graphics, SHIELD_ORB, x - ORB_OFFSET, y - ORB_OFFSET, ORB_SIZE);
            return;
        }

        blit(graphics, GATE_BASE, gateX, gateY, GATE_SIZE);
        Address encodedSymbols = gate.getEncodedSymbols();
        int lockedSymbols = Math.min(gate.getAddress().getLength(), 9);
        int liveSymbols = Math.max(lockedSymbols, gate.getAddressBuffer().getLength());
        if (liveSymbols == 0 && !gate.isSymbolSpinning() && !gate.isConnected())
            blit(graphics, IDLE_SYMBOLS, gateX, gateY, GATE_SIZE);

        for (int index = 0; index < lockedSymbols; ++index) {
            int symbol = encodedSymbols.symbolAt(index);
            int position = gate.getChevronPosition(index + 1);
            if (validSymbol(symbol) && position >= 0)
                blit(graphics, symbolTexture(symbol, mirrorPosition(position)),
                        gateX, gateY, GATE_SIZE);
        }

        if (gate.isSymbolSpinning()) {
            int index = gate.getSymbolBuffer();
            int symbol = symbolAt(encodedSymbols, index);
            if (!validSymbol(symbol))
                symbol = symbolAt(gate.getAddressBuffer(), index);
            int position = Math.floorMod(gate.getCurrentSymbol(), 36);
            if (validSymbol(symbol) && !isLockedPosition(gate, lockedSymbols, position))
                blit(graphics, symbolTexture(symbol, mirrorPosition(position)),
                        gateX, gateY, GATE_SIZE);
        }

        for (int index = 0; index < lockedSymbols; ++index) {
            int position = gate.getChevronPosition(index + 1);
            if (position < 0)
                continue;

            int slot = index == lockedSymbols - 1 && lockedSymbols >= 7
                    ? 0 : Math.floorMod(Math.floorDiv(mirrorPosition(position), 4), 9);
            blit(graphics, chevronTexture(slot), gateX, gateY, GATE_SIZE);
        }

        blit(graphics, IrisSoundState.active(gate.irisInfo()) ? SHIELD_ORB_ON : SHIELD_ORB,
                x - ORB_OFFSET, y - ORB_OFFSET, ORB_SIZE);
    }

    private AbstractStargateEntity<?> resolveGate(net.minecraft.world.level.Level level, BlockPos target) {
        if (target != null || level == null) {
            linkLostAt = -1;
            lastGate = level != null && level.hasChunkAt(target)
                    && level.getBlockEntity(target) instanceof AbstractStargateEntity<?> gate && !gate.isRemoved()
                    ? gate : null;
        } else if (lastGate != null) {
            long now = Util.getMillis();
            if (linkLostAt < 0) linkLostAt = now;
            BlockPos pos = lastGate.getBlockPos();
            if (now - linkLostAt >= 250 || lastGate.isRemoved() || lastGate.getLevel() != level
                    || !level.hasChunkAt(pos) || level.getBlockEntity(pos) != lastGate) lastGate = null;
        }
        return lastGate;
    }

    private static boolean isLockedPosition(
            PegasusStargateEntity gate, int lockedSymbols, int position) {
        for (int index = 0; index < lockedSymbols; ++index) {
            int lockedPosition = gate.getChevronPosition(index + 1);
            if (lockedPosition >= 0 && Math.floorMod(lockedPosition, 36) == position)
                return true;
        }
        return false;
    }

    private static boolean validSymbol(int symbol) {
        return symbol >= 0 && symbol <= 38;
    }

    private static int symbolAt(Address address, int index) {
        return index >= 0 && index < address.getLength()
                ? address.symbolAt(index)
                : -1;
    }

    private static int mirrorPosition(int position) {
        return Math.floorMod(-position, 36);
    }

    private static ResourceLocation symbolTexture(int symbol, int position) {
        return ResourceLocation.fromNamespaceAndPath("sgjpatch", String.format(Locale.ROOT,
                "textures/gui/mini_gate/early_port/s%02d_p%02d.png",
                symbol, position));
    }

    private static ResourceLocation chevronTexture(int slot) {
        return ResourceLocation.fromNamespaceAndPath("sgjpatch", String.format(Locale.ROOT,
                "textures/gui/mini_gate/early_port/chevron_slot_%d.png", slot));
    }

    private static void blit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int size) {
        graphics.blit(texture, x, y, 0F, 0F, size, size, size, size);
    }

    private static <G extends AbstractStargateEntity<?>, V extends ClientStargateVariant<G>> void renderRing(
            GuiGraphics graphics, AbstractStargateModel<G, V> model, AbstractStargateEntity<?> gate,
            int x, int y, float partialTick) {
        // The block entity dispatcher supplies the model registered for this gate type.
        @SuppressWarnings("unchecked") G typedGate = (G) gate;
        if (gate instanceof RotatingStargateEntity<?> rotating) {
            float rotation = rotating.getRotationDegrees(partialTick);
            if (model instanceof MilkyWayStargateModel milkyWay) milkyWay.setRotation(rotation);
            else if (model instanceof ClassicStargateModel classic) classic.setRotation(rotation);
            else if (model instanceof UniverseStargateModel universe) universe.setRotation(rotation);
        }
        graphics.flush();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x, y, 100);
            pose.scale(16, -16, 16);
            V variant = model.getClientVariant(typedGate);
            RenderType engaged = SGJourneyRenderTypes.engagedChevron(variant.getOverlayTexture(gate.isConnected()));
            MultiBufferSource preview = layer -> (layer == engaged ? CHEVRON_LIGHTS : PREVIEW).getBuffer(layer);
            model.renderStargate(typedGate, variant, partialTick,
                    pose, preview, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            PREVIEW.endBatch();
            CHEVRON_LIGHTS.endBatch();
        } finally {
            pose.popPose();
        }
    }
}
