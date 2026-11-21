package com.mustangdoc.sgjpatch.standalone;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

final class RecentAddressHistory {
    private static final String KEY = "AtlantisRecentAddresses";
    private static final String LAST_KEY = KEY + "Last";

    private RecentAddressHistory() {}

    static boolean isLast(CompoundTag data, String address) {
        return address.equals(data.getString(LAST_KEY));
    }

    static void record(CompoundTag data, String address) {
        ListTag old = data.getList(KEY, Tag.TAG_STRING);
        ListTag entries = new ListTag();
        entries.add(StringTag.valueOf(address));
        for (int i = 0; i < old.size() && entries.size() < 8; i++)
            if (!address.equals(old.getString(i)))
                entries.add(StringTag.valueOf(old.getString(i)));
        data.put(KEY, entries);
        data.putString(LAST_KEY, address);
    }

    static void copyTo(CompoundTag source, CompoundTag target) {
        target.put(KEY, source.getList(KEY, Tag.TAG_STRING).copy());
    }
}
