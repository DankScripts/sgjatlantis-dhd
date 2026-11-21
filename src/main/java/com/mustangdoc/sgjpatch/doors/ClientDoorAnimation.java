package com.mustangdoc.sgjpatch.doors;

import net.minecraft.util.Mth;

/** Tracks transitions when updates arrive, even while the door is not being rendered. */
public final class ClientDoorAnimation {
    private final long durationMillis;
    private boolean initialized;
    private boolean target;
    private float from;
    private long started;

    public ClientDoorAnimation(long durationMillis) {
        if (durationMillis <= 0) throw new IllegalArgumentException("Door animation duration must be positive");
        this.durationMillis = durationMillis;
    }

    public void setTarget(boolean open, long now) {
        if (!initialized) {
            initialized = true;
            target = open;
            from = open ? 1 : 0;
            started = now;
        } else if (target != open) {
            from = progress(now);
            target = open;
            started = now;
        }
    }

    public float progress(boolean open, long now) {
        setTarget(open, now);
        return progress(now);
    }

    private float progress(long now) {
        float elapsed = Mth.clamp((float)(now - started) / durationMillis, 0, 1);
        return Mth.lerp(elapsed, from, target ? 1 : 0);
    }
}
