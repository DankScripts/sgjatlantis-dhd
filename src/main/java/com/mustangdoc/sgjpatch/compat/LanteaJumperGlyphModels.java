package com.mustangdoc.sgjpatch.compat;

import java.util.Arrays;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Registers the optional Lantea jumper's individually illuminated physical keys. */
@EventBusSubscriber(modid = "sgjatlantis_dhd", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class LanteaJumperGlyphModels {
    private static final String MOD = "sgjadditions_capacity_patch";
    public static final ResourceLocation[] GLYPHS = new ResourceLocation[36];

    static {
        for (int i = 0; i < GLYPHS.length; i++) {
            GLYPHS[i] = ResourceLocation.fromNamespaceAndPath(MOD,
                    "entity/puddle_jumper/dhd_key_lit_" + String.format("%02d", i));
        }
    }

    private LanteaJumperGlyphModels() {}

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        Arrays.stream(GLYPHS).map(ModelResourceLocation::standalone).forEach(event::register);
    }
}
