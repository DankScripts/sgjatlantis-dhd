package com.mustangdoc.sgjpatch.client.sound;

import com.mustangdoc.sgjpatch.init.SGJPatchSounds;
import com.mustangdoc.sgjpatch.util.PegasusShieldPowerHelper;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.povstalec.sgjourney.common.block_entities.stargate.PegasusStargateEntity;

public final class PegasusShieldHumController {
   private static final Map<PegasusStargateEntity, PegasusShieldHumController.ShieldHum> SOUNDS = new WeakHashMap<>();

   private PegasusShieldHumController() {
   }

   public static void sync(PegasusStargateEntity gate) {
      if (IrisSoundState.active(gate.irisInfo()) && !SOUNDS.containsKey(gate)) {
         PegasusShieldHumController.ShieldHum sound = new PegasusShieldHumController.ShieldHum(gate);
         SOUNDS.put(gate, sound);
         Minecraft.getInstance().getSoundManager().play(sound);
      }
   }

   private static final class ShieldHum extends AbstractTickableSoundInstance {
      private final PegasusStargateEntity gate;

      private ShieldHum(PegasusStargateEntity gate) {
         super((SoundEvent)SGJPatchSounds.HUMMING.get(), SoundSource.BLOCKS, RandomSource.create());
         this.gate = gate;
         this.looping = true;
         this.delay = 0;
         this.volume = 0.8F;
         this.x = (double)gate.getBlockPos().getX() + 0.5;
         this.y = (double)gate.getBlockPos().getY() + 0.5;
         this.z = (double)gate.getBlockPos().getZ() + 0.5;
      }

      public void tick() {
         if (this.gate.isRemoved()
            || this.gate.getLevel() != Minecraft.getInstance().level
            || !IrisSoundState.active(this.gate.irisInfo())
            || !PegasusShieldPowerHelper.hasShield(this.gate.irisInfo())) {
            this.stop();
            PegasusShieldHumController.SOUNDS.remove(this.gate);
         }
      }
   }
}
