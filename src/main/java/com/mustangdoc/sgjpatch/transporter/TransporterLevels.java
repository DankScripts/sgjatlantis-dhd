package com.mustangdoc.sgjpatch.transporter;

import java.util.Locale;

public final class TransporterLevels {
    private TransporterLevels() {}

    public static boolean isLower(int x, int y, int z, String stationName) {
        String name = stationName.toLowerCase(Locale.ROOT);
        return (x == -3897 && y == 256 && z == 5876)
            || (x == -3885 && y == 255 && z == 5885)
            || (x == -3937 && y == 263 && z == 5973)
            || name.equals("lower spire")
            || name.equals("genetics lab")
            || (name.contains("underwater") && name.contains("bay"));
    }
}
