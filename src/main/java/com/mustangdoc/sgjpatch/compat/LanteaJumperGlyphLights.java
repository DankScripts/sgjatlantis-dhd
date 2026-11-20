package com.mustangdoc.sgjpatch.compat;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Client-side selected-symbol state for the physical puddle-jumper DHD. */
@OnlyIn(Dist.CLIENT)
public final class LanteaJumperGlyphLights {
    private static final Map<UUID, int[]> LIT = new ConcurrentHashMap<>();

    private LanteaJumperGlyphLights() {}

    public static void update(UUID jumper, String text, int totalSymbols) {
        if (jumper == null) return;
        if (text == null || text.isBlank()) {
            LIT.remove(jumper);
            return;
        }
        try {
            // Parsing the complete address would reject useful in-progress input. Validate
            // each token independently and illuminate every symbol entered so far.
            int[] mapped = Arrays.stream(text.trim().split("[-,\\s]+"))
                    .filter(token -> !token.isBlank())
                    .mapToInt(Integer::parseInt)
                    .filter(symbol -> symbol >= 0 && symbol < totalSymbols)
                    .map(LanteaJumperGlyphLights::toLanteaGlyph)
                    .filter(symbol -> symbol >= 0)
                    .distinct()
                    .toArray();
            if (mapped.length == 0) LIT.remove(jumper);
            else LIT.put(jumper, mapped);
        } catch (NumberFormatException ignored) {
            LIT.remove(jumper);
        }
    }

    /** Native Lantea key numbers already match the physical cockpit glyph IDs. */
    public static void updateNative(UUID jumper, String text, int totalSymbols) {
        if (jumper == null) return;
        if (text == null || text.isBlank()) { LIT.remove(jumper); return; }
        try {
            int[] symbols = Arrays.stream(text.trim().split("[-,\\s]+"))
                    .filter(token -> !token.isBlank())
                    .mapToInt(Integer::parseInt)
                    .filter(symbol -> symbol >= 0 && symbol < totalSymbols)
                    .distinct().toArray();
            if (symbols.length == 0) LIT.remove(jumper);
            else LIT.put(jumper, symbols);
        } catch (NumberFormatException ignored) { LIT.remove(jumper); }
    }

    public static int[] lit(UUID jumper) {
        int[] symbols = jumper == null ? null : LIT.get(jumper);
        return symbols == null ? new int[0] : symbols;
    }

    public static void clear(UUID jumper) {
        if (jumper != null) LIT.remove(jumper);
    }

    /**
     * Both mods use the named Pegasus order. SGJourney contains an extra Robandus
     * at numeric symbol 20, so IDs after it are one greater than Lantea's IDs.
     */
    static int toLanteaGlyph(int sgjSymbol) {
        if (sgjSymbol >= 0 && sgjSymbol <= 19) return sgjSymbol;
        if (sgjSymbol == 20) return 2; // SGJourney's duplicate Robandus
        if (sgjSymbol >= 21 && sgjSymbol <= 36) return sgjSymbol - 1;
        return -1; // Lantea has no physical key for SGJourney's extended symbols.
    }
}
