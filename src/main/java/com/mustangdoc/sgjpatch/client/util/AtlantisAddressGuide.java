package com.mustangdoc.sgjpatch.client.util;

import com.mustangdoc.sgjpatch.network.AtlantisAddressLookup;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.Tag;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.sgjourney.Address;

/** Client-side address selection. SGJourney still performs every button press and dial. */
public final class AtlantisAddressGuide {
    private static final Map<String, String> NAMES = new HashMap<>();
    private static final Set<String> REQUESTED = new HashSet<>();
    private static AbstractDHDEntity activeDhd;
    private static String selected;
    private static String message = "";
    private static int[] keys;

    private AtlantisAddressGuide() {}

    public static void begin(AbstractDHDEntity dhd) {
        activeDhd = dhd;
        selected = null;
        keys = null;
        message = "";
        NAMES.clear();
        REQUESTED.clear();
    }

    public static boolean paste(AbstractDHDEntity dhd) {
        String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
        var match = AtlantisClipboardAddress.find(clipboard);
        if (match == null) {
            message = "Clipboard has no SGJourney address";
            return true;
        }
        String prefix = clipboard.substring(Math.max(0, match.start()-3), match.start()).toLowerCase(java.util.Locale.ROOT);
        if (prefix.endsWith("mw:") || prefix.endsWith("pg:")) {
            message = "Lantea address format needs a Lantea dialer";
            return true;
        }
        select(dhd, AtlantisClipboardAddress.withOrigin(match));
        return true;
    }

    public static void select(AbstractDHDEntity dhd, String address) {
        if (activeDhd != dhd) begin(dhd);
        int[] parsed = Address.addressStringToIntArray(address);
        if (parsed.length < 7 || parsed.length > 9 || parsed[parsed.length - 1] != 0) {
            message = "Address needs 6-8 symbols and origin 0";
            return;
        }
        Set<Integer> seen = new HashSet<>();
        for (int i = 0; i < parsed.length - 1; i++) {
            if (parsed[i] < 1 || parsed[i] > 38 || !seen.add(parsed[i])) {
                message = "Address has an unavailable or repeated symbol";
                return;
            }
        }
        selected = address;
        keys = parsed;
        message = "";
        request(dhd, address);
    }

    public static void request(AbstractDHDEntity dhd, String address) {
        if (activeDhd != dhd) begin(dhd);
        if (!REQUESTED.add(address)) return;
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu<?> menu
                && menu.getDHD() == dhd) AtlantisAddressLookup.request(menu.containerId, dhd.getBlockPos(), address);
    }

    public static void receive(int container, net.minecraft.core.BlockPos pos, String address, String name) {
        var player = Minecraft.getInstance().player;
        if (player == null || !(player.containerMenu instanceof net.povstalec.sgjourney.common.menu.dhd.AbstractDHDMenu<?> menu)
                || menu.containerId != container || activeDhd != menu.getDHD() || !activeDhd.getBlockPos().equals(pos)) return;
        NAMES.put(address, name);
    }

    public static String name(String address) {
        String value = NAMES.getOrDefault(address, "Checking destination...");
        return value.startsWith("@") ? net.minecraft.network.chat.Component.translatable(value.substring(1)).getString() : value;
    }
    public static boolean selected(String address) { return address != null && address.equals(selected); }
    public static String selectedName() { return selected == null ? "" : name(selected); }

    /** -1 means no next symbol; -2 means current manual entry differs from the guide. */
    public static int next(AbstractDHDEntity dhd) {
        if (activeDhd != dhd || keys == null) return -1;
        int[] entered = dhd.getAddress().toArray();
        if (entered.length > keys.length) return -2;
        for (int i = 0; i < entered.length; i++) if (entered[i] != keys[i]) return -2;
        return entered.length == keys.length ? -1 : keys[entered.length];
    }

    public static boolean highlights(AbstractDHDEntity dhd, int symbol) { return symbol == next(dhd); }

    public static String status(AbstractDHDEntity dhd) {
        if (!message.isEmpty()) return message;
        if (activeDhd != dhd || selected == null) return "Ctrl+V to paste an address";
        int next = next(dhd);
        if (next == -2) return "Clear the current entry to follow " + selectedName();
        if (next == -1) return "Address ready: press the center crystal";
        return selectedName() + "  •  press symbol " + next + " (" + (dhd.getAddress().getLength()+1) + "/" + keys.length + ")";
    }

    public static String[] recent(AbstractDHDEntity dhd) {
        var entries = dhd.getPersistentData().getList("AtlantisRecentAddresses", Tag.TAG_STRING);
        String[] result = new String[Math.min(8, entries.size())];
        for (int i = 0; i < result.length; i++) result[i] = entries.getString(i);
        return result;
    }
}
