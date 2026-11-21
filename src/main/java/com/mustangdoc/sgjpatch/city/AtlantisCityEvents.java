package com.mustangdoc.sgjpatch.city;

import com.mojang.brigadier.CommandDispatcher;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchRegistry;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchService;
import com.mustangdoc.sgjpatch.hatch.AtlantisHatchTableBlock;
import com.mustangdoc.sgjpatch.mixin.CityStargateCacheAccessor;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import com.mustangdoc.sgjpatch.standalone.AtlantisBlockEntityDHD;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.data.BlockEntityList;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.stargate.pegasus.PegasusBlockEntityStargate;

/** Places the exact copied city a few chunks per tick without replacing existing builds. */
@EventBusSubscriber(modid = "sgjatlantis_dhd")
public final class AtlantisCityEvents {
    private static final String NS = "sgjadditions_capacity_patch";
    private static final ResourceLocation MANIFEST = ResourceLocation.fromNamespaceAndPath(NS, "atlantis_city_manifest.txt");
    private static final ResourceKey<Level> LANTEA = ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath("sgjourney", "lantea"));
    private static final Map<ServerLevel, Deque<Long>> PENDING = new WeakHashMap<>();
    private static final Map<ServerLevel, Set<Long>> QUEUED = new WeakHashMap<>();
    private static final Map<ServerLevel, ManualInstall> MANUAL = new WeakHashMap<>();
    private static final Set<ServerLevel> GATE_REGISTERED =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<ServerLevel> DHD_STOCKED =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<PegasusStargateEntity, WeakReference<PegasusBlockEntityStargate>> DHD_CALLBACKS =
            new WeakHashMap<>();
    private static final Map<ServerLevel, String> LINK_WARNINGS = new WeakHashMap<>();
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private static final BlockPos CITY_GATE = new BlockPos(-3876, 413, 5885);
    private static final BlockPos CITY_DHD = new BlockPos(-3895, 418, 5872);
    private static final BlockPos LEGACY_CITY_DHD = new BlockPos(-3899, 418, 5876);
    // Fallback for a bundled gate with no valid saved address.
    private static final Address.Immutable BUNDLED_GATE_ADDRESS =
            new Address.Immutable(10, 3, 32, 6, 11, 30, 19, 18, 0);
    private static Map<Long, Entry> manifest;

    private AtlantisCityEvents() {}

    public static boolean cityInstallationRunning(ServerLevel level) {
        synchronized (PENDING) { return MANUAL.containsKey(level); }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("sgjatlantis")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("city")
                        .then(Commands.literal("install")
                                .executes(context -> startManualInstall(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> manualStatus(context.getSource())))));
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !LANTEA.equals(level.dimension())) return;
        int cx = event.getChunk().getPos().x;
        int cz = event.getChunk().getPos().z;
        int gateCx = CITY_GATE.getX() >> 4;
        int gateCz = CITY_GATE.getZ() >> 4;
        // Restored structure block entities can begin ticking before the END-phase
        // level event gets a chance to repair SGJourney's saved lookup. Register the
        // bundled gate as soon as both chunks containing its base/ring are loaded.
        // Chunk-load callbacks run while the chunk is still being published.
        // Looking up a block entity here can synchronously wait on this same load.
        long key = ChunkPos.asLong(cx, cz);
        // Read the bundled index lazily when the first Atlantis chunk arrives.
        if (!entries(level).containsKey(key)) return;
        synchronized (PENDING) {
            if (QUEUED.computeIfAbsent(level, ignored -> new HashSet<>()).add(key))
                PENDING.computeIfAbsent(level, ignored -> new ArrayDeque<>()).addLast(key);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !LANTEA.equals(level.dimension())) return;
        Deque<Long> queue;
        synchronized (PENDING) { queue = PENDING.get(level); }
        for (int i = 0; i < 2 && queue != null && !MANUAL.containsKey(level); i++) {
            Long key;
            synchronized (PENDING) {
                key = queue.pollFirst();
                if (key != null) QUEUED.get(level).remove(key);
            }
            if (key == null) break;
            Entry entry = entries(level).get(key);
            if (entry != null && level.getChunkSource().hasChunk(entry.cx, entry.cz))
                process(level, entry);
        }
        processManualInstall(level);
        BlockPos cityDhd = installedCityDhd(level);
        if (level.getGameTime() % 20 == 0 && !DHD_STOCKED.contains(level)
                && level.hasChunkAt(cityDhd)
                && level.getBlockEntity(cityDhd) instanceof com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity dhd) {
            dhd.installBundledDialingCrystals();
            if (dhd.hasBundledControlCrystal() && dhd.hasRequiredDialingCrystals())
                DHD_STOCKED.add(level);
        }
        if (level.getGameTime() % 20 == 0 && !MANUAL.containsKey(level)) {
            installHatches(level);
            repairBundledDhdFacing(level);
        }
        // Structure-restored gates can skip SGJourney's normal registration path.
        if (level.getGameTime() % 20 == 0 && !MANUAL.containsKey(level)) {
            if (level.getGameTime() % 100 == 0 && !GATE_REGISTERED.contains(level))
                ensureCityGateRegistered(level, false, null);
            repairCityDhdLink(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            DHD_STOCKED.remove(level);
            synchronized (PENDING) {
                PENDING.remove(level);
                QUEUED.remove(level);
                MANUAL.remove(level);
                GATE_REGISTERED.remove(level);
            }
        }
    }

    private static ProcessResult process(ServerLevel level, Entry entry) {
        return process(level, entry, false);
    }

    private static ProcessResult process(ServerLevel level, Entry entry, boolean reviewCompleted) {
        AtlantisCitySavedData data = AtlantisCitySavedData.get(level);
        if (!reviewCompleted && data.complete(entry.cx, entry.cz))
            AtlantisBuildingMigration.apply(level, entry.cx, entry.cz);
        if (!reviewCompleted && data.complete(entry.cx, entry.cz)) return ProcessResult.COMPLETE;
        boolean sourceAlreadyPresent = false;
        boolean occupied = false;
        int matched = 0;
        for (Sample sample : entry.samples) {
            var current = level.getBlockState(sample.pos);
            String id = String.valueOf(BuiltInRegistries.BLOCK.getKey(current.getBlock()));
            if (id.equals(sample.blockId)) matched++;
            if (!current.isAir() && !isAmbientTerrain(sample.pos, id)) occupied = true;
        }
        sourceAlreadyPresent = matched > 0 && matched * 2 >= entry.samples.size();
        if (!reviewCompleted && (sourceAlreadyPresent || occupied)) {
            // A copied or edited city chunk belongs to the player; never replace it.
            data.markComplete(entry.cx, entry.cz);
            if (sourceAlreadyPresent) AtlantisBuildingMigration.apply(level, entry.cx, entry.cz);
            return sourceAlreadyPresent ? ProcessResult.PRESENT : ProcessResult.OCCUPIED;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(NS,
                "atlantis_city/chunk_" + entry.cx + "_" + entry.cz);
        StructureTemplate template = level.getStructureManager().get(id).orElse(null);
        if (template == null) {
            System.err.println("[Atlantis DHD] Missing bundled city chunk " + id);
            return ProcessResult.MISSING;
        }
        BlockPos origin = new BlockPos(entry.cx * 16, 201, entry.cz * 16);
        // Manual installs clear the whole city in a separate first pass.
        // Preserve an already-restored puddle jumper instead of duplicating it.
        boolean ignoreEntities = reviewCompleted && hasCityVehicle(level, entry);
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(ignoreEntities).setKnownShape(true);
        if (reviewCompleted) settings.setLiquidSettings(
                net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING);
        var transporterBounds = reviewCompleted ? List.<AABB>of() : com.mustangdoc.sgjpatch.transporter.TransporterNetwork.get(level).stations().stream()
                .map(s -> com.mustangdoc.sgjpatch.transporter.TransporterRoom.interior(s.pos(), s.facing()).inflate(1)).toList();
        if (!transporterBounds.isEmpty()) settings.addProcessor(new StructureProcessor() {
            @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos offset,
                    BlockPos pos, StructureTemplate.StructureBlockInfo raw,
                    StructureTemplate.StructureBlockInfo placed, StructurePlaceSettings options) {
                for (AABB bounds : transporterBounds) if (bounds.contains(placed.pos().getCenter())) return null;
                return placed;
            }
            @Override protected StructureProcessorType<?> getType() { return StructureProcessorType.BLOCK_IGNORE; }
        });
        // A repair must never replace the existing gate base. Replacing its block entity
        // gives it a new address while SGJourney retains the old address in its network.
        if (level.getBlockEntity(CITY_GATE) instanceof AbstractStargateEntity<?>)
            settings.addProcessor(PRESERVE_EXISTING_CITY_GATE);
        if (reviewCompleted) {
            Set<BlockPos> supports = new HashSet<>();
            for (var player : level.players()) supports.add(player.getOnPos());
            ManualInstall job = MANUAL.get(level);
            if (job != null) supports.addAll(job.supports);
            if (!supports.isEmpty()) settings.addProcessor(new StructureProcessor() {
                @Override public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos offset,
                        BlockPos origin, StructureTemplate.StructureBlockInfo original,
                        StructureTemplate.StructureBlockInfo placed, StructurePlaceSettings placement) {
                    return supports.contains(placed.pos()) ? null : placed;
                }
                @Override protected StructureProcessorType<?> getType() { return StructureProcessorType.BLOCK_IGNORE; }
            });
        }
        if (template.placeInWorld(level, origin, origin, settings, level.random, 18)) {
            if (entry.cx == (CITY_DHD.getX() >> 4) && entry.cz == (CITY_DHD.getZ() >> 4)) {
                if (!(level.getBlockEntity(CITY_DHD) instanceof com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity dhd)) {
                    System.err.println("[Atlantis DHD] Missing bundled city DHD at " + CITY_DHD);
                    return ProcessResult.FAILED;
                }
                dhd.installBundledDialingCrystals();
                if (!dhd.hasBundledControlCrystal() || !dhd.hasRequiredDialingCrystals()) {
                    System.err.println("[Atlantis DHD] Could not stock bundled city DHD at " + CITY_DHD);
                    return ProcessResult.FAILED;
                }
            }
            data.markComplete(entry.cx, entry.cz);
            if (!reviewCompleted) AtlantisCityDoorRepair.restoreLoaded(level);
            return ProcessResult.PLACED;
        }
        return ProcessResult.FAILED;
    }

    private static boolean hasCityVehicle(ServerLevel level, Entry entry) {
        AABB chunkBounds = new AABB(entry.cx * 16, 201, entry.cz * 16,
                entry.cx * 16 + 16, 512, entry.cz * 16 + 16);
        return !level.getEntities((Entity) null, chunkBounds, entity ->
                "lantea:puddle_jumper".equals(String.valueOf(
                        BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())))).isEmpty();
    }

    private static int startManualInstall(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(LANTEA);
        if (level == null) {
            source.sendFailure(Component.literal("The SGJourney Lantea dimension is not available."));
            return 0;
        }
        Map<Long, Entry> city = entries(level);
        if (city.isEmpty()) {
            source.sendFailure(Component.literal("The bundled Atlantis city manifest could not be loaded."));
            return 0;
        }
        synchronized (PENDING) {
            ManualInstall active = MANUAL.get(level);
            if (active != null) {
                source.sendFailure(Component.literal("Atlantis city installation is already running: "
                        + active.finished() + "/" + active.total + " chunks checked."));
                return 0;
            }
            List<Entry> ordered = new ArrayList<>(city.values());
            ordered.sort((a, b) -> a.cx != b.cx ? Integer.compare(a.cx, b.cx) : Integer.compare(a.cz, b.cz));
            Set<BlockPos> supports = new HashSet<>();
            for (var player : level.players()) supports.add(player.getOnPos());
            level.getChunk(CITY_GATE);
            Address.Immutable gateAddress = level.getBlockEntity(CITY_GATE) instanceof PegasusStargateEntity gate
                    && isGateAddress(gate.get9ChevronAddress()) ? gate.get9ChevronAddress() : null;
            GATE_REGISTERED.remove(level);
            MANUAL.put(level, new ManualInstall(ordered, source, supports, gateAddress));
        }
        source.sendSuccess(() -> Component.literal("Started Atlantis city rebuild in Lantea at "
                + AtlantisLightingConfig.cityInstallSpeed() + " speed: " + city.size()
                + " bundled city chunks will be cleared first, then rebuilt and drained in batches."), true);
        return 1;
    }

    private static int manualStatus(CommandSourceStack source) {
        ServerLevel level = source.getServer().getLevel(LANTEA);
        if (level == null) {
            source.sendFailure(Component.literal("The SGJourney Lantea dimension is not available."));
            return 0;
        }
        ManualInstall job;
        synchronized (PENDING) { job = MANUAL.get(level); }
        if (job == null) {
            source.sendSuccess(() -> Component.literal("No manual Atlantis city installation is running."), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal(job.progress()), false);
        return 1;
    }

    private static void processManualInstall(ServerLevel level) {
        ManualInstall job;
        synchronized (PENDING) { job = MANUAL.get(level); }
        if (job == null) return;
        var speed = AtlantisLightingConfig.cityInstallSpeed();
        int interval = switch (speed) {
            case HIGH -> 1;
            case MEDIUM -> 1;
            case LOW -> 4;
        };
        if (level.getGameTime() % interval != 0) return;
        int chunksThisTick = speed == AtlantisLightingConfig.CityInstallSpeed.HIGH
                && job.phase != ManualPhase.CLEAR ? 2 : 1;
        for (int i = 0; i < chunksThisTick; i++) {
            Entry entry = job.pending.pollFirst();
            if (entry == null) break;
            // Load only the chunks being handled this tick instead of force-loading the whole city.
            level.getChunk(entry.cx, entry.cz);
            long key = ChunkPos.asLong(entry.cx, entry.cz);
            try {
                switch (job.phase) {
                    case CLEAR -> job.removed += AtlantisCityReconciler.clearForRebuild(
                            level, entry.cx, entry.cz, job.supports);
                    case PLACE -> {
                        if (!job.failedChunks.contains(key)) job.record(process(level, entry, true));
                    }
                    case RESTORE -> {
                        if (!job.failedChunks.contains(key))
                            job.restored += AtlantisCityReconciler.restoreSpaces(
                                    level, entry.cx, entry.cz, job.supports);
                    }
                    case ATTACHMENTS -> {
                        if (!job.failedChunks.contains(key)) {
                            ResourceLocation structureId = ResourceLocation.fromNamespaceAndPath(NS, "atlantis_city/chunk_" + entry.cx + "_" + entry.cz);
                            StructureTemplate template = level.getStructureManager().get(structureId).orElseThrow();
                            BlockPos origin = new BlockPos(entry.cx * 16, 201, entry.cz * 16);
                            var settings = new StructurePlaceSettings().setIgnoreEntities(true).setKnownShape(true);
                            for (var info : template.filterBlocks(origin, settings, com.mustangdoc.sgjpatch.doors.AtlantisDoorRegistry.PANEL)) {
                                if (info.state().canSurvive(level, info.pos()))
                                    level.setBlock(info.pos(), info.state(), 18);
                            }
                        }
                    }
                }
            } catch (Exception error) {
                job.failedChunks.add(key);
                job.failed++;
                System.err.println("[Atlantis DHD] City " + job.phase + " failed for "
                        + entry.cx + "," + entry.cz + ": " + error);
            }
        }
        if (job.finished() >= job.nextReport && !job.pending.isEmpty()) {
            job.nextReport += 250;
            job.source.sendSuccess(() -> Component.literal(job.progress()), false);
        }
        if (!job.pending.isEmpty()) return;
        if (job.phase != ManualPhase.RESTORE) {
            job.nextPhase();
            job.source.sendSuccess(() -> Component.literal(job.progress()), false);
            return;
        }
        synchronized (PENDING) { MANUAL.remove(level); }
        com.mustangdoc.sgjpatch.transporter.TransporterCityUpgrade.refreshBundledStations(level);
        int doorsRepaired = AtlantisCityDoorRepair.restoreLoaded(level);
        boolean gateRegistered = ensureCityGateRegistered(level, true, job.gateAddress);
        boolean linked = false;
        if (gateRegistered) {
            level.getChunk(CITY_DHD);
            linked = repairCityDhdLink(level);
        }
        String gateStatus = !gateRegistered ? "registration could not be verified; see the server log."
                : linked ? "registered; city table link verified."
                : "registered; city table link could not be verified. See the server log.";
        job.source.sendSuccess(() -> Component.literal("Atlantis city installation finished. "
                + job.placed + " chunks placed, " + job.present + " already present, "
                + job.occupied + " protected because they contained other builds, and "
                + job.removed + " city blocks cleared and " + job.restored + " air/water spaces restored, "
                + (job.missing + job.failed) + " failed, " + doorsRepaired
                + " door and control blocks restored. Bundled Pegasus Stargate: "
                + gateStatus), true);
    }

    private static boolean ensureCityGateRegistered(ServerLevel level, boolean loadChunks, Address.Immutable previousAddress) {
        int baseCx = CITY_GATE.getX() >> 4;
        int baseCz = CITY_GATE.getZ() >> 4;
        // The Pegasus ring crosses the southern chunk boundary.
        if (loadChunks) {
            level.getChunk(baseCx, baseCz);
            level.getChunk(baseCx, baseCz + 1);
        } else if (!level.getChunkSource().hasChunk(baseCx, baseCz)
                || !level.getChunkSource().hasChunk(baseCx, baseCz + 1)) {
            return false;
        }
        if (!(level.getBlockEntity(CITY_GATE) instanceof PegasusStargateEntity gate)) {
            if (loadChunks) warnCityLink(level, "cannot register: bundled Pegasus gate block entity is missing");
            return false;
        }
        var network = StargateNetwork.get(level);
        var currentAddress = gate.get9ChevronAddress();
        var address = previousAddress != null ? previousAddress
                : isGateAddress(currentAddress) ? currentAddress : BUNDLED_GATE_ADDRESS;
        var blockEntityList = BlockEntityList.get(level);
        var saved = blockEntityList.getStargate(address);
        if (saved != null && !isCityGateAdapter(level, saved)) {
            warnCityLink(level, "refusing to register: the gate address already belongs to a different gate");
            return false;
        }
        for (var entry : List.copyOf(network.getStargatesInDimension(LANTEA))) {
            if (!isCityGateAdapter(level, entry)) continue;
            var owner = blockEntityList.getStargate(entry.get9ChevronAddress());
            // Removing a network entry also removes its address from the block entity list.
            if (entry != saved && (owner == null || owner == entry)) network.removeStargate(entry);
        }
        if (!address.equals(currentAddress)) gate.set9ChevronAddress(address);
        if (saved == null) saved = blockEntityList.addStargate(gate);
        if (!isCityGateAdapter(level, saved) || blockEntityList.getStargate(address) != saved) {
            warnCityLink(level, "could not register the live Pegasus gate");
            return false;
        }
        var registered = saved;
        network.addStargate(registered);
        if (network.getStargatesInDimension(LANTEA).stream().noneMatch(entry -> entry == registered)) {
            warnCityLink(level, "the bundled Pegasus registry entry is missing from the Lantea network at " + address);
            return false;
        }
        GATE_REGISTERED.add(level);
        return true;
    }

    private static boolean isGateAddress(Address.Immutable address) {
        return address != null && address.getType() == Address.Type.ADDRESS_9_CHEVRON;
    }

    private static boolean isCityGateAdapter(ServerLevel level, Object candidate) {
        return candidate instanceof PegasusBlockEntityStargate adapter
                && CITY_GATE.equals(adapter.getBlockPos())
                && level.dimension().equals(adapter.getDimension());
    }

    private static boolean repairCityDhdLink(ServerLevel level) {
        if (!level.hasChunkAt(CITY_GATE)
                || !(level.getBlockEntity(CITY_GATE) instanceof PegasusStargateEntity gate)) return false;
        var address = gate.get9ChevronAddress();
        if (address == null) return false;
        var saved = BlockEntityList.get(level).getStargate(address);
        if (!(saved instanceof PegasusBlockEntityStargate adapter) || !isCityGateAdapter(level, saved)) {
            warnCityLink(level, "no matching saved Pegasus adapter for " + address);
            return false;
        }
        if (gate.getStargate() != adapter) {
            // SGJourney caches failed lookups too. Registration alone does not invalidate that cache.
            ((CityStargateCacheAccessor) gate).sgjpatch$markStargateCacheDirty(true);
        }
        if (gate.getStargate() != adapter) {
            warnCityLink(level, "the live Pegasus gate could not resolve its saved adapter");
            return false;
        }
        if (adapter.getStargateBlockEntity() != gate) {
            adapter.invalidateCachedStargateEntity();
            if (adapter.getStargateBlockEntity() != gate) {
                warnCityLink(level, "saved adapter does not resolve the live gate");
                return false;
            }
        }
        var initialized = DHD_CALLBACKS.get(gate);
        if (initialized == null || initialized.get() != adapter) {
            gate.setupAutoCache(adapter.controllerCache());
            DHD_CALLBACKS.put(gate, new WeakReference<>(adapter));
        }
        BlockPos cityDhd = installedCityDhd(level);
        if (!level.hasChunkAt(cityDhd)) return false;
        if (!(level.getBlockEntity(cityDhd) instanceof AtlantisDHDEntity dhd)
                || !(dhd.getDHD() instanceof AtlantisBlockEntityDHD table)) {
            warnCityLink(level, "bundled table or its DHD adapter is missing");
            return false;
        }
        var linkedGate = table.receiverCache().getCached();
        var linkedDhd = adapter.controllerCache().getCached();
        if (linkedGate == adapter && linkedDhd == table) {
            if (dhd.symbolInfo().symbols() == null || dhd.symbolInfo().pointOfOrigin() == null) {
                dhd.setSymbolsFromStargate();
                dhd.updateClient();
            }
        } else if (CityDhdLinkPolicy.canReconnect(linkedGate, linkedDhd, adapter, table,
                cityDhd.distSqr(CITY_GATE) <= table.getMaxConnectionDistanceSqr(),
                gate.isConnected() || gate.isDialing())) {
            table.receiverCache().setTwoWays(adapter);
            dhd.setSymbolsFromStargate();
            dhd.setChanged();
            gate.setChanged();
            dhd.updateClient();
            if (table.receiverCache().getCached() != adapter || adapter.controllerCache().getCached() != table) {
                warnCityLink(level, "the city table and Pegasus gate did not form a two-way link");
                return false;
            }
            LOGGER.info("Restored the bundled Atlantis DHD link at {}", cityDhd);
        } else {
            warnCityLink(level, "recovery skipped: table link=" + (linkedGate == null ? "none" : linkedGate.get9ChevronAddress())
                    + ", gate linked to another DHD=" + (linkedDhd != null && linkedDhd != table)
                    + ", table range=" + table.getMaxConnectionDistance()
                    + ", gate busy=" + (gate.isConnected() || gate.isDialing()));
            return false;
        }
        if (dhd.symbolInfo().symbols() == null || dhd.symbolInfo().pointOfOrigin() == null) {
            warnCityLink(level, "table is linked but symbol data is incomplete");
            return false;
        }
        LINK_WARNINGS.remove(level);
        return true;
    }

    private static BlockPos installedCityDhd(ServerLevel level) {
        return level.hasChunkAt(CITY_DHD) && level.getBlockEntity(CITY_DHD) instanceof AtlantisDHDEntity
                ? CITY_DHD : LEGACY_CITY_DHD;
    }

    private static void warnCityLink(ServerLevel level, String reason) {
        if (!reason.equals(LINK_WARNINGS.put(level, reason)))
            LOGGER.warn("Atlantis city DHD at {}: {}", CITY_DHD, reason);
    }

    private static final StructureProcessor PRESERVE_EXISTING_CITY_GATE = new StructureProcessor() {
        @Override
        public StructureTemplate.StructureBlockInfo processBlock(LevelReader world, BlockPos offset,
                BlockPos origin, StructureTemplate.StructureBlockInfo original,
                StructureTemplate.StructureBlockInfo placed, StructurePlaceSettings settings) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(placed.state().getBlock());
            if (CITY_GATE.equals(placed.pos()) || id != null
                    && "sgjourney".equals(id.getNamespace())
                    && id.getPath().contains("stargate")
                    && world.getBlockState(placed.pos()).getBlock() == placed.state().getBlock())
                return null;
            return placed;
        }

        @Override
        protected StructureProcessorType<?> getType() {
            return StructureProcessorType.BLOCK_IGNORE;
        }
    };

    private static boolean isAmbientTerrain(BlockPos pos, String id) {
        return switch (id) {
            case "minecraft:water", "minecraft:stone", "minecraft:deepslate",
                 "minecraft:gravel", "minecraft:dirt",
                 "minecraft:clay", "minecraft:sand", "minecraft:seagrass",
                 "minecraft:tall_seagrass", "minecraft:kelp", "minecraft:kelp_plant",
                 "minecraft:snow_block", "minecraft:snow", "minecraft:bubble_column" -> true;
            default -> false;
        };
    }

    private static void installHatches(ServerLevel level) {
        AtlantisCitySavedData data = AtlantisCitySavedData.get(level);
        if (data.hatchesInstalled()) return;
        BlockPos gate = CITY_GATE;
        if (!level.getChunkSource().hasChunk(gate.getX() >> 4, gate.getZ() >> 4)
                || !"sgjourney:pegasus_stargate".equals(String.valueOf(
                    BuiltInRegistries.BLOCK.getKey(level.getBlockState(gate).getBlock())))) return;
        BlockPos table = new BlockPos(-3898, 418, 5873);
        BlockPos previousTable = new BlockPos(-3905, 418, 5868);
        BlockPos oldTable = new BlockPos(-3903, 418, 5873);
        if (!level.getChunkSource().hasChunk(table.getX() >> 4, table.getZ() >> 4)
                || !level.getChunkSource().hasChunk(oldTable.getX() >> 4, oldTable.getZ() >> 4)
                || !data.complete(table.getX() >> 4, table.getZ() >> 4)) return;
        for (int door = 0; door < 2; door++)
            for (BlockPos pos : AtlantisHatchService.positions(door))
                if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                        || !data.complete(pos.getX() >> 4, pos.getZ() >> 4)) return;
        boolean placedDoors = true;
        for (int door = 0; door < 2; door++)
            for (BlockPos pos : AtlantisHatchService.positions(door))
                if (!level.getBlockState(pos).is(AtlantisHatchRegistry.PANEL)) placedDoors = false;
        if (level.getBlockState(table).is(AtlantisHatchRegistry.TABLE) && placedDoors) {
            // The current bundle already contains the player's table, panels and motor NBT.
            data.markHatchesInstalled();
            return;
        }
        if (!level.getBlockState(table).is(AtlantisHatchRegistry.TABLE)) {
            if (level.hasChunkAt(previousTable) && level.getBlockState(previousTable).is(AtlantisHatchRegistry.TABLE)) table = previousTable;
            else if (level.getBlockState(oldTable).is(AtlantisHatchRegistry.TABLE)) table = oldTable;
        }
        var state = level.getBlockState(table);
        if (state.isAir()) {
            level.setBlock(table, AtlantisHatchRegistry.TABLE.defaultBlockState()
                    .setValue(AtlantisHatchTableBlock.FACING, net.minecraft.core.Direction.SOUTH), 3);
        } else if (!state.is(AtlantisHatchRegistry.TABLE)) {
            return;
        }
        for (int door = 0; door < 2; door++) {
            if (!AtlantisHatchService.setOpen(level, door, false, null)) return;
        }
        data.markHatchesInstalled();
    }

    private static void repairBundledDhdFacing(ServerLevel level) {
        // Older NeoForge bundles omitted this table's facing and placed it north.
        // The Forge source city's table faces west. Correct the placed block too,
        // so an existing city does not require a full reinstall for this fix.
        BlockPos pos = LEGACY_CITY_DHD;
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || !AtlantisCitySavedData.get(level).complete(pos.getX() >> 4, pos.getZ() >> 4)) return;
        var state = level.getBlockState(pos);
        if (state.is(com.mustangdoc.sgjpatch.init.SGJPatchBlocks.ATLANTIS_DHD.get())
                && state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                    == net.minecraft.core.Direction.NORTH)
            level.setBlock(pos, state.setValue(
                    net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING,
                    net.minecraft.core.Direction.WEST), 3);
    }

    private static synchronized Map<Long, Entry> entries(ServerLevel level) {
        if (manifest != null) return manifest;
        Map<Long, Entry> loaded = new HashMap<>();
        boolean readSuccessfully = false;
        try (var stream = level.getServer().getResourceManager().open(MANIFEST);
             var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] parts = line.split(" ");
                int cx = Integer.parseInt(parts[0]);
                int cz = Integer.parseInt(parts[1]);
                List<Sample> samples = new ArrayList<>();
                for (int i = 3; i < parts.length; i++) {
                    String[] fields = parts[i].split(",", 4);
                    samples.add(new Sample(new BlockPos(Integer.parseInt(fields[0]),
                            Integer.parseInt(fields[1]), Integer.parseInt(fields[2])), fields[3]));
                }
                loaded.put(ChunkPos.asLong(cx, cz), new Entry(cx, cz, samples));
            }
            readSuccessfully = true;
        } catch (Exception exception) {
            System.err.println("[Atlantis DHD] Could not read city bundle: " + exception);
        }
        if (readSuccessfully) manifest = loaded;
        return loaded;
    }

    private record Sample(BlockPos pos, String blockId) {}
    private record Entry(int cx, int cz, List<Sample> samples) {}
    private enum ProcessResult { COMPLETE, PLACED, PRESENT, OCCUPIED, MISSING, FAILED }
    private enum ManualPhase { CLEAR, PLACE, ATTACHMENTS, RESTORE }

    private static final class ManualInstall {
        final List<Entry> entries;
        Deque<Entry> pending;
        final int total;
        final CommandSourceStack source;
        final Set<BlockPos> supports;
        final Address.Immutable gateAddress;
        final Set<Long> failedChunks = new HashSet<>();
        ManualPhase phase = ManualPhase.CLEAR;
        int placed, present, occupied, missing, failed, removed, restored;
        int nextReport = 250;
        ManualInstall(List<Entry> entries, CommandSourceStack source, Set<BlockPos> supports, Address.Immutable gateAddress) {
            this.entries = entries;
            this.pending = new ArrayDeque<>(entries);
            this.total = entries.size();
            this.source = source;
            this.supports = Set.copyOf(supports);
            this.gateAddress = gateAddress;
        }
        void nextPhase() {
            phase = phase == ManualPhase.CLEAR ? ManualPhase.PLACE
                    : phase == ManualPhase.PLACE ? ManualPhase.ATTACHMENTS : ManualPhase.RESTORE;
            pending = new ArrayDeque<>(entries);
            nextReport = 250;
        }
        int finished() { return total - pending.size(); }
        void record(ProcessResult result) {
            switch (result) {
                case PLACED -> placed++;
                case PRESENT, COMPLETE -> present++;
                case OCCUPIED -> occupied++;
                case MISSING -> missing++;
                case FAILED -> failed++;
            }
        }
        String progress() {
            return "Atlantis city " + phase.toString().toLowerCase(java.util.Locale.ROOT)
                    + ": " + finished() + "/" + total + " chunks checked; "
                    + placed + " placed, " + present + " already present, " + occupied + " protected, "
                    + removed + " city blocks cleared, " + restored + " air/water spaces restored, " + (missing + failed) + " failed.";
        }
    }
}

