package com.mustangdoc.sgjpatch.zpm;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TripleZpmHubClient {
    private TripleZpmHubClient() {}
    @SubscribeEvent public static void screens(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(TripleZpmHubRegistration.MENU, TripleZpmHubScreen::new));
    }
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(TripleZpmHubRegistration.BLOCK_ENTITY, TripleZpmHubRenderer::new);
    }
}
