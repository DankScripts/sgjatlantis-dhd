package com.mustangdoc.sgjpatch.util;

import com.mustangdoc.sgjpatch.standalone.AtlantisDHDApi;
import com.mustangdoc.sgjpatch.init.SGJPatchItems;
import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;
import net.povstalec.sgjourney.common.blockstates.ShieldingState;
import net.povstalec.sgjourney.common.capabilities.SGJourneyEnergy;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo;
import net.povstalec.sgjourney.common.sgjourney.info.IrisInfo.IrisMotion;

public final class PegasusShieldPowerHelper {
   public static final long SHIELD_FE_PER_TICK = 1200L;
   private static final Map<PegasusStargateEntity, Boolean> SHIELD_STATES = new WeakHashMap<>();

   private PegasusShieldPowerHelper() {
   }

   public static boolean hasShield(IrisInfo iris) {
      return iris.getIris().is((Item)SGJPatchItems.PEGASUS_UPGRADED_SHIELD.get());
   }

   public static boolean canEngage(PegasusStargateEntity gate) {
      return !hasShield(gate.irisInfo()) || transfer(gate, true) >= 1200L;
   }

   public static void tick(PegasusStargateEntity gate) {
      if (gate.getLevel() != null && !gate.getLevel().isClientSide()) {
         IrisInfo iris = gate.irisInfo();
         if (!hasShield(iris)) {
            SHIELD_STATES.remove(gate);
         } else {
            if (iris.isIrisClosed()) {
               if (transfer(gate, true) >= 1200L) {
                  transfer(gate, false);
               } else {
                  open(gate);
               }
            }

            boolean closed = iris.isIrisClosed();
            Boolean previous = SHIELD_STATES.put(gate, closed);
            if (previous != null && previous != closed) {
               gate.getLevel()
                  .playSound(
                     null,
                     gate.getBlockPos(),
                     closed ? (SoundEvent)SGJPatchSounds.CLOSING.get() : (SoundEvent)SGJPatchSounds.OPENING.get(),
                     SoundSource.BLOCKS,
                     1.0F,
                     1.0F
                  );
            }
         }
      }
   }

   public static void open(PegasusStargateEntity gate) {
      IrisInfo iris = gate.irisInfo();
      iris.setIrisMotion(IrisMotion.IDLE);
      iris.setIrisProgress(ShieldingState.OPEN.getProgress());
      gate.setStargateState(true, ShieldingState.OPEN);
      gate.updateClient();
      gate.setChanged();
   }

   private static long transfer(PegasusStargateEntity gate, boolean simulate) {
      SGJourneyEnergy gateEnergy = gate.getEnergyStorage();
      long supplied = gateEnergy.depleteEnergy(1200L, simulate);
      var dhd = AtlantisDHDApi.connectedDhdEntity((net.povstalec.sgjourney.common.block_entities.stargate.AbstractStargateEntity<?>) gate);
      if (supplied < 1200L && dhd != null && dhd.getEnergyStorage() != gateEnergy) {
         supplied += dhd.getEnergyStorage().depleteEnergy(1200L - supplied, simulate);
      }

      return supplied;
   }
}
