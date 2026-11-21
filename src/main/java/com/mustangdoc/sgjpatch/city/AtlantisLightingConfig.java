package com.mustangdoc.sgjpatch.city;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
public final class AtlantisLightingConfig {
 private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
 private static final ModConfigSpec.IntValue HOLD = BUILDER.comment("Seconds illuminated stairs and slabs remain on after the last nearby player leaves. Returning players reset the timer.").defineInRange("inscribedLightHoldSeconds", 90, 0, 86400);
 public enum CityInstallSpeed { HIGH, MEDIUM, LOW }
 private static final ModConfigSpec.EnumValue<CityInstallSpeed> CITY_INSTALL_SPEED = BUILDER.comment(
     "Atlantis city install pace. HIGH keeps the original rate (one clear chunk or two rebuild chunks per tick).",
     "MEDIUM handles one chunk per tick; LOW handles one chunk every 4 ticks (the previous MEDIUM rate). Use LOW on servers that restart during city installation.")
     .defineEnum("cityInstallSpeed", CityInstallSpeed.MEDIUM);
 private static final ModConfigSpec SPEC = BUILDER.build();
 public static void register(ModContainer container) { container.registerConfig(ModConfig.Type.COMMON, SPEC, "sgjatlantis-config.toml"); }
 public static long holdTicks() { return HOLD.get() * 20L; }
 public static CityInstallSpeed cityInstallSpeed() { return CITY_INSTALL_SPEED.get(); }
}
