package com.mustangdoc.sgjpatch.compat;

import java.util.Arrays;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Registers the optional Lantea jumper's 36 individually illuminated keys. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LanteaJumperGlyphModels {
    private static final String MOD = "sgjadditions_capacity_patch";
    public static final ResourceLocation[] GLYPHS = new ResourceLocation[36];

    static {
        for (int i = 0; i < GLYPHS.length; i++) {
            GLYPHS[i] = new ResourceLocation(MOD,
                    "entity/puddle_jumper/dhd_key_lit_" + String.format("%02d", i));
        }
    }

    private LanteaJumperGlyphModels() {}

    @SubscribeEvent
    public static void register(ModelEvent.RegisterAdditional event) {
        Arrays.stream(GLYPHS).forEach(event::register);
    }
}
