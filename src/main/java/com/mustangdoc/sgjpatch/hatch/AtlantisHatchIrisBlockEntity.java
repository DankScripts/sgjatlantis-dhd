package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** One authoritative motor per roof iris; persisted and synchronized to observers. */
public final class AtlantisHatchIrisBlockEntity extends BlockEntity {
    public static final int TRAVEL_TICKS = 120;
    private int progress;
    private int previousProgress;
    private boolean targetOpen;

    public AtlantisHatchIrisBlockEntity(BlockPos pos, BlockState state) {
        super(AtlantisHatchRegistry.IRIS_ENTITY, pos, state);
    }
    public float progress(float partial) {
        return (previousProgress + (progress - previousProgress) * partial) / TRAVEL_TICKS;
    }
    public float progress() { return (float) progress / TRAVEL_TICKS; }
    public boolean targetOpen() { return targetOpen; }
    public boolean moving() { return progress != (targetOpen ? TRAVEL_TICKS : 0); }
    public void initialize(boolean open) {
        progress = previousProgress = open ? TRAVEL_TICKS : 0;
        targetOpen = open;
        sync();
    }
    public void request(boolean open) { targetOpen = open; sync(); }
    public void tick() {
        previousProgress = progress;
        if (level == null || !moving()) return;
        if (level.isClientSide) {
            progress += targetOpen ? 1 : -1;
            progress = Math.max(0, Math.min(TRAVEL_TICKS, progress));
            return;
        }
        int door = getBlockState().getValue(AtlantisHatchPanelBlock.UPPER) ? 1 : 0;
        if (!AtlantisHatchService.areaLoaded(level, door)) return;
        // Reverse before advancing into an occupied doorway, including vehicle bodies.
        if (!targetOpen && AtlantisHatchService.occupied((ServerLevel) level, door)) {
            targetOpen = true;
            sync();
        }
        progress += targetOpen ? 1 : -1;
        progress = Math.max(0, Math.min(TRAVEL_TICKS, progress));
        setChanged();
        if (!moving()) AtlantisHatchService.setWeatherSeal((ServerLevel) level, door, !targetOpen);
        if (progress % 10 == 0 || !moving()) sync();
    }
    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("IrisProgress", progress);
        tag.putBoolean("TargetOpen", targetOpen);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int next = Math.max(0, Math.min(TRAVEL_TICKS, tag.getInt("IrisProgress")));
        previousProgress = level != null && level.isClientSide ? progress : next;
        progress = next;
        targetOpen = tag.getBoolean("TargetOpen");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
