package com.mustangdoc.sgjpatch.city;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Replaces only the exact retired-block positions in an already-installed city. */
public final class AtlantisBuildingMigration {
    private static final String RESOURCE = "/data/sgjadditions_capacity_patch/atlantis_building_migration.txt";
    private static Map<Long, List<Entry>> byChunk;
    private AtlantisBuildingMigration() {}

    public static void apply(ServerLevel level, int cx, int cz) {
        for (Entry entry : entries().getOrDefault(ChunkPos.asLong(cx, cz), List.of())) {
            BlockPos pos = new BlockPos(cx * 16 + entry.lx, entry.y, cz * 16 + entry.lz);
            BlockState current = level.getBlockState(pos);
            String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(current.getBlock()));
            String old = switch (entry.name) {
                case "ancientpillar" -> "minecraft:quartz_pillar";
                case "ancientlight" -> "minecraft:sea_lantern";
                case "ancientstairs" -> "minecraft:quartz_stairs";
                default -> "";
            };
            if (!id.equals(old) && !id.equals("sgjadditions:" + entry.name)) continue;
            Block block = AtlantisBuildingBlocks.get(entry.name);
            if (block == null) continue;
            BlockState replacement = block.defaultBlockState();
            Direction facing = Direction.byName(entry.facing);
            if (facing != null && replacement.hasProperty(BlockStateProperties.HORIZONTAL_FACING))
                replacement = replacement.setValue(BlockStateProperties.HORIZONTAL_FACING, facing);
            if (block instanceof StairBlock && current.hasProperty(StairBlock.HALF)) {
                replacement = replacement.setValue(StairBlock.HALF, current.getValue(StairBlock.HALF));
                replacement = replacement.setValue(StairBlock.SHAPE, current.getValue(StairBlock.SHAPE));
            }
            level.setBlock(pos, replacement, 2);
        }
    }

    private static synchronized Map<Long, List<Entry>> entries() {
        if (byChunk != null) return byChunk;
        Map<Long, List<Entry>> found = new HashMap<>();
        try (InputStream stream = AtlantisBuildingMigration.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) throw new IllegalStateException("Missing city migration resource " + RESOURCE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
            for (String line; (line = reader.readLine()) != null; ) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] values = line.split(" ");
                int cx = Integer.parseInt(values[0]), cz = Integer.parseInt(values[1]);
                Entry entry = new Entry(Integer.parseInt(values[2]), Integer.parseInt(values[3]),
                        Integer.parseInt(values[4]), values[5], values[6]);
                found.computeIfAbsent(ChunkPos.asLong(cx, cz), ignored -> new ArrayList<>()).add(entry);
            }
        } catch (Exception error) {
            throw new IllegalStateException("Could not load Atlantis building migration", error);
        }
        byChunk = found;
        return found;
    }

    private record Entry(int lx, int y, int lz, String name, String facing) {}
}
