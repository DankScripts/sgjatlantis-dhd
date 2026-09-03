package com.mustangdoc.sgjpatch.compat.moregates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Builds the conditional compatibility pack from the user's installed JAR. */
public final class MoreGates432PackGenerator {
    public static final String PACK_ID = "sgjatlantis_dhd_moregates_432_compat";
    private static final String GENERATOR_VERSION = "4";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean ready;

    private MoreGates432PackGenerator() {
    }

    public static synchronized Path ensureReady(Path moreGatesJar) throws IOException {
        Path root = MoreGates432Compatibility.generatedPackRoot();
        if (ready || isCurrent(root)) {
            ready = true;
            return root;
        }

        Files.createDirectories(root);
        try (ZipFile zip = new ZipFile(moreGatesJar.toFile())) {
            writePackMetadata(root);
            writeServerRegistryBridges(root);
            generateCrystallizingRecipeOverrides(zip, root);
            writeClientPointOfOrigin(root);
            generateSymbolSet(zip, root,
                    "assets/moregates/textures/symbols/enchantment/enchantment.png",
                    "enchantment", 64);
            generateSymbolSet(zip, root,
                    "assets/moregates/textures/symbols/universal_custom/universal_custom.png",
                    "universal_custom", 32);
            generateModernVariantJson(zip, root);
        }

        writeString(root.resolve(".generator-version"), GENERATOR_VERSION + "\n");
        ready = true;
        System.out.println("[SGJPATCH] Prepared More Gates 4.3.2 compatibility pack at " + root);
        return root;
    }

    private static boolean isCurrent(Path root) {
        try {
            return Files.readString(root.resolve(".generator-version"), StandardCharsets.UTF_8).trim()
                    .equals(GENERATOR_VERSION)
                    && Files.isRegularFile(root.resolve("pack.mcmeta"))
                    && Files.isRegularFile(root.resolve(
                    "assets/moregates/textures/symbol/generated/enchantment_38.png"))
                    && Files.isRegularFile(root.resolve(
                    "assets/moregates/textures/symbol/generated/universal_custom_38.png"))
                    && Files.isRegularFile(root.resolve(
                    "data/moregates/recipes/crystallizing/dark_ascension_variant_crystal.json"));
        } catch (IOException ignored) {
            return false;
        }
    }

    private static void writePackMetadata(Path root) throws IOException {
        JsonObject pack = new JsonObject();
        JsonObject metadata = new JsonObject();
        metadata.addProperty("pack_format", 15);
        metadata.addProperty("description", "Atlantis DHD: More Gates 4.3.2 compatibility");
        pack.add("pack", metadata);
        writeJson(root.resolve("pack.mcmeta"), pack);
    }

    private static void writeServerRegistryBridges(Path root) throws IOException {
        writeSinglePropertyJson(root.resolve(
                        "data/moregates/sgjourney/symbols/enchantment.json"),
                "client_symbols", "moregates:enchantment");
        writeSinglePropertyJson(root.resolve(
                        "data/moregates/sgjourney/symbols/universal_custom.json"),
                "client_symbols", "moregates:universal_custom");
        writeSinglePropertyJson(root.resolve(
                        "data/moregates/sgjourney/point_of_origin/enchant.json"),
                "client_point_of_origin", "moregates:enchant");
        writeSinglePropertyJson(root.resolve(
                        "data/moregates/sgjourney/point_of_origin/icarus.json"),
                "client_point_of_origin", "sgjourney:icarus");
    }

    /**
     * More Gates 4.3.2 omits input_fluid from all of its SGJourney crystallizer
     * recipes. SGJourney supplies liquid naquadah only later as a serializer
     * default, after Almost Fluidified has already inspected the recipe JSON.
     * Copying the exact recipes into this required top-priority pack and making
     * that default explicit gives Almost Fluidified a real id to unify.
     */
    private static void generateCrystallizingRecipeOverrides(ZipFile zip, Path root)
            throws IOException {
        String prefix = "data/moregates/recipes/crystallizing/";
        int generated = 0;
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(prefix)
                    || !entry.getName().endsWith(".json")) {
                continue;
            }

            JsonObject recipe;
            try (Reader reader = new java.io.InputStreamReader(
                    zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                recipe = JsonParser.parseReader(reader).getAsJsonObject();
            }

            if (!recipe.has("type")
                    || !"sgjourney:crystallizing".equals(recipe.get("type").getAsString())) {
                throw new IOException("Unexpected More Gates crystallizer recipe type: "
                        + entry.getName());
            }

            if (!recipe.has("input_fluid")) {
                JsonObject inputFluid = new JsonObject();
                inputFluid.addProperty("id", "sgjourney:liquid_naquadah");
                inputFluid.addProperty("amount", 100);
                recipe.add("input_fluid", inputFluid);
            }

            writeJson(safeResolve(root, entry.getName()), recipe);
            generated++;
        }

        if (generated != 30) {
            throw new IOException("Expected 30 More Gates crystallizer recipes, found "
                    + generated);
        }
    }

    private static void writeClientPointOfOrigin(Path root) throws IOException {
        JsonObject point = new JsonObject();
        point.addProperty("name", "point_of_origin.moregates.enchant");
        point.addProperty("texture", "moregates:symbols/points_of_origin/enchant");
        writeJson(root.resolve(
                "assets/moregates/sgjourney/point_of_origin/enchant.json"), point);
    }

    private static void generateSymbolSet(ZipFile zip, Path root, String atlasEntry,
                                          String id, int tileSize) throws IOException {
        ZipEntry entry = zip.getEntry(atlasEntry);
        if (entry == null) {
            throw new IOException("Required More Gates 4.3.2 atlas is missing: " + atlasEntry);
        }

        BufferedImage atlas;
        try (InputStream input = zip.getInputStream(entry)) {
            atlas = ImageIO.read(input);
        }
        if (atlas == null || atlas.getHeight() != tileSize || atlas.getWidth() != tileSize * 38) {
            throw new IOException("Unexpected dimensions for " + atlasEntry);
        }

        JsonArray textures = new JsonArray();
        Path textureDirectory = root.resolve("assets/moregates/textures/symbol/generated");
        Files.createDirectories(textureDirectory);
        for (int index = 1; index <= 38; index++) {
            String textureName = id + "_" + index;
            textures.add("moregates:symbol/generated/" + textureName);
            BufferedImage tile = atlas.getSubimage((index - 1) * tileSize, 0, tileSize, tileSize);
            Path output = textureDirectory.resolve(textureName + ".png");
            if (!ImageIO.write(tile, "PNG", output.toFile())) {
                throw new IOException("PNG writer unavailable while generating " + output);
            }
        }

        JsonObject symbols = new JsonObject();
        symbols.addProperty("name", "symbols.moregates." + id);
        symbols.addProperty("symbol_set", "moregates:" + id);
        symbols.add("textures", textures.deepCopy());
        writeJson(root.resolve("assets/moregates/sgjourney/symbols/" + id + ".json"), symbols);

        JsonObject symbolSet = new JsonObject();
        symbolSet.addProperty("name", "symbol_set.moregates." + id);
        symbolSet.add("textures", textures);
        writeJson(root.resolve("assets/moregates/sgjourney/symbol_set/" + id + ".json"), symbolSet);
    }

    private static void generateModernVariantJson(ZipFile zip, Path root) throws IOException {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        String prefix = "assets/moregates/sgjourney/stargate_variant/";
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory() || !entry.getName().startsWith(prefix)
                    || !entry.getName().endsWith(".json")) {
                continue;
            }

            JsonObject variant;
            try (Reader reader = new java.io.InputStreamReader(
                    zip.getInputStream(entry), StandardCharsets.UTF_8)) {
                variant = JsonParser.parseReader(reader).getAsJsonObject();
            }

            String relative = entry.getName().substring(prefix.length());
            String gateType = relative.substring(0, relative.indexOf('/'));
            modernizeWormhole(variant, "wormhole", gateType, false);
            modernizeWormhole(variant, "shiny_wormhole", gateType, true);

            Path output = safeResolve(root, entry.getName());
            writeJson(output, variant);
        }
    }

    private static void modernizeWormhole(JsonObject variant, String member,
                                          String gateType, boolean shiny) {
        if (!variant.has(member) || !variant.get(member).isJsonObject()) {
            return;
        }

        JsonObject wormhole = variant.getAsJsonObject(member);
        if (wormhole.has("event_horizon") && !wormhole.has("event_horizon_unstable")) {
            wormhole.add("event_horizon_unstable", wormhole.get("event_horizon").deepCopy());
        }

        String nativeType = switch (gateType) {
            case "pegasus", "tollan", "universe" -> gateType;
            default -> "milky_way";
        };
        if (!wormhole.has("strudel")) {
            String suffix = shiny ? "_vortex_shiny.png" : "_vortex.png";
            wormhole.add("strudel", frontBackTexture(
                    "sgjourney:textures/entity/stargate/" + nativeType + "/"
                            + nativeType + suffix,
                    32, 5, 160, 1.0));
        }

        if (!wormhole.has("disconnect_ticks")) {
            JsonObject ticks = new JsonObject();
            ticks.addProperty("wait_ticks", 16);
            ticks.addProperty("fade_in_ticks", 10);
            ticks.addProperty("stable_ticks", 0);
            ticks.addProperty("fade_out_ticks", 20);
            wormhole.add("disconnect_ticks", ticks);
        }

        if (!wormhole.has("disconnect")) {
            wormhole.add("disconnect", frontBackTexture(
                    "sgjourney:textures/entity/stargate/" + nativeType + "/"
                            + nativeType + "_disconnect.png",
                    20, 1, 20, 1.0));
        }
    }

    private static JsonObject frontBackTexture(String texture, int rows, int columns,
                                               int frames, double alpha) {
        JsonObject frontBack = new JsonObject();
        frontBack.add("front", texture(texture, rows, columns, frames, alpha));
        frontBack.add("back", texture(texture, rows, columns, frames, alpha));
        return frontBack;
    }

    private static JsonObject texture(String texture, int rows, int columns,
                                      int frames, double alpha) {
        JsonObject value = new JsonObject();
        value.addProperty("texture", texture);
        value.addProperty("rows", rows);
        value.addProperty("columns", columns);
        value.addProperty("frames", frames);
        JsonObject rgba = new JsonObject();
        rgba.addProperty("red", 1.0);
        rgba.addProperty("green", 1.0);
        rgba.addProperty("blue", 1.0);
        rgba.addProperty("alpha", alpha);
        value.add("rgba", rgba);
        return value;
    }

    private static void writeSinglePropertyJson(Path path, String key, String value)
            throws IOException {
        JsonObject object = new JsonObject();
        object.addProperty(key, value);
        writeJson(path, object);
    }

    private static void writeJson(Path path, JsonObject object) throws IOException {
        writeString(path, GSON.toJson(object) + "\n");
    }

    private static void writeString(Path path, String value) throws IOException {
        Files.createDirectories(path.getParent());
        Files.writeString(path, value, StandardCharsets.UTF_8);
    }

    private static Path safeResolve(Path root, String relative) throws IOException {
        Path result = root.resolve(relative).normalize();
        if (!result.startsWith(root.normalize())) {
            throw new IOException("Unsafe More Gates resource path: " + relative);
        }
        return result;
    }
}
