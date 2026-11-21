package com.mustangdoc.sgjpatch.standalone;

import com.mustangdoc.sgjpatch.init.SGJPatchBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.povstalec.sgjourney.api.dhd.DHD;
import net.povstalec.sgjourney.api.stargate.DHDControlledStargate;
import net.povstalec.sgjourney.common.misc.AutoCache;
import net.povstalec.sgjourney.common.config.SyncedConfig;
import net.povstalec.sgjourney.common.block_entities.StructureGenEntity;
import net.povstalec.sgjourney.common.blockstates.ShieldingState;
import net.povstalec.sgjourney.common.block_entities.dhd.CrystalDHDEntity;
import net.povstalec.sgjourney.common.block_entities.dhd.AbstractDHDEntity;
import net.povstalec.sgjourney.common.block_entities.stargate.MilkyWayStargateEntity;
import net.povstalec.sgjourney.common.block_entities.tech.CableBlockEntity;
import net.povstalec.sgjourney.common.config.CommonDHDConfig;
import net.povstalec.sgjourney.common.config.CommonTechConfig;
import net.povstalec.sgjourney.common.init.ItemInit;
import net.povstalec.sgjourney.common.init.SoundInit;
import net.povstalec.sgjourney.common.items.crystals.ControlCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.EnergyCrystalItem;
import net.povstalec.sgjourney.common.items.crystals.TransferCrystalItem;
import net.povstalec.sgjourney.common.items.energy_cores.FusionCoreItem;
import net.povstalec.sgjourney.common.data.ConduitNetworks;
import net.povstalec.sgjourney.common.sgjourney.PointOfOrigin;
import net.povstalec.sgjourney.common.sgjourney.Address;
import net.povstalec.sgjourney.common.sgjourney.Symbols;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;

/**
 * Atlantis DHD backed by SGJourney's current Pegasus DHD behavior.
 * The physical model and UI remain supplied by this mod; energy/crystal/symbol behavior stays native to SGJourney.
 */
public class AtlantisDHDEntity extends CrystalDHDEntity<AtlantisBlockEntityDHD> {

    public AtlantisDHDEntity(BlockPos pos, BlockState state) {
        super(SGJPatchBlockEntities.ATLANTIS_DHD.get(), pos, state);
    }

    @Override
    protected AtlantisBlockEntityDHD fetchDHD() {
        return new AtlantisBlockEntityDHD(this);
    }

    @Override
    public void setupAutoCache(AutoCache.Receiver<DHD, DHDControlledStargate> cache) {
        super.setupAutoCache(cache);
        if (level instanceof ServerLevel serverLevel)
            cache.setFetch(() -> findNativeGate(serverLevel, cache.controller));
    }

    private DHDControlledStargate findNativeGate(ServerLevel level, DHD table) {
        var capability = DHDControlledStargate.DHD_CONTROLLED_STARGATE_CAPABILITY_BLOCK;
        double range = getMaxConnectionDistance();
        double rangeSqr = getMaxConnectionDistanceSqr();
        BlockPos savedPos = linkedGatePosition();
        if (savedPos != null && worldPosition.distSqr(savedPos) <= rangeSqr && level.hasChunkAt(savedPos)) {
            var savedGate = level.getCapability(capability, savedPos, null);
            if (savedGate != null && nativeLinkPriority(table, savedGate.controllerCache().getCached()) > 0)
                return savedGate;
        }

        DHDControlledStargate best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        int bestPriority = 0;
        for (int x = SectionPos.blockToSectionCoord(worldPosition.getX() - range);
                x <= SectionPos.blockToSectionCoord(worldPosition.getX() + range); x++) {
            for (int z = SectionPos.blockToSectionCoord(worldPosition.getZ() - range);
                    z <= SectionPos.blockToSectionCoord(worldPosition.getZ() + range); z++) {
                var chunk = level.getChunkSource().getChunkNow(x, z);
                if (chunk == null) continue;
                for (BlockPos pos : chunk.getBlockEntitiesPos()) {
                    double distance = worldPosition.distSqr(pos);
                    if (distance > rangeSqr) continue;
                    var gate = level.getCapability(capability, pos, null);
                    if (gate == null) continue;
                    int priority = nativeLinkPriority(table, gate.controllerCache().getCached());
                    if (priority > 0 && (priority > bestPriority
                            || priority == bestPriority && distance < bestDistance)) {
                        best = gate;
                        bestDistance = distance;
                        bestPriority = priority;
                    }
                }
            }
        }
        return best;
    }

    static int nativeLinkPriority(Object table, Object gateOwner) {
        // A gate may still point to this table after the table loses its half of the link.
        return gateOwner == table ? 2 : gateOwner == null ? 1 : 0;
    }

    @Override
    public void updateDHD(DHDControlledStargate gate, Address address, boolean shouldEngageCenterButton) {
        if (gate != null && level instanceof ServerLevel serverLevel) {
            var origin = gate.pointOfOrigin();
            var symbols = gate.symbols();
            if (PointOfOrigin.isValid(serverLevel.getServer(), origin))
                symbolInfo().setPointOfOrigin(origin);
            if (Symbols.isValid(serverLevel.getServer(), symbols))
                symbolInfo().setSymbols(symbols);
        }
        super.updateDHD(gate, address, shouldEngageCenterButton);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        com.mustangdoc.sgjpatch.compat.AtlantisDHDConcealedCableBootstrapEvents.register(this);
        if (this.level == null || this.level.isClientSide())
            return;

        if (generationStep == Step.GENERATED) {
            if (AtlantisDHDApi.connectedStargate(this) != null)
                setSymbolsFromStargate();
            else
                setLocalSymbols();
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AtlantisDHDEntity dhd) {
        AbstractDHDEntity.tick(level, pos, state, dhd);
        if (!level.isClientSide()) {
            if (level.getGameTime() % 20 == 0 && dhd.getStargateOpenTime() > 0
                    && dhd.getAddress().getLength() >= 6) {
                String address = dhd.getAddress().toString();
                if (!address.isBlank() && !RecentAddressHistory.isLast(dhd.getPersistentData(), address))
                    dhd.recordRecentAddress(address, pos, state);
            }
            if (dhd.getDHD() instanceof AtlantisBlockEntityDHD table)
                table.tickPendingDial();
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        RecentAddressHistory.copyTo(getPersistentData(), tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        RecentAddressHistory.copyTo(tag, getPersistentData());
    }

    @Override public void setRemoved() {
        com.mustangdoc.sgjpatch.compat.AtlantisDHDConcealedCableBootstrapEvents.unregister(this);
        super.setRemoved();
    }
    /**
     * Minimum Atlantis operating set. Communication crystals remain optional,
     * while every other Pegasus crystal installed in the native nine-slot
     * handler retains SGJourney's own behavior.
     */
    @Override
    public void pressButton(int symbol) {
        if (symbol < 0 || hasRequiredDialingCrystals()) {
            super.pressButton(symbol);
        } else if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            for (var player : serverLevel.players()) {
                if (player.blockPosition().distSqr(worldPosition) <= 64)
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "Atlantis DHD needs a large control crystal, two energy crystals, and a transfer crystal."), true);
            }
        }
    }

    @Override
    public net.povstalec.sgjourney.common.sgjourney.StargateInfo.FeedbackMessage encodeSymbol(int symbol) {
        if (!hasRequiredDialingCrystals())
            return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
        if (AtlantisDHDApi.connectedStargate(this) == null && level != null
                && com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.nearest(level, worldPosition) != null) {
            var next = new net.povstalec.sgjourney.common.sgjourney.Address.Mutable(getAddress());
            if (!next.addSymbol(symbol))
                return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
            updateDHD(AtlantisDHDApi.connectedStargate(this), next, false);
            return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.SYMBOL_ENCODED.withInfo();
        }
        return super.encodeSymbol(symbol);
    }

    @Override
    public net.povstalec.sgjourney.common.sgjourney.StargateInfo.FeedbackMessage engageStargate() {
        return engageStargate(null);
    }

    /** The menu action supplies the player for the SG Mobs bridge. */
    public net.povstalec.sgjourney.common.sgjourney.StargateInfo.FeedbackMessage engageStargate(
            net.minecraft.world.entity.player.Player initiatingPlayer) {
        var linkedStargate = AtlantisDHDApi.connectedStargate(this);
        var mobsGate = level != null && linkedStargate == null
                ? com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.nearest(level, worldPosition) : null;
        if (mobsGate != null && level instanceof net.minecraft.server.level.ServerLevel
                && getAddress().getLength() > 0
                && getAddress().getLength() < 6) {
            updateDHD(linkedStargate, new net.povstalec.sgjourney.common.sgjourney.Address.Mutable(), false);
            return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.INCOMPLETE_ADDRESS.withInfo();
        }
        if (mobsGate != null && level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            net.minecraft.server.level.ServerPlayer operator =
                    initiatingPlayer instanceof net.minecraft.server.level.ServerPlayer serverPlayer
                            ? serverPlayer : null;
            if (operator == null) {
                for (var player : serverLevel.players()) {
                    if (player.containerMenu instanceof AtlantisDHDMenu menu && menu.getDHD() == this
                            && player.blockPosition().distSqr(worldPosition) <= 64) {
                        operator = player;
                        break;
                    }
                }
            }
            if (operator == null || operator.blockPosition().distSqr(worldPosition) > 64)
                return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
            Boolean busy = com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.busy(mobsGate);
            if (busy == null) {
                operator.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Could not read the SG Mobs gate state; check the server log."), true);
                return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
            }
            if (busy) {
                if (!operator.mayBuild() || !serverLevel.mayInteract(operator, mobsGate.getBlockPos())) {
                    operator.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "You do not have permission to close this SG Mobs gate."), true);
                    return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
                }
                if (!com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.close(mobsGate, operator)) {
                    operator.displayClientMessage(net.minecraft.network.chat.Component.literal(
                            "Could not close the SG Mobs gate; check the server log."), true);
                    return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
                }
                updateDHD(linkedStargate, new net.povstalec.sgjourney.common.sgjourney.Address.Mutable(), false);
                return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.NONE.withInfo();
            }
            if (!hasRequiredDialingCrystals()) {
                operator.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "Atlantis DHD needs a large control crystal, two energy crystals, and a transfer crystal."), true);
                return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
            }
            int[] entered = getAddress().toArray();
            int[] complete = entered.length >= 6 && entered.length <= 8
                    && entered[entered.length - 1] != 0
                    ? java.util.Arrays.copyOf(entered, entered.length + 1) : entered;
            if (complete.length > 0 && entered.length < complete.length) complete[complete.length - 1] = 0;
            int dialStatus = com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.dialStatus(
                    mobsGate, operator, complete);
            if (dialStatus == 4 || dialStatus == 18) {
                recordSgmobsAddress(complete);
                updateDHD(linkedStargate, new net.povstalec.sgjourney.common.sgjourney.Address.Mutable(), false);
            } else {
                operator.displayClientMessage(net.minecraft.network.chat.Component.literal(
                        "SG Mobs gate rejected the address (status " + dialStatus
                                + "). Dialer cleared; check the destination and gate access."), true);
                updateDHD(linkedStargate, new net.povstalec.sgjourney.common.sgjourney.Address.Mutable(), false);
                return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
            }
            return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.NONE.withInfo();
        }
        if (hasRequiredDialingCrystals())
            return super.engageStargate();
        return net.povstalec.sgjourney.common.sgjourney.StargateInfo.Feedback.UNKNOWN_ERROR.withInfo();
    }

    private void recordSgmobsAddress(int[] complete) {
        recordRecentAddress(new Address.Mutable(complete).toString(), worldPosition, getBlockState());
    }

    private void recordRecentAddress(String address, BlockPos pos, BlockState state) {
        RecentAddressHistory.record(getPersistentData(), address);
        setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    public boolean hasRequiredDialingCrystals() {
        int largeControlCrystals = 0;
        int energyCrystals = 0;
        int transferCrystals = 0;

        for (int slot = 0; slot < crystalHandler.getSlots(); ++slot) {
            Item item = crystalHandler.getStackInSlot(slot).getItem();
            if (item instanceof ControlCrystalItem crystal && crystal.isLarge())
                ++largeControlCrystals;
            else if (item instanceof EnergyCrystalItem)
                ++energyCrystals;
            else if (item instanceof TransferCrystalItem)
                ++transferCrystals;
        }

        return largeControlCrystals >= 1
                && energyCrystals >= 2
                && transferCrystals >= 1;
    }

    /** Stock only the DHD placed by the bundled city, leaving normal player tables empty. */
    public void installBundledDialingCrystals() {
        Item control = ItemInit.LARGE_CONTROL_CRYSTAL.get();
        installBundledCrystal(0, control);
        installBundledCrystal(1, ItemInit.ADVANCED_ENERGY_CRYSTAL.get());
        installBundledCrystal(2, ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get());
        installBundledCrystal(3, ItemInit.ADVANCED_ENERGY_CRYSTAL.get());
        installBundledCrystal(6, ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get());
        installBundledCrystal(7, ItemInit.ADVANCED_TRANSFER_CRYSTAL.get());
        setChanged();
    }

    private void installBundledCrystal(int slot, Item item) {
        if (crystalHandler.getStackInSlot(slot).isEmpty())
            crystalHandler.setStackInSlot(slot, new ItemStack(item));
    }

    public boolean hasBundledControlCrystal() {
        return crystalHandler.getStackInSlot(0).getItem() == ItemInit.LARGE_CONTROL_CRYSTAL.get();
    }

    @Override
    protected long buttonPressEnergyCost() {
        return Math.max(1L, CommonDHDConfig.pegasus_dhd_button_press_energy_cost.getAsLong() / 100L);
    }

    @Override
    public long getEnergyCapacity() {
        return level != null && level.isClientSide()
                ? SyncedConfig.pegasus_dhd_energy_buffer_capacity.get()
                : CommonDHDConfig.pegasus_dhd_energy_buffer_capacity.getAsLong();
    }

    @Override
    public long getMaxEnergyReceive() {
        return CommonDHDConfig.pegasus_dhd_max_energy_receive.getAsLong();
    }

    @Override
    public long maxEnergyTransfer() {
        return this.maxEnergyTransfer < 0
                ? CommonDHDConfig.pegasus_dhd_max_energy_extract.getAsLong()
                : this.maxEnergyTransfer;
    }

    @Override
    protected SoundEvent getEnterSound() {
        return SoundInit.PEGASUS_DHD_ENTER.get();
    }

    @Override
    protected SoundEvent getPressSound() {
        return SoundInit.PEGASUS_DHD_PRESS.get();
    }

    /** Toggle the linked gate's iris or shield. */
    public void toggleConnectedShield(net.minecraft.world.entity.player.Player player) {
        if (!(level instanceof net.minecraft.server.level.ServerLevel serverLevel))
            return;

        var stargate = AtlantisDHDApi.connectedStargateEntity(this);
        if (!(stargate instanceof IrisInfo.Interface irisGate)) {
            if (com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.nearest(serverLevel, worldPosition) != null) {
                com.mustangdoc.sgjpatch.compat.SgmobsGateBridge.toggleIris(serverLevel, worldPosition, player);
                return;
            }
            com.mustangdoc.sgjpatch.compat.LanteaIrisBridge.toggleNearest(serverLevel, worldPosition);
            return;
        }

        IrisInfo irisInfo = irisGate.irisInfo();
        if (!irisInfo.hasIris())
            return;

        if (stargate instanceof MilkyWayStargateEntity) {
            var motion = irisInfo.getIrisMotion();
            boolean opening = motion.isMoving ? motion.isClosing() : irisInfo.isIrisClosed();
            if (opening ? irisInfo.aboveMinProgress() : irisInfo.belowMaxProgress()) {
                irisInfo.setIrisMotion(opening ? IrisInfo.IrisMotion.OPENING_COMPUTER
                        : IrisInfo.IrisMotion.CLOSING_COMPUTER);
                stargate.setChanged();
                serverLevel.playSound(null, stargate.getBlockPos(),
                        opening ? SoundEvents.IRON_DOOR_OPEN : SoundEvents.IRON_DOOR_CLOSE,
                        SoundSource.BLOCKS, 1.0F, 1.0F);
            } else if (motion.isMoving) {
                irisInfo.setIrisMotion(IrisInfo.IrisMotion.IDLE);
                stargate.setChanged();
            }
            stargate.updateClient();
            return;
        }

        boolean shouldClose = !irisInfo.isIrisClosed();
        if (shouldClose && stargate instanceof net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity gate
                && !com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper.canEngage(gate)) {
            sendMessageToNearbyPlayers(net.minecraft.network.chat.Component.literal("Insufficient power: Atlantis shield requires 1200 FE/t"), 5);
            return;
        }
        irisInfo.setIrisMotion(IrisInfo.IrisMotion.IDLE);
        irisInfo.setIrisProgress(shouldClose ? ShieldingState.MAX_PROGRESS : ShieldingState.OPEN.getProgress());
        stargate.setStargateState(true, shouldClose ? ShieldingState.CLOSED : ShieldingState.OPEN);
        stargate.updateClient();
        stargate.setChanged();
    }

    public void clearSymbols() {
        symbolInfo().setPointOfOrigin(null);
        symbolInfo().setSymbols(null);
    }

    public BlockPos linkedGatePosition() {
        return stargateRelativePos == null ? null : net.povstalec.sgjourney.common.misc.CoordinateHelper.Relative
                .getOffsetPos(getDirection(), getBlockPos(), stargateRelativePos);
    }

    @Override
    protected void generateEnergyCore() {
        energyItemHandler.setStackInSlot(
                0,
                FusionCoreItem.randomFusionCore(
                        CommonTechConfig.fusion_core_fuel_capacity.getAsInt() / 2,
                        CommonTechConfig.fusion_core_fuel_capacity.getAsInt()
                )
        );
    }

    @Override
    public void generateAdditional(StructureGenEntity.Step generationStep) {
        if (generationStep == StructureGenEntity.Step.SETUP) {
            if (!PointOfOrigin.isValid(level.getServer(), symbolInfo().pointOfOrigin()))
                symbolInfo().setPointOfOrigin(null);
            if (!Symbols.isValid(level.getServer(), symbolInfo().symbols()))
                symbolInfo().setSymbols(null);
        } else if (AtlantisDHDApi.connectedStargate(this) != null) {
            setSymbolsFromStargate();
        } else {
            setLocalSymbols();
        }

        crystalCache.recalculateCrystals();
    }

    @Override
    protected void generateCrystals() {
        crystalHandler.setStackInSlot(0, new ItemStack(ItemInit.LARGE_CONTROL_CRYSTAL.get()));
        crystalHandler.setStackInSlot(1, new ItemStack(ItemInit.ADVANCED_ENERGY_CRYSTAL.get()));
        crystalHandler.setStackInSlot(2, new ItemStack(ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get()));
        crystalHandler.setStackInSlot(3, new ItemStack(ItemInit.ADVANCED_ENERGY_CRYSTAL.get()));
        crystalHandler.setStackInSlot(6, new ItemStack(ItemInit.ADVANCED_COMMUNICATION_CRYSTAL.get()));
        crystalHandler.setStackInSlot(7, new ItemStack(ItemInit.ADVANCED_TRANSFER_CRYSTAL.get()));
    }
}


