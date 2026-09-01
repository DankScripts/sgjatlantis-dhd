package com.mustangdoc.sgjpatch.compat.moregates;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Strict activation guard for the legacy More Gates compatibility bridge.
 *
 * <p>The CurseForge 4.3.2 artifact incorrectly reports version 4.2.0 in its
 * own mods.toml. For that reason the bridge verifies both its external filename
 * and the SHA-256 fingerprint of CurseForge file 5783142.</p>
 */
public final class MoreGates432Compatibility {
    public static final String EXACT_JAR_NAME = "moregates-4.3.2-forge-1.20.1.jar";
    public static final String EXACT_SHA256 = "d80db3800f5161edb6a0a409779a3b44d9d8a8cc20bf64c45325e480b8a6eb0a";
    public static final String CONFIG_KEY = "enableMoreGates432CompatibilityPatch";

    private static final Pattern SGJ_VERSION = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("sgjatlantis-dhd.toml");
    private static final boolean CONFIG_ENABLED = loadOrCreateConfig();
    private static Optional<Path> exactJar;
    private static Boolean currentSgjSchema;

    private MoreGates432Compatibility() {
    }

    public static boolean isEnabledAndApplicable() {
        return CONFIG_ENABLED && usesCurrentSgjSchema() && exactJar().isPresent();
    }

    public static Optional<Path> exactJar() {
        if (exactJar == null) {
            exactJar = locateAndVerifyExactJar();
        }
        return exactJar;
    }

    public static Path generatedPackRoot() {
        return FMLPaths.CONFIGDIR.get()
                .resolve("sgjatlantis-dhd-generated")
                .resolve("moregates-4.3.2-compat");
    }

    public static Path configPath() {
        return CONFIG_PATH;
    }

    private static Optional<Path> locateAndVerifyExactJar() {
        Path modsDir = FMLPaths.MODSDIR.get();
        if (!Files.isDirectory(modsDir)) {
            return Optional.empty();
        }

        try (Stream<Path> paths = Files.list(modsDir)) {
            Optional<Path> candidate = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> EXACT_JAR_NAME.equalsIgnoreCase(path.getFileName().toString()))
                    .findFirst();

            if (candidate.isEmpty()) {
                return Optional.empty();
            }

            String actualHash = sha256(candidate.get());
            if (!EXACT_SHA256.equals(actualHash)) {
                System.out.println("[SGJPATCH] More Gates compatibility not applied: "
                        + EXACT_JAR_NAME + " has an unexpected SHA-256 fingerprint.");
                return Optional.empty();
            }

            return candidate;
        } catch (IOException exception) {
            System.out.println("[SGJPATCH] Could not inspect the mods directory for More Gates 4.3.2: "
                    + exception.getMessage());
            return Optional.empty();
        }
    }

    private static boolean usesCurrentSgjSchema() {
        if (currentSgjSchema != null) {
            return currentSgjSchema;
        }

        String version = ModList.get().getModContainerById("sgjourney")
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("0.0.0");
        Matcher matcher = SGJ_VERSION.matcher(version);
        if (!matcher.find()) {
            currentSgjSchema = false;
            return false;
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = Integer.parseInt(matcher.group(3));
        currentSgjSchema = major > 0 || minor > 6 || minor == 6 && patch >= 48;
        return currentSgjSchema;
    }

    private static boolean loadOrCreateConfig() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            if (!Files.exists(CONFIG_PATH)) {
                Files.writeString(CONFIG_PATH, fullDefaults(), StandardCharsets.UTF_8);
            } else if (!containsKey(CONFIG_PATH, CONFIG_KEY)) {
                Files.writeString(CONFIG_PATH, compatibilitySetting(), StandardCharsets.UTF_8,
                        StandardOpenOption.APPEND);
            }

            for (String line : Files.readAllLines(CONFIG_PATH, StandardCharsets.UTF_8)) {
                String value = stripComment(line);
                int separator = value.indexOf('=');
                if (separator < 0 || !CONFIG_KEY.equalsIgnoreCase(value.substring(0, separator).trim())) {
                    continue;
                }

                String configured = value.substring(separator + 1).trim().toLowerCase(Locale.ROOT);
                boolean enabled = !"false".equals(configured);
                System.out.println("[SGJPATCH] More Gates 4.3.2 compatibility: "
                        + (enabled ? "ENABLED" : "DISABLED") + " | config=" + CONFIG_PATH);
                return enabled;
            }
        } catch (Throwable throwable) {
            System.out.println("[SGJPATCH] Could not read/update " + CONFIG_PATH
                    + "; enabling the More Gates 4.3.2 compatibility bridge by default. " + throwable);
        }
        return true;
    }

    private static boolean containsKey(Path path, String key) throws IOException {
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String value = stripComment(line);
            int separator = value.indexOf('=');
            if (separator >= 0 && key.equalsIgnoreCase(value.substring(0, separator).trim())) {
                return true;
            }
        }
        return false;
    }

    private static String stripComment(String line) {
        String value = line.trim();
        if (value.isEmpty() || value.startsWith("#")) {
            return "";
        }
        int comment = value.indexOf('#');
        return comment >= 0 ? value.substring(0, comment).trim() : value;
    }

    private static String fullDefaults() {
        return "# Atlantis DHD settings\n"
                + "#\n"
                + "# Leave false for normal locked gameplay. Set true only to enable F8 editing.\n"
                + "enableDhdEditorDebug = false\n\n"
                + "# SGJ Additions Atlantis DHD compatibility\n"
                + "# false (default): use this mod's upgraded Atlantis DHD.\n"
                + "# true: when SGJ Additions is installed, use its original Atlantis DHD.\n"
                + "# Restart Minecraft after changing this setting.\n"
                + "useOriginalSGJAdditionsDHD = false\n"
                + compatibilitySetting();
    }

    private static String compatibilitySetting() {
        return "\n# More Gates 4.3.2 compatibility\n"
                + "# ONLY for moregates-4.3.2-forge-1.20.1.jar (CurseForge file 5783142).\n"
                + "# true (default): enable the legacy More Gates 4.3.2 compatibility bridge.\n"
                + "# false: disable this bridge. Other More Gates versions are never patched.\n"
                + "# Restart Minecraft after changing this setting.\n"
                + CONFIG_KEY + " = true\n";
    }

    private static String sha256(Path path) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(path)) {
                byte[] buffer = new byte[64 * 1024];
                int count;
                while ((count = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, count);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable", impossible);
        }
    }
}
