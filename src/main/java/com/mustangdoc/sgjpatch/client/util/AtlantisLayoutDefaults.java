package com.mustangdoc.sgjpatch.client.util;

/** Built-in F8 defaults for the accepted Atlantis dialer layout. */
public final class AtlantisLayoutDefaults {
    private AtlantisLayoutDefaults() {
    }

    public static int dx(int index) {
        return switch (index) {
            case 0 -> -17;
            case 1 -> 111;
            case 2 -> -30;
            case 3 -> -27;
            case 4 -> 5;
            case 5 -> -12;
            case 6 -> 9;
            case 7, 11, 39 -> 0;
            case 8 -> -68;
            case 9 -> 11;
            case 10 -> -31;
            case 12 -> 46;
            case 13 -> -77;
            case 14 -> -6;
            case 15 -> -25;
            case 16 -> -18;
            case 17 -> -38;
            case 18 -> 59;
            case 19 -> -17;
            case 20 -> -14;
            case 21 -> 31;
            case 22 -> -62;
            case 23 -> -41;
            case 24 -> -39;
            case 25 -> -15;
            case 26 -> -5;
            case 27 -> -1;
            case 28 -> -29;
            case 29 -> -26;
            case 30 -> -40;
            case 31 -> -4;
            case 32 -> 3;
            case 33 -> -41;
            case 34 -> -88;
            case 35 -> -3;
            case 36 -> -27;
            case 37 -> -65;
            case 38 -> -2;
            // The user-adjusted minigate position from the accepted R11 layout.
            case 1000 -> -29;
            default -> 0;
        };
    }

    public static int dy(int index) {
        return switch (index) {
            case 0 -> -149;
            case 1 -> -106;
            case 2 -> -19;
            case 3 -> -16;
            case 4 -> 46;
            case 5 -> 1;
            case 6 -> 26;
            case 7 -> 8;
            case 8 -> -38;
            case 9 -> -41;
            case 10, 11 -> -20;
            case 12 -> 67;
            case 13 -> 26;
            case 14 -> 67;
            case 15 -> 26;
            case 16 -> 2;
            case 17 -> 45;
            case 18 -> -18;
            case 19, 20 -> 4;
            case 21, 22, 37 -> 5;
            case 23, 24 -> 48;
            case 25 -> 2;
            case 26 -> 27;
            case 27, 31 -> 23;
            case 28 -> 26;
            case 29 -> 24;
            case 30 -> 45;
            case 32, 38 -> 15;
            case 33, 34 -> 46;
            case 35 -> 67;
            case 36 -> 26;
            case 39 -> 14;
            // The user-adjusted minigate position from the accepted R11 layout.
            case 1000 -> -16;
            default -> 0;
        };
    }

    public static boolean flip(int index) {
        return switch (index) {
            case 6, 8, 11, 12, 13 -> true;
            default -> false;
        };
    }
}
