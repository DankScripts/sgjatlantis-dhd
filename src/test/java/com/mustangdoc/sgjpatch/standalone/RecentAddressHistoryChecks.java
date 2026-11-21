package com.mustangdoc.sgjpatch.standalone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public final class RecentAddressHistoryChecks {
    private RecentAddressHistoryChecks() {}

    public static void run() {
        var data = new CompoundTag();
        for (int i = 1; i <= 9; i++)
            RecentAddressHistory.record(data, "address-" + i);

        var entries = data.getList("AtlantisRecentAddresses", Tag.TAG_STRING);
        if (entries.size() != 8 || !entries.getString(0).equals("address-9")
                || !entries.getString(7).equals("address-2"))
            throw new AssertionError("Recent addresses must be newest first and limited to eight");

        RecentAddressHistory.record(data, "address-4");
        entries = data.getList("AtlantisRecentAddresses", Tag.TAG_STRING);
        if (entries.size() != 8 || !entries.getString(0).equals("address-4")
                || !entries.getString(1).equals("address-9")
                || !data.getString("AtlantisRecentAddressesLast").equals("address-4"))
            throw new AssertionError("Repeated addresses must move to the front without duplication");
        for (int i = 1; i < entries.size(); i++)
            if (entries.getString(i).equals("address-4"))
                throw new AssertionError("Repeated address remained in history");
        if (!RecentAddressHistory.isLast(data, "address-4") || RecentAddressHistory.isLast(data, "address-9"))
            throw new AssertionError("Last address marker was not preserved");

        var update = new CompoundTag();
        var client = new CompoundTag();
        RecentAddressHistory.copyTo(data, update);
        RecentAddressHistory.copyTo(update, client);
        RecentAddressHistory.record(data, "address-10");
        if (!client.getList("AtlantisRecentAddresses", Tag.TAG_STRING).getString(0).equals("address-4"))
            throw new AssertionError("Client history must be copied independently");
        System.out.println("PASS: Atlantis recent address ordering, capacity, duplicates and update copying.");
    }
}
