package com.mustangdoc.sgjpatch.client.util;

import java.util.regex.MatchResult;
import java.util.regex.Pattern;

final class AtlantisClipboardAddress {
    private static final Pattern ADDRESS = Pattern.compile("(?<![0-9-])-?((?:[0-9]{1,2}-){5,8}[0-9]{1,2})-?(?![0-9-])");

    private AtlantisClipboardAddress() {}

    static MatchResult find(String clipboard) {
        var match = ADDRESS.matcher(clipboard);
        return match.find() ? match.toMatchResult() : null;
    }

    static String withOrigin(MatchResult match) {
        String address = match.group(1);
        return address.endsWith("-0") || address.endsWith("-00") ? address : address + "-0";
    }
}
