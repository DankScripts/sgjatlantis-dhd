package com.mustangdoc.sgjpatch.compat;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;

/** Optional jumper presentation settings, owned by the server choosing the DHD screen. */
@Mod.EventBusSubscriber(modid = "sgjadditions_capacity_patch", bus = Mod.EventBusSubscriber.Bus.MOD)
public final class LanteaJumperConfig {
    private static final ForgeConfigSpec SPEC;
    private static final ForgeConfigSpec.BooleanValue USE_ATLANTIS_FOR_LANTEA;

    static {
        var builder = new ForgeConfigSpec.Builder();
        USE_ATLANTIS_FOR_LANTEA = builder
                .comment("Use the Atlantis-style puddle jumper dialer for native Lantea gates as well.",
                        "false: native Lantea gates keep Lantea's original dialer (default).",
                        "true: use the Atlantis dialer while retaining Lantea's addresses and gate behavior.",
                        "SGJourney gates always use the Atlantis dialer. Lantea remains optional.",
                        "On multiplayer servers this setting is chosen by the server.")
                .define("useAtlantisDialerForLanteaGates", false);
        SPEC = builder.build();
    }

    private LanteaJumperConfig() {}

    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "sgjatlantis-jumper-common.toml");
        com.mustangdoc.sgjpatch.city.AtlantisLightingConfig.register();
    }

    public static boolean useAtlantisForLantea() {
        return USE_ATLANTIS_FOR_LANTEA.get();
    }
}
