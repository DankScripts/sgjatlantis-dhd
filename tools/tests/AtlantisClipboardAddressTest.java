package com.mustangdoc.sgjpatch.client.util;

public final class AtlantisClipboardAddressTest {
    public static void main(String[] args) {
        expect("-18-20-1-15-14-7-19-", "18-20-1-15-14-7-19-0");
        expect("Destination: -1-2-3-4-5-6-", "1-2-3-4-5-6-0");
        expect("-1-2-3-4-5-6-7-8-", "1-2-3-4-5-6-7-8-0");
        expect("-18-20-1-15-14-7-19-0-", "18-20-1-15-14-7-19-0");
        if (AtlantisClipboardAddress.find("-1-2-3-4-5-6-7-8-9-10-") != null)
            throw new AssertionError("Overlong addresses must not be truncated");
        System.out.println("Atlantis clipboard address tests passed");
    }

    private static void expect(String clipboard, String expected) {
        var match = AtlantisClipboardAddress.find(clipboard);
        String actual = match == null ? null : AtlantisClipboardAddress.withOrigin(match);
        if (!expected.equals(actual))
            throw new AssertionError("Expected " + expected + ", got " + actual);
    }
}
