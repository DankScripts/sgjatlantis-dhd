package com.mustangdoc.sgjpatch.standalone;

import java.util.Arrays;

final class AtlantisDialProgress {
    enum Decision { WAIT, ENGAGE, CANCEL }
    enum CenterAction { EMPTY, CLEAR, DIAL }

    static CenterAction centerAction(int regularSymbols, boolean hasBufferedSymbols, boolean hasLockedSymbols) {
        if (regularSymbols >= 6) return CenterAction.DIAL;
        return hasBufferedSymbols || hasLockedSymbols ? CenterAction.CLEAR : CenterAction.EMPTY;
    }

    private final int[] address;

    AtlantisDialProgress(int[] address) {
        this.address = address.clone();
    }

    Decision decide(boolean sameGate, boolean gateLoaded, boolean connected, boolean crystals,
                    int[] bufferedAddress, int[] lockedAddress) {
        if (!sameGate || !gateLoaded || connected || !crystals || !Arrays.equals(address, bufferedAddress))
            return Decision.CANCEL;
        return Arrays.equals(address, lockedAddress) ? Decision.ENGAGE : Decision.WAIT;
    }
}
