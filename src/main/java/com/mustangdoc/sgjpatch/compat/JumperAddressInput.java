package com.mustangdoc.sgjpatch.compat;

import java.util.Arrays;
import java.util.HashSet;

/** Parses a complete SGJourney address for the jumper's staged dialing sequence. */
public final class JumperAddressInput {
    private JumperAddressInput() {}
    public static int[] parse(String input, int totalSymbols) {
        String text = input.trim();
        if (text.isEmpty() || text.length() > 80 || !text.matches("[0-9,\\s-]+"))
            throw new IllegalArgumentException("Enter SGJourney symbol numbers separated by spaces.");
        int[] symbols;
        try {
            symbols = Arrays.stream(text.split("[\\s,-]+"))
                    .mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Invalid symbol number.");
        }
        boolean hasOrigin = symbols.length > 0 && symbols[symbols.length - 1] == 0;
        int destinationLength = symbols.length - (hasOrigin ? 1 : 0);
        if (destinationLength < 6 || destinationLength > 8)
            throw new IllegalArgumentException("Use 6, 7 or 8 destination symbols; origin 0 is optional.");
        var seen = new HashSet<Integer>();
        for (int i = 0; i < destinationLength; i++) {
            int symbol = symbols[i];
            if (symbol <= 0 || symbol >= totalSymbols || !seen.add(symbol))
                throw new IllegalArgumentException("Symbols must be unique and between 1 and " + (totalSymbols - 1) + ".");
        }
        if (hasOrigin) return symbols;
        int[] complete = Arrays.copyOf(symbols, symbols.length + 1);
        complete[complete.length - 1] = 0;
        return complete;
    }
}
