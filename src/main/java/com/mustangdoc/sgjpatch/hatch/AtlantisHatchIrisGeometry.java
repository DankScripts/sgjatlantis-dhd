package com.mustangdoc.sgjpatch.hatch;

/** Shared blade geometry for the rendered iris and its conservative collision aperture. */
public final class AtlantisHatchIrisGeometry {
    public static final float OPEN_DEGREES = 84;
    public static final float SCALE = 2.54f;
    public static final double APERTURE_RADIUS = 6.25;
    private AtlantisHatchIrisGeometry() {}
    public static double apertureRadius(double progress) {
        if (progress >= 1) return APERTURE_RADIUS;
        // Inscribed aperture of SGJourney's twenty pivoting triangular iris blades.
        double angle = Math.toRadians(-OPEN_DEGREES * progress);
        double c = Math.cos(angle), s = Math.sin(angle);
        double[][] vertices = {{-.792, 0}, {-.396, -2.5}, {0, 0}};
        for (double[] v : vertices) {
            double x = v[0], y = v[1];
            v[0] = .396 + x * c - y * s;
            v[1] = 2.5 + x * s + y * c;
        }
        double min = Double.MAX_VALUE;
        for (int i = 0; i < 3; i++) {
            double[] a = vertices[i], b = vertices[(i + 1) % 3];
            double x = b[0] - a[0], y = b[1] - a[1];
            double t = Math.max(0, Math.min(1, -(a[0] * x + a[1] * y) / (x*x + y*y)));
            min = Math.min(min, Math.hypot(a[0] + t*x, a[1] + t*y));
        }
        return Math.min(APERTURE_RADIUS, min * SCALE);
    }
}
