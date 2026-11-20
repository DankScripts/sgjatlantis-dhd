package com.mustangdoc.sgjpatch.city;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Wipes the city's mapped vertical footprint without draining the surrounding ocean. */
final class AtlantisCityReconciler {
    private static final BlockPos CITY_GATE = new BlockPos(-3876, 413, 5885);
    private AtlantisCityReconciler() {}

    static int clearForRebuild(ServerLevel level, int cx, int cz, Set<BlockPos> supports) throws IOException {
        // Read both maps before changing the world. Their union bounds each city
        // column; clearing the gaps too removes legacy blocks between old pieces.
        List<int[]> spaces = readSpans(level, cx, cz, "atlantis_city_spaces", false);
        List<int[]> solids = readSpans(level, cx, cz, "atlantis_city_wipe", true);
        var players = players(level, cx, cz);
        int[] low = new int[256];
        int[] high = new int[256];
        java.util.Arrays.fill(low, Integer.MAX_VALUE);
        for (int[] span : spaces) includeColumn(low, high, span);
        for (int[] span : solids) includeColumn(low, high, span);
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int x = 0; x < 256; x++) if (low[x] != Integer.MAX_VALUE) {
            minY = Math.min(minY, low[x]);
            maxY = Math.max(maxY, high[x]);
        }
        int cleared = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
            int index = x * 16 + z;
            for (int y = minY; y != Integer.MAX_VALUE && y <= maxY; y++) {
                cursor.set(cx * 16 + x, y, cz * 16 + z);
                BlockState state = level.getBlockState(cursor);
                if (state.isAir()) continue;
                boolean insideCityColumn = y >= low[index] && y <= high[index];
                // Outside the captured column, strip stray old construction but
                // retain the ocean and natural seabed around Atlantis.
                if (!insideCityColumn && isAmbientTerrain(state)) continue;
                if (!protectedPosition(level, cursor, players, supports)
                        && level.setBlock(cursor.immutable(), Blocks.AIR.defaultBlockState(), 18)) cleared++;
            }
        }
        return cleared;
    }

    private static boolean isAmbientTerrain(BlockState state) {
        return state.is(Blocks.WATER) || state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT) || state.is(Blocks.CLAY)
                || state.is(Blocks.SAND) || state.is(Blocks.SEAGRASS)
                || state.is(Blocks.TALL_SEAGRASS) || state.is(Blocks.KELP)
                || state.is(Blocks.KELP_PLANT) || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.SNOW) || state.is(Blocks.BUBBLE_COLUMN);
    }

    private static void includeColumn(int[] low, int[] high, int[] span) {
        int index = span[0] * 16 + span[1];
        low[index] = Math.min(low[index], span[2]);
        high[index] = Math.max(high[index], span[3]);
    }

    static int restoreSpaces(ServerLevel level, int cx, int cz, Set<BlockPos> supports) throws IOException {
        List<int[]> spans = readSpans(level, cx, cz, "atlantis_city_spaces", false);
        var players = players(level, cx, cz);
        int changed = 0;
        for (int[] span : spans) {
            // Discard fluid work scheduled while the city's shell was incomplete.
            // Limit cancellation to captured interior air, preserving ocean ticks.
            if (span[4] < 0) level.getFluidTicks().clearArea(
                    new net.minecraft.world.level.levelgen.structure.BoundingBox(
                        cx * 16 + span[0], span[2], cz * 16 + span[1],
                        cx * 16 + span[0], span[3], cz * 16 + span[1]));
            var desired = span[4] < 0 ? Blocks.AIR.defaultBlockState()
                    : Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, span[4]);
            for (int y = span[2]; y <= span[3]; y++) {
                BlockPos pos = new BlockPos(cx * 16 + span[0], y, cz * 16 + span[1]);
                // Clearing water from an occupied room is safe even at the
                // player's feet; keep solid support blocks protected.
                var current = level.getBlockState(pos);
                if (current.equals(desired)) continue;
                if (protectedPosition(level, pos, players, supports)
                        && !(desired.isAir() && current.is(Blocks.WATER))) continue;
                if (level.setBlock(pos, desired, 18)) changed++;
            }
        }
        return changed;
    }

    private static List<Player> players(ServerLevel level, int cx, int cz) {
        return level.getEntitiesOfClass(Player.class,
                new AABB(cx * 16 - 1, 200, cz * 16 - 1, cx * 16 + 17, 514, cz * 16 + 17));
    }

    private static boolean protectedPosition(ServerLevel level, BlockPos pos, List<Player> players,
                                             Set<BlockPos> supports) {
        if (CITY_GATE.equals(pos) || supports.contains(pos)) return true;
        var id = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(level.getBlockState(pos).getBlock());
        if (id != null && "sgjourney".equals(id.getNamespace()) && id.getPath().contains("stargate"))
            return true;
        return players.stream().anyMatch(player -> pos.equals(player.getOnPos()));
    }

    private static List<int[]> readSpans(ServerLevel level, int cx, int cz,
                                         String folder, boolean solids) throws IOException {
        ResourceLocation id = new ResourceLocation("sgjadditions_capacity_patch",
                folder + "/chunk_" + cx + "_" + cz + ".txt");
        var resource = level.getServer().getResourceManager().getResourceOrThrow(id);
        var spans = new ArrayList<int[]>();
        try (var reader = new BufferedReader(new InputStreamReader(resource.open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] fields = line.split(" ");
                if (fields.length != (solids ? 4 : 5)) throw new IOException("Invalid city span: " + id);
                int[] span = new int[5];
                try {
                    for (int i = 0; i < fields.length; i++) span[i] = Integer.parseInt(fields[i]);
                } catch (NumberFormatException error) {
                    throw new IOException("Invalid city span: " + id, error);
                }
                if (span[0] < 0 || span[0] > 15 || span[1] < 0 || span[1] > 15
                        || span[2] < 201 || span[3] > 511 || span[2] > span[3]
                        || !solids && (span[4] < -1 || span[4] > 15))
                    throw new IOException("Out-of-bounds city span: " + id);
                if (solids) span[4] = 16;
                spans.add(span);
            }
        }
        return spans;
    }
}
