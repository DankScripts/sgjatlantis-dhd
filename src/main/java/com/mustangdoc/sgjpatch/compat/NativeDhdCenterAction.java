package com.mustangdoc.sgjpatch.compat;

final class NativeDhdCenterAction {
    private NativeDhdCenterAction() {}

    static int forStatus(String status, boolean stationary) {
        return switch (status) {
            case "Ready" -> 0;
            case "Entering address" -> stationary ? 2 : 0;
            case "Awaiting activation" -> 1;
            default -> 2;
        };
    }
}
