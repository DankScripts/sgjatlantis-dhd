package com.mustangdoc.sgjpatch.city;

import com.mustangdoc.sgjpatch.doors.AtlantisDoorBlock;
import com.mustangdoc.sgjpatch.doors.AtlantisDoorRegistry;
import com.mustangdoc.sgjpatch.doors.DoorControlBlock;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/** Reassembles city doors after structure chunks have finished replacing blocks. */
final class AtlantisCityDoorRepair {
    private static final ResourceLocation INDEX = ResourceLocation.fromNamespaceAndPath(
            "sgjadditions_capacity_patch", "atlantis_city_doors.txt");
    private static final ResourceLocation CONTROL_INDEX = ResourceLocation.fromNamespaceAndPath(
            "sgjadditions_capacity_patch", "atlantis_city_door_controls.txt");

    private AtlantisCityDoorRepair() {}

    static int restoreLoaded(ServerLevel level) {
        var resource = level.getServer().getResourceManager().getResource(INDEX);
        if (resource.isEmpty()) {
            System.err.println("[Atlantis DHD] Missing bundled city door index " + INDEX);
            return 0;
        }
        int repaired = 0;
        try (var reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length != 9) continue;
                BlockPos pos = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
                if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                        || !AtlantisCitySavedData.get(level).complete(pos.getX() >> 4, pos.getZ() >> 4)) continue;
                Direction facing = Direction.byName(parts[3]);
                if (facing == null) continue;
                BlockState expected = AtlantisDoorRegistry.BLOCK.defaultBlockState()
                        .setValue(AtlantisDoorBlock.FACING, facing)
                        .setValue(AtlantisDoorBlock.COL, Integer.parseInt(parts[4]))
                        .setValue(AtlantisDoorBlock.ROW, Integer.parseInt(parts[5]))
                        .setValue(AtlantisDoorBlock.SOLID, Boolean.parseBoolean(parts[6]))
                        .setValue(AtlantisDoorBlock.ALIGNED, Boolean.parseBoolean(parts[7]))
                        .setValue(AtlantisDoorBlock.STYLE, Integer.parseInt(parts[8]));
                BlockState current = level.getBlockState(pos);
                // Preserve the live animation stage and block entity of an intact door.
                if (current.is(AtlantisDoorRegistry.BLOCK)
                        && current.setValue(AtlantisDoorBlock.STAGE, 0).equals(expected)) continue;
                if (level.setBlock(pos, expected, 2)) repaired++;
            }
        } catch (Exception error) {
            System.err.println("[Atlantis DHD] Could not repair city doors: " + error);
        }
        return repaired + restoreControls(level);
    }

    private static int restoreControls(ServerLevel level) {
        var resource = level.getServer().getResourceManager().getResource(CONTROL_INDEX);
        if (resource.isEmpty()) {
            System.err.println("[Atlantis DHD] Missing bundled city door control index " + CONTROL_INDEX);
            return 0;
        }
        int repaired = 0;
        try (var reader = new BufferedReader(new InputStreamReader(resource.get().open(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                if (parts.length != 5) continue;
                BlockPos pos = new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]));
                if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                        || !AtlantisCitySavedData.get(level).complete(pos.getX() >> 4, pos.getZ() >> 4)
                        || !level.getBlockState(pos).isAir()) continue;
                Direction facing = Direction.byName(parts[3]);
                if (facing == null) continue;
                BlockState expected = AtlantisDoorRegistry.PANEL.defaultBlockState()
                        .setValue(DoorControlBlock.FACING, facing)
                        .setValue(DoorControlBlock.CRYSTAL, Boolean.parseBoolean(parts[4]));
                if (expected.canSurvive(level, pos) && level.setBlock(pos, expected, 3)) repaired++;
            }
        } catch (Exception error) {
            System.err.println("[Atlantis DHD] Could not repair city door controls: " + error);
        }
        return repaired;
    }
}
