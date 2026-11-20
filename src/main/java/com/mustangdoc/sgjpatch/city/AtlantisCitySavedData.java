package com.mustangdoc.sgjpatch.city;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

/** Remembers which city chunks have already been imported or found in the save. */
public final class AtlantisCitySavedData extends SavedData {
    private static final String KEY = "sgjatlantis_dhd_city_205";
    private final Set<Long> complete = new HashSet<>();
    private boolean hatchesInstalled;

    public static AtlantisCitySavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(AtlantisCitySavedData::load,
                AtlantisCitySavedData::new, KEY);
    }

    private static AtlantisCitySavedData load(CompoundTag tag) {
        AtlantisCitySavedData data = new AtlantisCitySavedData();
        for (long value : tag.getLongArray("CompleteChunks")) data.complete.add(value);
        data.hatchesInstalled = tag.getBoolean("HatchesInstalled");
        return data;
    }

    public boolean complete(int cx, int cz) {
        return complete.contains(ChunkPos.asLong(cx, cz));
    }

    public void markComplete(int cx, int cz) {
        if (complete.add(ChunkPos.asLong(cx, cz))) setDirty();
    }

    public boolean hatchesInstalled() { return hatchesInstalled; }

    public void markHatchesInstalled() {
        hatchesInstalled = true;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("CompleteChunks", complete.stream().mapToLong(Long::longValue).toArray());
        tag.putBoolean("HatchesInstalled", hatchesInstalled);
        return tag;
    }
}
