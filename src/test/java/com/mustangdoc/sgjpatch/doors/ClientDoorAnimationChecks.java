package com.mustangdoc.sgjpatch.doors;

public final class ClientDoorAnimationChecks {
    private ClientDoorAnimationChecks() {}

    private static void near(float actual, float expected) {
        if (Math.abs(actual - expected) > 0.0001f)
            throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    public static void run() {
        ClientDoorAnimation animation = new ClientDoorAnimation(1800);
        animation.setTarget(false, 0);
        animation.setTarget(true, 100);
        near(animation.progress(true, 1000), .5f);
        animation.setTarget(true, 1100);
        near(animation.progress(true, 1900), 1);
        animation.setTarget(false, 2000);
        near(animation.progress(false, 2900), .5f);
        animation.setTarget(true, 2900);
        near(animation.progress(true, 3800), .75f);
        ClientDoorAnimation initiallyOpen = new ClientDoorAnimation(1800);
        initiallyOpen.setTarget(true, 0);
        near(initiallyOpen.progress(true, 0), 1);
        System.out.println("PASS: client door animation advances from updates while offscreen and handles reversals.");
    }
}
