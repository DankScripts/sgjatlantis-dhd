package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.LanteaIrisBridge;
import com.mustangdoc.sgjpatch.network.ServerboundToggleIrisPacket;
import com.mustangdoc.sgjpatch.util.DhdLookup;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.MilkyWayStargateEntity;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Use native Milky Way iris motion and retain the nearby Lantea iris fallback. */
@Mixin(value = ServerboundToggleIrisPacket.class, remap = false)
public abstract class ServerboundToggleIrisLanteaMixin {
    @Shadow @Final private BlockPos dhdPos;

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void sgjpatch$toggleIris(ServerboundToggleIrisPacket packet,
                                               Supplier<NetworkEvent.Context> contextSupplier, CallbackInfo ci) {
        NetworkEvent.Context context = contextSupplier.get();
        ServerPlayer player = context.getSender();
        if (player == null) return;
        BlockPos pos = ((ServerboundToggleIrisLanteaMixin) (Object) packet).dhdPos;
        if (!(player.level() instanceof ServerLevel level)
                || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 4096
                || !(level.getBlockEntity(pos) instanceof AbstractDHDEntity dhd)) return;
        var gate = DhdLookup.resolveStargate(dhd);
        if (gate instanceof MilkyWayStargateEntity milkyWay) {
            IrisInfo iris = milkyWay.irisInfo();
            if (iris.hasIris()) {
                var motion = iris.getIrisMotion();
                boolean opening = motion.isMoving ? motion.isClosing() : iris.isIrisClosed();
                if (opening ? iris.aboveMinProgress() : iris.belowMaxProgress()) {
                    iris.setIrisMotion(opening ? IrisInfo.IrisMotion.OPENING_COMPUTER
                            : IrisInfo.IrisMotion.CLOSING_COMPUTER);
                    milkyWay.setChanged();
                    level.playSound(null, milkyWay.getBlockPos(),
                            opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                            SoundSource.BLOCKS, 1.0F, 1.0F);
                } else if (motion.isMoving) {
                    iris.setIrisMotion(IrisInfo.IrisMotion.IDLE);
                    milkyWay.updateClient();
                }
            }
        } else if (gate == null) {
            context.enqueueWork(() -> LanteaIrisBridge.toggleNearest(level, pos));
        } else {
            return;
        }
        context.setPacketHandled(true);
        ci.cancel();
    }
}
