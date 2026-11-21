package com.mustangdoc.sgjpatch.compat;

import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;

/** Optional jumper presentation settings, owned by the server choosing the DHD screen. */
public final class LanteaJumperConfig {
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue USE_ATLANTIS_FOR_LANTEA;

    static {
        var builder = new ModConfigSpec.Builder();
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

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "sgjatlantis-jumper-common.toml");
    }

    public static boolean useAtlantisForLantea() {
        return USE_ATLANTIS_FOR_LANTEA.get();
    }
}
