package com.mustangdoc.sgjpatch.client.render;

public final class PegasusShieldTransitionTest {
    public static void main(String[] args) {
        Object gate = new Object();
        Object otherGate = new Object();
        long second = 1_000_000_000L;
        if (PegasusShieldTransitionHelper.reveal(gate, false, 0) != 0F)
            throw new AssertionError("An initially open iris should not animate");
        float start = PegasusShieldTransitionHelper.reveal(gate, true, second);
        float closing = PegasusShieldTransitionHelper.reveal(gate, true, 2 * second);
        if (!(start < closing && closing < 1F))
            throw new AssertionError("Closing must progress through an intermediate state");
        float reversed = PegasusShieldTransitionHelper.reveal(gate, false, 2 * second);
        if (reversed != closing || PegasusShieldTransitionHelper.reveal(gate, false, 4 * second) != 0F)
            throw new AssertionError("Reversing mid-close should start from the current reveal and finish open");
        if (PegasusShieldTransitionHelper.reveal(otherGate, true, 4 * second) != 1F)
            throw new AssertionError("An initially closed iris should start fully visible");
        float openingStart = PegasusShieldTransitionHelper.reveal(otherGate, false, 5 * second);
        float opening = PegasusShieldTransitionHelper.reveal(otherGate, false, 6 * second);
        if (openingStart != 1F || !(opening > 0F && opening < 1F)
                || PegasusShieldTransitionHelper.reveal(otherGate, false, 7 * second) != 0F)
            throw new AssertionError("Opening must fade from fully visible to hidden");
        System.out.println("Native iris transition tests passed");
    }
}
