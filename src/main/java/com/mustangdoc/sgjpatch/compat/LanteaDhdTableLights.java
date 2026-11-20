package com.mustangdoc.sgjpatch.compat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** Short-lived client display state for a stationary DHD linked to a native Lantea gate. */
@OnlyIn(Dist.CLIENT)
public final class LanteaDhdTableLights {
    private record Key(ResourceLocation dimension, BlockPos pos) {}
    private record Lit(boolean[] symbols, boolean connected, long expiresAt) {}
    private static final Map<Key, Lit> TABLES = new ConcurrentHashMap<>();
    private LanteaDhdTableLights() {}

    public static void receive(ResourceLocation dimension, BlockPos pos, String glyphSet,
                               int[] nativeSymbols, boolean connected) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !minecraft.level.dimension().location().equals(dimension)) return;
        Key key = new Key(dimension, pos.immutable());
        if (nativeSymbols.length == 0 && !connected) {
            TABLES.remove(key);
            return;
        }
        boolean[] tableSymbols = new boolean[39];
        for (int symbol : nativeSymbols) {
            int tableSymbol = "pg".equals(glyphSet) && symbol >= 20 ? symbol + 1 : symbol;
            if (tableSymbol >= 0 && tableSymbol < tableSymbols.length) tableSymbols[tableSymbol] = true;
        }
        TABLES.put(key, new Lit(tableSymbols, connected, System.nanoTime() + 2_000_000_000L));
    }

    public static boolean encoded(ResourceLocation dimension, BlockPos pos, int symbol) {
        Lit lit = current(dimension, pos);
        return lit != null && symbol >= 0 && symbol < lit.symbols.length && lit.symbols[symbol];
    }

    public static boolean connected(ResourceLocation dimension, BlockPos pos) {
        Lit lit = current(dimension, pos);
        return lit != null && lit.connected;
    }

    private static Lit current(ResourceLocation dimension, BlockPos pos) {
        Key key = new Key(dimension, pos);
        Lit lit = TABLES.get(key);
        if (lit != null && System.nanoTime() > lit.expiresAt) {
            TABLES.remove(key, lit);
            return null;
        }
        return lit;
    }
}
