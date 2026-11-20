package com.mustangdoc.sgjpatch.city;

public final class CityDhdLinkPolicyTest {
    public static void main(String[] args) {
        Object gate = new Object(), table = new Object(), other = new Object();
        check(CityDhdLinkPolicy.canReconnect(null, null, gate, table, true, false), "unclaimed pair should reconnect");
        check(CityDhdLinkPolicy.canReconnect(null, table, gate, table, true, false), "gate's one-sided link to this table should recover");
        check(CityDhdLinkPolicy.canReconnect(gate, null, gate, table, true, false), "table's one-sided link to this gate should recover");
        check(!CityDhdLinkPolicy.canReconnect(other, table, gate, table, true, false), "table's link to another gate must be preserved");
        check(!CityDhdLinkPolicy.canReconnect(gate, other, gate, table, true, false), "gate's link to another DHD must be preserved");
        check(!CityDhdLinkPolicy.canReconnect(null, table, gate, table, false, false), "out-of-range pair must not reconnect");
        check(!CityDhdLinkPolicy.canReconnect(null, table, gate, table, true, true), "busy gate must not reconnect");
        System.out.println("City DHD link safety tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
