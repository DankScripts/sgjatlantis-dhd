package com.mustangdoc.sgjpatch.zpm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers;

@EventBusSubscriber(
   modid = "sgjatlantis_dhd",
   bus = Bus.MOD,
   value = {Dist.CLIENT}
)
/** Ported from SG Mobs with permission from Mike; retains SGJourney ZPM behavior. */
public final class TripleZpmHubClient {
   @SubscribeEvent
   public static void screens(RegisterMenuScreensEvent e) {
      e.register(TripleZpmHubRegistration.MENU, TripleZpmHubScreen::new);
   }

   @SubscribeEvent
   public static void renderers(RegisterRenderers e) {
      e.registerBlockEntityRenderer(TripleZpmHubRegistration.BLOCK_ENTITY, TripleZpmHubRenderer::new);
   }
}
