package com.mustangdoc.sgjpatch.client.sound;

import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;

public final class PegasusShieldHumController {
   private static final Map<BlockPos, PegasusShieldHumController.PegasusShieldHumSound> ACTIVE_SOUNDS = new HashMap<>();
   private static int clientTick;

   private PegasusShieldHumController() {
   }

   public static void sync(PegasusStargateEntity stargate, boolean shouldHum) {
      if (stargate != null) {
         BlockPos pos = stargate.getBlockPos().immutable();
         if (!shouldHum) {
            stop(pos);
         } else {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.level != null && minecraft.player != null && stargate.getLevel() == minecraft.level) {
               PegasusShieldHumController.PegasusShieldHumSound sound = ACTIVE_SOUNDS.get(pos);
               if (sound == null || sound.isStopped()) {
                  sound = new PegasusShieldHumController.PegasusShieldHumSound(stargate);
                  ACTIVE_SOUNDS.put(pos, sound);
                  minecraft.getSoundManager().play(sound);
               }

               sound.refresh(clientTick, stargate);
            } else {
               stop(pos);
            }
         }
      }
   }

   public static void tickClient() {
      clientTick++;
      Minecraft minecraft = Minecraft.getInstance();
      if (minecraft != null && minecraft.level != null) {
         Iterator<Entry<BlockPos, PegasusShieldHumController.PegasusShieldHumSound>> iterator = ACTIVE_SOUNDS.entrySet().iterator();

         while (iterator.hasNext()) {
            PegasusShieldHumController.PegasusShieldHumSound sound = iterator.next().getValue();
            if (sound.isStopped() || sound.isStale(clientTick) || sound.getLevel() != minecraft.level) {
               sound.requestStop();
               iterator.remove();
            }
         }
      } else {
         stopAll();
      }
   }

   private static void stop(BlockPos pos) {
      PegasusShieldHumController.PegasusShieldHumSound sound = ACTIVE_SOUNDS.remove(pos);
      if (sound != null) {
         sound.requestStop();
      }
   }

   private static void stopAll() {
      for (PegasusShieldHumController.PegasusShieldHumSound sound : ACTIVE_SOUNDS.values()) {
         sound.requestStop();
      }

      ACTIVE_SOUNDS.clear();
   }

   private static final class PegasusShieldHumSound extends AbstractTickableSoundInstance {
      private static final float VOLUME = 4.0F;
      private static final int STALE_TICKS = 2;
      private int lastRefreshTick;
      private Level level;

      private PegasusShieldHumSound(PegasusStargateEntity stargate) {
         super(SGJPatchSounds.PEGASUS_SHIELD_HUMMING.get(), SoundSource.BLOCKS, RandomSource.create());
         this.looping = true;
         this.delay = 0;
         this.volume = 4.0F;
         this.pitch = 1.0F;
         this.relative = false;
         this.attenuation = Attenuation.LINEAR;
         this.refresh(0, stargate);
      }

      private void refresh(int currentTick, PegasusStargateEntity stargate) {
         BlockPos pos = stargate.getBlockPos();
         this.x = (double)pos.getX() + 0.5;
         this.y = (double)pos.getY() + 0.5;
         this.z = (double)pos.getZ() + 0.5;
         this.level = stargate.getLevel();
         this.lastRefreshTick = currentTick;
      }

      private boolean isStale(int currentTick) {
         return currentTick - this.lastRefreshTick > 2;
      }

      private void requestStop() {
         this.stop();
      }

      private Level getLevel() {
         return this.level;
      }

      @Override
      public void tick() {
         if (this.isStale(PegasusShieldHumController.clientTick)) {
            this.stop();
         }
      }
   }
}
