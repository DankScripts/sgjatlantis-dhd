package com.mustangdoc.sgjpatch.hatch;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** The two matching 13-block circular openings in the copied Atlantis city. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AtlantisHatchService {
    public static final int CENTER_X = -3892;
    public static final int CENTER_Z = 5885;
    public static final int[] HEIGHTS = {428, 443};
    private static final ResourceLocation LANTEA = new ResourceLocation("sgjourney", "lantea");
    private AtlantisHatchService() {}
    public static boolean isAtlantis(Level level) { return level.dimension().location().equals(LANTEA); }
    public static BlockPos center(int door) { return new BlockPos(CENTER_X, HEIGHTS[door], CENTER_Z); }
    public static boolean areaLoaded(Level level, int door) {
        for (BlockPos pos : positions(door)) if (!level.hasChunkAt(pos)) return false;
        return true;
    }
    public static AtlantisHatchIrisBlockEntity motor(Level level, int door) {
        return level.getBlockEntity(center(door)) instanceof AtlantisHatchIrisBlockEntity iris ? iris : null;
    }
    public static int status(Level level, int door) {
        if (!isAtlantis(level) || door < 0 || door >= HEIGHTS.length || !areaLoaded(level, door)) return 3;
        AtlantisHatchIrisBlockEntity iris = motor(level, door);
        if (iris != null) return iris.moving() ? (iris.targetOpen() ? 4 : 5) : iris.targetOpen() ? 1 : 0;
        int panels = 0, air = 0;
        for (BlockPos pos : positions(door)) {
            BlockState state = level.getBlockState(pos);
            if (state.is(AtlantisHatchRegistry.PANEL)) panels++;
            else if (state.isAir()) air++;
            else return 2;
        }
        return panels == 145 ? 0 : air == 145 ? 1 : 2;
    }
    public static boolean occupied(ServerLevel level, int door) {
        int y = HEIGHTS[door];
        AABB volume = new AABB(CENTER_X - 6, y - .25, CENTER_Z - 6, CENTER_X + 7, y + 1.25, CENTER_Z + 7);
        return !level.getEntities((net.minecraft.world.entity.Entity) null, volume, e -> !e.isSpectator() && !e.isRemoved()
                && (e instanceof LivingEntity || e.isPickable() || e.isVehicle())).isEmpty();
    }
    public static boolean setOpen(ServerLevel level, int door, boolean open, ServerPlayer operator) {
        if (!isAtlantis(level) || door < 0 || door >= HEIGHTS.length) return false;
        if (!areaLoaded(level, door)) return fail(operator, "Door area is not loaded.");
        for (BlockPos pos : positions(door)) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && !state.is(AtlantisHatchRegistry.PANEL))
                return fail(operator, "Doorway is obstructed at " + pos.toShortString() + ".");
        }
        if (!open && occupied(level, door)) return fail(operator, "Someone or a vehicle is in the doorway.");
        AtlantisHatchIrisBlockEntity iris = motor(level, door);
        if (iris == null) {
            boolean wasOpen = status(level, door) == 1;
            installMotor(level, door, wasOpen);
            iris = motor(level, door);
        }
        if (iris == null) return false;
        iris.request(open);
        return true;
    }
    private static void installMotor(ServerLevel level, int door, boolean open) {
        BlockState panel = AtlantisHatchRegistry.PANEL.defaultBlockState()
                .setValue(AtlantisHatchPanelBlock.UPPER, door == 1)
                .setValue(AtlantisHatchPanelBlock.IRIS, true)
                .setValue(AtlantisHatchPanelBlock.SEALED, !open);
        for (BlockPos pos : positions(door)) level.setBlock(pos, panel, 3);
        // Existing saves already contain this block ID, but predate its block entity.
        if (motor(level, door) == null)
            level.setBlockEntity(new AtlantisHatchIrisBlockEntity(center(door), panel));
        motor(level, door).initialize(open);
        setWeatherSeal(level, door, !open);
    }
    public static void setWeatherSeal(ServerLevel level, int door, boolean sealed) {
        for (BlockPos pos : positions(door)) {
            BlockState state = level.getBlockState(pos);
            boolean irisPanel = state.is(AtlantisHatchRegistry.PANEL)
                    && state.getValue(AtlantisHatchPanelBlock.IRIS);
            if (irisPanel && state.getValue(AtlantisHatchPanelBlock.SEALED) != sealed)
                level.setBlock(pos, state.setValue(AtlantisHatchPanelBlock.SEALED, sealed), 3);
            BlockPos cover = pos.above();
            BlockState above = level.getBlockState(cover);
            if (sealed && irisPanel && above.isAir()) {
                level.setBlock(cover, AtlantisHatchRegistry.WEATHER_SEAL.defaultBlockState(), 3);
            } else if ((!sealed || !irisPanel) && above.is(AtlantisHatchRegistry.WEATHER_SEAL)) {
                level.removeBlock(cover, false);
            }
        }
    }
    @SubscribeEvent public static void migrateExistingDoors(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)
                || !isAtlantis(level) || level.getGameTime() % 20 != 0) return;
        for (int door = 0; door < HEIGHTS.length; door++) {
            if (!areaLoaded(level, door)) continue;
            AtlantisHatchIrisBlockEntity iris = motor(level, door);
            if (iris == null && status(level, door) == 0) installMotor(level, door, false);
            else if (iris != null && !iris.moving()) setWeatherSeal(level, door, !iris.targetOpen());
        }
    }
    public static boolean toggle(ServerLevel level, int door, ServerPlayer operator) {
        AtlantisHatchIrisBlockEntity iris = motor(level, door);
        return setOpen(level, door, iris != null ? !iris.targetOpen() : status(level, door) == 0, operator);
    }
    private static boolean fail(ServerPlayer operator, String message) {
        if (operator != null) operator.displayClientMessage(Component.literal(message), true);
        return false;
    }
    public static Iterable<BlockPos> positions(int door) {
        java.util.List<BlockPos> result = new java.util.ArrayList<>(145);
        for (int dz = -6; dz <= 6; dz++) {
            int halfWidth = Math.min(6, 9 - Math.abs(dz));
            for (int dx = -halfWidth; dx <= halfWidth; dx++)
                result.add(new BlockPos(CENTER_X + dx, HEIGHTS[door], CENTER_Z + dz));
        }
        return result;
    }
}
