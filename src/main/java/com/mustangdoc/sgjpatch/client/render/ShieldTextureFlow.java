package com.mustangdoc.sgjpatch.client.render;

/** Gentle texture flow; never changes shield geometry, colour, or opacity. */
final class ShieldTextureFlow {
    private static final long START = System.nanoTime();
    private static double phase;
    private ShieldTextureFlow() { }

    // Sample once per shield draw so shared vertices and both faces stay aligned.
    static void beginFrame() {
        phase = (System.nanoTime() - START) * 1.0e-9;
    }
    static float u(float u, float v) {
        return u + envelope(u, v) * (float) Math.sin(v * 18.0 + phase * 1.2);
    }
    static float v(float u, float v) {
        return v + envelope(u, v) * (float) Math.sin(u * 16.0 - phase * 0.9);
    }
    private static float envelope(float u, float v) {
        // Pin texture edges to avoid wrapping seams; maximum displacement <1%.
        return 0.007F * Math.max(0.0F, 16.0F * u * (1.0F - u) * v * (1.0F - v));
    }
}
