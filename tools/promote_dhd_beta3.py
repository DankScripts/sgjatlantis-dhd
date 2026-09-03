"""Promote the user-confirmed DHD hotfix5 artifacts to 2.0.3-beta.3."""
import hashlib
import json
from pathlib import Path
import struct
import sys
import zipfile

OLD = b"2.0.2-beta.2-hotfix5"
NEW = b"2.0.3-beta.3"
NETWORK = "com/mustangdoc/sgjpatch/network/SGJPatchNetwork.class"
USE_HELPER = "com/mustangdoc/sgjpatch/compat/AtlantisDHDUseHelper.class"
LEGACY_MIXIN = "com/mustangdoc/sgjpatch/mixin/AtlantisDHDBlockMixin.class"

USE_OLD = bytes.fromhex(
    "19 04 b6 00 13 9a 00 0e 19 06 b6 00 18 b2 00 1e a5 00 07 "
    "04 a7 00 04 03 36 07"
)
USE_NEW = bytes.fromhex(
    "19 04 b6 00 13 9a 00 0e 00 00 00 00 00 00 01 01 a5 00 07 "
    "04 a7 00 04 03 36 07"
)
LEGACY_OLD = bytes.fromhex("18 08 14 00 55 97 9e 00 99")


def read_zip(path):
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None
        infos = [i for i in archive.infolist() if not i.is_dir()]
        return infos, {i.filename: archive.read(i.filename) for i in infos}


def replace_utf8_constant(class_bytes):
    """Replace one modified-UTF8 constant while preserving the whole class structure."""
    assert class_bytes[:4] == b"\xca\xfe\xba\xbe"
    count = struct.unpack_from(">H", class_bytes, 8)[0]
    cursor = 10
    found = []
    index = 1
    while index < count:
        tag = class_bytes[cursor]
        cursor += 1
        if tag == 1:
            length = struct.unpack_from(">H", class_bytes, cursor)[0]
            start = cursor + 2
            value = class_bytes[start:start + length]
            if value == OLD:
                found.append((cursor, start + length))
            cursor = start + length
        elif tag in (3, 4):
            cursor += 4
        elif tag in (5, 6):
            cursor += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            cursor += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            cursor += 4
        elif tag == 15:
            cursor += 3
        else:
            raise AssertionError(f"unknown class constant tag {tag}")
        index += 1
    assert len(found) == 1
    length_pos, end = found[0]
    return class_bytes[:length_pos] + struct.pack(">H", len(NEW)) + NEW + class_bytes[end:]


def parse_constant_pool(class_bytes):
    assert class_bytes[:4] == b"\xca\xfe\xba\xbe"
    count = struct.unpack_from(">H", class_bytes, 8)[0]
    cursor = 10
    entries = {}
    index = 1
    while index < count:
        start = cursor
        tag = class_bytes[cursor]
        cursor += 1
        if tag == 1:
            length = struct.unpack_from(">H", class_bytes, cursor)[0]
            value = class_bytes[cursor + 2:cursor + 2 + length]
            entries[index] = (tag, value)
            cursor += 2 + length
        elif tag in (3, 4):
            cursor += 4
        elif tag in (5, 6):
            cursor += 8
            index += 1
        elif tag in (7, 8, 16, 19, 20):
            cursor += 2
        elif tag in (9, 10, 11, 12, 17, 18):
            cursor += 4
        elif tag == 15:
            cursor += 3
        else:
            raise AssertionError(f"unknown class constant tag {tag} at {start}")
        index += 1
    return count, cursor, entries


def patch_legacy_mixin(class_bytes):
    """Replace the hit-height choice with Player.isShiftKeyDown(), length-neutrally."""
    assert class_bytes.count(LEGACY_OLD) == 1
    old_count, cp_end, entries = parse_constant_pool(class_bytes)
    bool_desc = next(i for i, entry in entries.items() if entry == (1, b"()Z"))
    added = bytearray()

    def utf8(value):
        nonlocal old_count
        index = old_count
        old_count += 1
        added.extend(b"\x01" + struct.pack(">H", len(value)) + value)
        return index

    player_name = utf8(b"net/minecraft/world/entity/player/Player")
    player_class = old_count
    old_count += 1
    added.extend(b"\x07" + struct.pack(">H", player_name))
    method_name = utf8(b"m_6144_")
    name_and_type = old_count
    old_count += 1
    added.extend(b"\x0c" + struct.pack(">HH", method_name, bool_desc))
    method_ref = old_count
    old_count += 1
    added.extend(b"\x0a" + struct.pack(">HH", player_class, name_and_type))
    assert method_ref <= 0xFFFF

    with_cp = class_bytes[:8] + struct.pack(">H", old_count) + class_bytes[10:cp_end] + bytes(added) + class_bytes[cp_end:]
    replacement = bytes.fromhex("19 04 b6") + struct.pack(">H", method_ref) + bytes.fromhex("9a 00 9a 00")
    assert len(replacement) == len(LEGACY_OLD)
    assert with_cp.count(LEGACY_OLD) == 1
    result = with_cp.replace(LEGACY_OLD, replacement)
    assert result.count(replacement) == 1
    return result


def use_helper_source():
    return b'''package com.mustangdoc.sgjpatch.compat;

import com.mustangdoc.sgjpatch.block.AtlantisDHDBlock;
import com.mustangdoc.sgjpatch.menu.AtlantisDHDCrystalMenu;
import com.mustangdoc.sgjpatch.standalone.AtlantisDHDEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;

/** Routes normal right-click to the dialer and sneak-right-click to Crystal Controls. */
public final class AtlantisDHDUseHelper {
    private AtlantisDHDUseHelper() {}

    public static InteractionResult use(AtlantisDHDBlock block, BlockState state,
            Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        if (player.isShiftKeyDown()) {
            if (player instanceof ServerPlayer serverPlayer) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity instanceof AtlantisDHDEntity) {
                    SimpleMenuProvider provider = new SimpleMenuProvider(
                            (containerId, inventory, menuPlayer) ->
                                    new AtlantisDHDCrystalMenu(containerId, inventory, blockEntity),
                            Component.literal("Atlantis DHD Crystal / Power Unit"));
                    NetworkHooks.openScreen(serverPlayer, provider, pos);
                }
            }
            return InteractionResult.CONSUME;
        }

        return block.sgjpatch$useNormal(state, level, pos, player, hand, hit);
    }
}
'''


def write_zip(path, infos, data):
    assert not path.exists(), f"refusing to overwrite {path}"
    with zipfile.ZipFile(path, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        for info in infos:
            archive.writestr(info, data[info.filename])
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None
        assert {n: archive.read(n) for n in archive.namelist() if not n.endswith('/')} == data


def main():
    jar_in, source_in, output = map(Path, sys.argv[1:])
    output.mkdir(parents=True, exist_ok=True)
    jar_infos, jar = read_zip(jar_in)
    src_infos, src = read_zip(source_in)
    old_jar = dict(jar)
    old_src = dict(src)

    jar["META-INF/MANIFEST.MF"] = jar["META-INF/MANIFEST.MF"].replace(OLD, NEW)
    jar["META-INF/mods.toml"] = jar["META-INF/mods.toml"].replace(OLD, NEW)
    previous_description = (
        b"Version 2.0.3-beta.3 insets the triangular dialer backplate UVs to prevent "
        b"edge sampling into neighboring atlas textures. All geometry, crystal heights, "
        b"buttons, shield-style frame, GUI, and gameplay behavior are preserved."
    )
    release_description = (
        b"Version 2.0.3-beta.3 makes normal right-click open the Atlantis dialer from "
        b"any part of the table. Crystal Controls now opens only with sneak-right-click. "
        b"The user-confirmed hotfix5 visuals, models, GUI layouts, recipes, and all other "
        b"gameplay behavior are preserved."
    )
    assert previous_description in jar["META-INF/mods.toml"]
    jar["META-INF/mods.toml"] = jar["META-INF/mods.toml"].replace(previous_description, release_description)
    jar[NETWORK] = replace_utf8_constant(jar[NETWORK])
    assert jar[USE_HELPER].count(USE_OLD) == 1
    jar[USE_HELPER] = jar[USE_HELPER].replace(USE_OLD, USE_NEW)
    jar[LEGACY_MIXIN] = patch_legacy_mixin(jar[LEGACY_MIXIN])

    for name in ("gradle.properties", "src/main/java/com/mustangdoc/sgjpatch/network/SGJPatchNetwork.java"):
        assert src[name].count(OLD) == 1
        src[name] = src[name].replace(OLD, NEW)
    source_mods = "src/main/resources/META-INF/mods.toml"
    assert src[source_mods].count(OLD) == 1
    src[source_mods] = src[source_mods].replace(OLD, NEW)
    assert previous_description in src[source_mods]
    src[source_mods] = src[source_mods].replace(previous_description, release_description)
    src["CHANGELOG.md"] = (
        b"# 2.0.3-beta.3\n\n"
        b"- Normal right-click opens the Atlantis dialer from every part of the table.\n"
        b"- Crystal Controls opens only with sneak/Shift + right-click.\n"
        b"- Removes the old side/lower-table shortcut to Crystal Controls.\n"
        b"- Preserves every confirmed hotfix5 model and visual unchanged.\n\n"
        + src["CHANGELOG.md"]
    )
    src["src/main/java/com/mustangdoc/sgjpatch/compat/AtlantisDHDUseHelper.java"] = use_helper_source()
    src["README-BETA3.md"] = (
        b"# 2.0.3-beta.3\n\n"
        b"This release starts from the exact user-confirmed 2.0.2-beta.2-hotfix5\n"
        b"JAR and source. Normal right-click now opens the dialer regardless of which\n"
        b"table face or height was clicked. Crystal Controls opens only while the\n"
        b"player is sneaking (Shift + right-click). This is enforced for both the\n"
        b"standalone Atlantis DHD and the SGJ Additions compatibility table.\n\n"
        b"No model, texture, geometry, crystal position, button mapping, GUI layout,\n"
        b"recipe, power behavior, or other gameplay behavior changed. Historical\n"
        b"hotfix scripts and validation reports remain unchanged. The supplied\n"
        b"promotion tool reproducibly applies the two narrowly validated class-code\n"
        b"changes to the confirmed binary baseline.\n\n"
        b"Install this JAR instead of hotfix5, not alongside it. Client and server\n"
        b"must use the same version because the existing handshake enforces it.\n"
    )

    jar_changed = {n for n in jar if jar[n] != old_jar[n]}
    assert jar_changed == {"META-INF/MANIFEST.MF", "META-INF/mods.toml", NETWORK,
                           USE_HELPER, LEGACY_MIXIN}
    assert all(jar[n] == old_jar[n] for n in jar if n not in jar_changed)
    assert jar[NETWORK].count(NEW) == 1 and OLD not in jar[NETWORK]
    assert src["gradle.properties"].count(NEW) == 1
    assert src[source_mods].replace(b"${mod_version}", NEW) == jar["META-INF/mods.toml"]
    source_changed = {n for n in old_src if src[n] != old_src[n]}
    assert source_changed == {"CHANGELOG.md", "gradle.properties", source_mods,
                              "src/main/java/com/mustangdoc/sgjpatch/network/SGJPatchNetwork.java"}

    report = {
        "version": NEW.decode(),
        "baseline_jar_sha256": hashlib.sha256(jar_in.read_bytes()).hexdigest(),
        "baseline_source_sha256": hashlib.sha256(source_in.read_bytes()).hexdigest(),
        "changed_jar_entries": sorted(jar_changed),
        "interaction_behavior": {
            "normal_right_click": "dialer GUI from all table faces",
            "sneak_right_click": "Crystal Controls",
            "standalone_and_legacy_compatibility_paths_updated": True,
        },
        "all_models_textures_resources_and_other_gameplay_classes_byte_identical": True,
        "network_class_change": "UTF8 protocol version constant only",
        "minecraft_runtime_tested": False,
    }
    src["beta3-promotion-validation.json"] = (json.dumps(report, indent=2) + "\n").encode()
    src["tools/promote_dhd_beta3.py"] = Path(__file__).read_bytes()
    src_infos += [
        zipfile.ZipInfo("README-BETA3.md"),
        zipfile.ZipInfo("beta3-promotion-validation.json"),
        zipfile.ZipInfo("tools/promote_dhd_beta3.py"),
        zipfile.ZipInfo("src/main/java/com/mustangdoc/sgjpatch/compat/AtlantisDHDUseHelper.java"),
    ]

    jar_out = output / "sgjatlantis-dhd-1.20.1-2.0.3-beta.3.jar"
    src_out = output / "sgjatlantis-dhd-1.20.1-2.0.3-beta.3-source.zip"
    write_zip(jar_out, jar_infos, jar)
    write_zip(src_out, src_infos, src)
    report["jar_sha256"] = hashlib.sha256(jar_out.read_bytes()).hexdigest()
    report["source_sha256"] = hashlib.sha256(src_out.read_bytes()).hexdigest()
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
