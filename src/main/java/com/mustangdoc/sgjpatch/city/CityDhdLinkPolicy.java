package com.mustangdoc.sgjpatch.city;

final class CityDhdLinkPolicy {
    private CityDhdLinkPolicy() {}

    static boolean canReconnect(Object tableGate, Object gateDhd, Object expectedGate, Object expectedDhd,
                                boolean inRange, boolean gateBusy) {
        return (tableGate == null || tableGate == expectedGate)
                && (gateDhd == null || gateDhd == expectedDhd)
                && inRange && !gateBusy;
    }
}
