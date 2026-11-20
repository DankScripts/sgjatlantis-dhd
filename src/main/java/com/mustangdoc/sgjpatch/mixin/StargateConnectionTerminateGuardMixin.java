package com.mustangdoc.sgjpatch.mixin;

import java.util.List;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.povstalec.sgjourney.common.data.StargateNetwork;
import net.povstalec.sgjourney.common.sgjourney.StargateConnection;
import net.povstalec.sgjourney.common.sgjourney.StargateInfo.FeedbackMessage;
import net.povstalec.sgjourney.api.stargate.Stargate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(
   value = {StargateConnection.class},
   remap = false
)
public abstract class StargateConnectionTerminateGuardMixin {
   private static final Logger SGJPATCH_LOGGER = LogManager.getLogger("SGJPATCH");
   @Shadow
   protected UUID uuid;
   @Shadow
   protected Stargate dialingStargate;
   @Shadow
   protected List<Stargate> dialedStargates;

   @Shadow
   public abstract Stargate getDialedStargate();

   @Inject(
      method = {"terminate"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void sgjpatch$skipTerminateForCorruptConnection(MinecraftServer server, FeedbackMessage message, CallbackInfo ci) {
      boolean corrupt = this.dialingStargate == null || this.dialedStargates == null || this.dialedStargates.isEmpty() || this.getDialedStargate() == null;
      if (corrupt) {
         if (server != null && this.uuid != null) {
            StargateNetwork.get(server).removeConnection(this.uuid);
         }

         SGJPATCH_LOGGER.warn(
            "[SGJPATCH] Removed corrupt StargateConnection {} before terminate() null-crash (dialing={}, dialedCount={})",
            this.uuid,
            this.dialingStargate != null,
            this.dialedStargates == null ? -1 : this.dialedStargates.size()
         );
         ci.cancel();
      }
   }
}

