package com.mustangdoc.sgjpatch.mixin;

import com.mustangdoc.sgjpatch.compat.LanteaEnergyIrisState;
import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "io.github.aqueoxx.lantea.blockentity.StargateControllerBlockEntity", remap = false)
public abstract class LanteaGateIrisStateMixin implements LanteaEnergyIrisState {
    @Unique private boolean sgjpatch$irisInstalled;
    @Unique private boolean sgjpatch$irisClosed;

    @Override public boolean sgjpatch$irisInstalled() { return sgjpatch$irisInstalled; }
    @Override public boolean sgjpatch$irisClosed() { return sgjpatch$irisInstalled && sgjpatch$irisClosed; }

    @Override public void sgjpatch$setIris(boolean installed, boolean closed) {
        boolean wasClosed = sgjpatch$irisClosed();
        sgjpatch$irisInstalled = installed;
        sgjpatch$irisClosed = installed && closed;
        BlockEntity gate = (BlockEntity) (Object) this;
        gate.setChanged();
        if (gate.getLevel() != null && !gate.getLevel().isClientSide) {
            gate.getLevel().sendBlockUpdated(gate.getBlockPos(), gate.getBlockState(), gate.getBlockState(), 3);
            if (wasClosed != sgjpatch$irisClosed())
                gate.getLevel().playSound(null, gate.getBlockPos(),
                        (sgjpatch$irisClosed() ? SGJPatchSounds.PEGASUS_SHIELD_CLOSING : SGJPatchSounds.PEGASUS_SHIELD_OPENING).get(),
                        SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"), require = 1, remap = true)
    private void sgjpatch$saveIris(CompoundTag tag, CallbackInfo ci) {
        tag.putBoolean("SgjpatchEnergyIrisInstalled", sgjpatch$irisInstalled);
        tag.putBoolean("SgjpatchEnergyIrisClosed", sgjpatch$irisClosed);
    }

    @Inject(method = "load", at = @At("TAIL"), require = 1, remap = true)
    private void sgjpatch$loadIris(CompoundTag tag, CallbackInfo ci) {
        sgjpatch$readIris(tag);
    }

    @Inject(method = "getUpdateTag", at = @At("RETURN"), require = 1, remap = true)
    private void sgjpatch$updateTag(CallbackInfoReturnable<CompoundTag> cir) {
        CompoundTag tag = cir.getReturnValue();
        tag.putBoolean("SgjpatchEnergyIrisInstalled", sgjpatch$irisInstalled);
        tag.putBoolean("SgjpatchEnergyIrisClosed", sgjpatch$irisClosed);
    }

    @Inject(method = "handleUpdateTag", at = @At("TAIL"), require = 1, remap = false)
    private void sgjpatch$handleUpdate(CompoundTag tag, CallbackInfo ci) {
        sgjpatch$readIris(tag);
    }

    @Unique private void sgjpatch$readIris(CompoundTag tag) {
        sgjpatch$irisInstalled = tag.getBoolean("SgjpatchEnergyIrisInstalled");
        sgjpatch$irisClosed = sgjpatch$irisInstalled && tag.getBoolean("SgjpatchEnergyIrisClosed");
    }
}
