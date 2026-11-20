package com.mustangdoc.sgjpatch.compat;

public final class NativeDhdCenterActionTest {
    public static void main(String[] args) {
        expect("Entering address", true, 2);
        expect("Awaiting activation", true, 1);
        expect("Ready", true, 0);
        expect("Connected", true, 2);
        expect("Entering address", false, 0);
        System.out.println("Native Atlantis centre button tests passed");
    }

    private static void expect(String status, boolean stationary, int action) {
        int actual = NativeDhdCenterAction.forStatus(status, stationary);
        if (actual != action)
            throw new AssertionError(status + " (stationary=" + stationary + "): expected " + action + ", got " + actual);
    }
}
