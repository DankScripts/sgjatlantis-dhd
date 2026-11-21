package com.mustangdoc.sgjpatch.compat;

import java.util.Map;
import java.util.WeakHashMap;

/** Client-side transition state for the Atlantis energy iris drawn over a Lantea gate. */
public final class LanteaEnergyIrisTransition {
    private static final long CLOSE_DURATION_NS = 1_776_000_000L;
    private static final long OPEN_DURATION_NS = 1_829_000_000L;
    private static final Map<Object, Transition> STATES = new WeakHashMap<>();

    private LanteaEnergyIrisTransition() {}

    public static float reveal(Object gate, boolean closed) {
        long now = System.nanoTime();
        synchronized (STATES) {
            Transition transition = STATES.get(gate);
            if (transition == null) {
                float initial = closed ? 1F : 0F;
                transition = new Transition(closed, initial, now);
                STATES.put(gate, transition);
            } else {
                transition.advance(closed, now);
            }
            return transition.reveal(now);
        }
    }

    private static float clamp(float value) {
        return Math.max(0F, Math.min(value, 1F));
    }

    private static final class Transition {
        private boolean closed;
        private float start;
        private long startedAt;

        private Transition(boolean closed, float start, long startedAt) {
            this.closed = closed;
            this.start = start;
            this.startedAt = startedAt;
        }

        private void advance(boolean targetClosed, long now) {
            float current = reveal(now);
            if (closed == targetClosed) return;
            start = targetClosed ? Math.max(current, 0.08F) : current;
            closed = targetClosed;
            startedAt = now;
        }

        private float reveal(long now) {
            float target = closed ? 1F : 0F;
            float distance = Math.abs(target - start);
            long duration = Math.max(120_000_000L,
                    (long) ((closed ? CLOSE_DURATION_NS : OPEN_DURATION_NS) * distance));
            float progress = distance <= 0.0001F ? 1F : clamp((float) (now - startedAt) / duration);
            float eased = progress * progress * progress * (progress * (progress * 6F - 15F) + 10F);
            return start + (target - start) * eased;
        }
    }
}
