package com.mustangdoc.sgjpatch.standalone;

public final class AtlantisDialProgressTest {
    public static void main(String[] args) {
        checkCenter(AtlantisDialProgress.CenterAction.EMPTY, 0, false, false);
        checkCenter(AtlantisDialProgress.CenterAction.CLEAR, 0, true, false);
        checkCenter(AtlantisDialProgress.CenterAction.CLEAR, 0, false, true);
        checkCenter(AtlantisDialProgress.CenterAction.CLEAR, 5, true, false);
        checkCenter(AtlantisDialProgress.CenterAction.DIAL, 6, true, false);
        checkCenter(AtlantisDialProgress.CenterAction.DIAL, 8, true, true);
        int[] requested = {1, 2, 3, 4, 5, 6, 0};
        AtlantisDialProgress progress = new AtlantisDialProgress(requested);
        requested[0] = 38;
        int[] buffered = {1, 2, 3, 4, 5, 6, 0};

        expect(AtlantisDialProgress.Decision.WAIT,
                progress.decide(true, true, false, true, buffered, new int[] {1, 2, 3}));
        expect(AtlantisDialProgress.Decision.WAIT,
                progress.decide(true, true, false, true, buffered, new int[] {2, 1, 3, 4, 5, 6, 0}));
        expect(AtlantisDialProgress.Decision.ENGAGE,
                progress.decide(true, true, false, true, buffered, buffered.clone()));
        int[] nineChevron = {1, 2, 3, 4, 5, 6, 7, 8, 0};
        expect(AtlantisDialProgress.Decision.WAIT,
                new AtlantisDialProgress(nineChevron).decide(true, true, false, true, nineChevron,
                        new int[] {1, 2, 3, 4, 5, 6, 7, 8}));
        expect(AtlantisDialProgress.Decision.ENGAGE,
                new AtlantisDialProgress(nineChevron).decide(true, true, false, true, nineChevron,
                        nineChevron.clone()));
        expect(AtlantisDialProgress.Decision.CANCEL,
                progress.decide(false, true, false, true, buffered, buffered));
        expect(AtlantisDialProgress.Decision.CANCEL,
                progress.decide(true, false, false, true, buffered, buffered));
        expect(AtlantisDialProgress.Decision.CANCEL,
                progress.decide(true, true, true, true, buffered, buffered));
        expect(AtlantisDialProgress.Decision.CANCEL,
                progress.decide(true, true, false, false, buffered, buffered));
        expect(AtlantisDialProgress.Decision.CANCEL,
                progress.decide(true, true, false, true, new int[] {1, 2}, buffered));
        System.out.println("Atlantis pending-dial progress tests passed");
    }

    private static void checkCenter(AtlantisDialProgress.CenterAction expected, int regularSymbols,
                                    boolean buffered, boolean locked) {
        var actual = AtlantisDialProgress.centerAction(regularSymbols, buffered, locked);
        if (actual != expected) throw new AssertionError("Expected " + expected + ", got " + actual);
    }

    private static void expect(AtlantisDialProgress.Decision expected, AtlantisDialProgress.Decision actual) {
        if (expected != actual) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
