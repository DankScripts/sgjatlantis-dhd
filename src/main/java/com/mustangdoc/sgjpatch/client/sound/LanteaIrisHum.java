package com.mustangdoc.sgjpatch.client.sound;

import com.mustangdoc.sgjpatch.compat.LanteaEnergyIrisState;
import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Continuous energy-iris hum while a closed Lantea gate is visible. */
public final class LanteaIrisHum {
    private static final Map<BlockPos, Hum> ACTIVE = new HashMap<>();

    private LanteaIrisHum() { }

    public static void sync(BlockEntity gate, boolean closed) {
        BlockPos pos = gate.getBlockPos().immutable();
        Hum current = ACTIVE.get(pos);
        if (!closed) {
            if (current != null) current.requestStop();
            ACTIVE.remove(pos);
            return;
        }
        if (current == null || current.isStopped()) {
            Hum sound = new Hum(gate);
            ACTIVE.put(pos, sound);
            Minecraft.getInstance().getSoundManager().play(sound);
        }
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        private final BlockEntity gate;
        private final Level level;

        private Hum(BlockEntity gate) {
            super(SGJPatchSounds.HUMMING.get(), SoundSource.BLOCKS, RandomSource.create());
            this.gate = gate;
            this.level = gate.getLevel();
            this.x = gate.getBlockPos().getX() + 0.5;
            this.y = gate.getBlockPos().getY() + 3.5;
            this.z = gate.getBlockPos().getZ() + 0.5;
            this.volume = 4.0F;
            this.looping = true;
            this.delay = 0;
        }

        private void requestStop() { stop(); }

        @Override public void tick() {
            if (Minecraft.getInstance().level != level || level == null
                    || level.getBlockEntity(gate.getBlockPos()) != gate
                    || !(gate instanceof LanteaEnergyIrisState iris) || !iris.sgjpatch$irisClosed()) stop();
        }
    }
}
