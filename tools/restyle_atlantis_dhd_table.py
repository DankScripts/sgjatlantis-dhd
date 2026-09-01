#!/usr/bin/env python3
"""Restyle the Atlantis DHD table and its explicitly selected controls."""

from __future__ import annotations

import base64
import copy
import json
import shutil
import uuid
import zipfile
from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
BASELINE = ROOT / "baseline" / "sgjatlantis-dhd-1.20.1-2.0.0-beta.1.jar"
SOURCE_BBMODEL = ROOT / "blockbench" / "atlantis_dhd_table_original.bbmodel"
OUTPUT_BBMODEL = ROOT / "blockbench" / "atlantis_dhd_table_shield_style.bbmodel"
RESOURCE_ROOT = ROOT / "src" / "main" / "resources"

NAMESPACES = ("sgjadditions", "sgjadditions_capacity_patch")
STYLE_TEXTURES = {
    "table_shell": "atlantis_prop_shell.png",
    "table_fascia": "atlantis_prop_fascia.png",
    "table_deck": "atlantis_prop_deck.png",
    "table_inlay": "atlantis_prop_inlay.png",
    "table_shadow": "atlantis_prop_shadow.png",
}
BROWN_PLATE_FILENAME = "brown_plate.png"

# The DHD plate is assembled from 21 small angled cuboids.  A patterned
# texture produces visible atlas/mipmap seams at all of those UV boundaries,
# so keep the dedicated plate texture completely uniform and opaque.  This is
# the medium copper-brown already present in the accepted fascia palette.
DIALER_PLATE_COLOR = (112, 57, 47, 255)

# The front shadow band is only 0.25 model pixels deep.  Compressing the
# patterned inlay across its exposed top/end faces turns the copper accents
# into bright dotted streaks at both front corners.  Preserve the inlay's
# accepted dark base color in a fully uniform, mipmap-safe texture.
FRONT_SHADOW_COLOR = (25, 21, 25, 255)

# Push the two raised side housings outward far enough to hide the exposed end
# faces seen in game.  One model pixel is 1/16 of a Minecraft block.
SIDE_HOUSING_EXTENSION = 0.5

# Match the visual weight of the accepted city-shield table supports.  The
# DHD's original sled feet and diagonal legs were only one model pixel wide;
# the shield table uses approximately 2.45- and 2.12-pixel cross-sections.
SLED_FOOT_WIDTH = 2.45
DIAGONAL_LEG_WIDTH = 2.12

# Reversing only the sign of the original diagonal support's rotation would
# drive it below floor level.  This pivot produces the mirrored Z profile while
# preserving the support's accepted world-space height and depth envelope.
FLIPPED_LEG_ANGLE = 45
FLIPPED_LEG_PIVOT_Y = 9.097413408594532
FLIPPED_LEG_PIVOT_Z = 20.031727983645297

# These are the original table-shell elements this pass may restyle.  The
# triangle buttons and all six patterned crystal panels remain unchanged.
CUSTOM_SHELL_INDICES = set(range(8))
BBMODEL_SHELL_INDICES = set(range(8))

# The broad support underneath the six patterned crystal panels.  It is not
# part of the DHD/button assembly, but it should follow the restyled console's
# copper fascia and dark work-deck language.
CRYSTAL_DIVIDER_BOUNDS = ((16, 16.4, 8.5), (30, 18.4, 14.5))

# The original physical iris control was a 2x2 square hovering half a model
# pixel above the work deck.  Replace it in place with a seated, stepped round
# button.  Keeping the same center also preserves its existing interaction
# location.
IRIS_BUTTON_BOUNDS = ((2.6, 17.925, 10.925), (4.6, 18.425, 12.925))
IRIS_BUTTON_PARTS = (
    ("iris_button_base_center", [2.45, 17.4, 11.225], [4.75, 17.56, 12.625], "table_fascia"),
    ("iris_button_base_north", [2.9, 17.4, 10.775], [4.3, 17.56, 11.225], "table_fascia"),
    ("iris_button_base_south", [2.9, 17.4, 12.625], [4.3, 17.56, 13.075], "table_fascia"),
    ("iris_button_crystal_center", [2.65, 17.56, 11.345], [4.55, 17.88, 12.505], "triangle"),
    ("iris_button_crystal_north", [3.02, 17.56, 10.975], [4.18, 17.88, 11.345], "triangle"),
    ("iris_button_crystal_south", [3.02, 17.56, 12.505], [4.18, 17.88, 12.875], "triangle"),
)

DECORATIVE_CUBES = (
    ("front_fascia_shadow", [1.5, 15.55, 0.05], [30.5, 16.9, 0.3], "table_shadow"),
    ("front_fascia_panel_1", [2.2, 15.75, -0.08], [6.2, 16.65, 0.08], "table_fascia"),
    ("front_fascia_panel_2", [7.9, 15.75, -0.08], [12.1, 16.65, 0.08], "table_fascia"),
    ("front_fascia_panel_3", [13.8, 15.75, -0.08], [18.2, 16.65, 0.08], "table_fascia"),
    ("front_fascia_panel_4", [19.9, 15.75, -0.08], [24.1, 16.65, 0.08], "table_fascia"),
    ("front_fascia_panel_5", [25.8, 15.75, -0.08], [29.8, 16.65, 0.08], "table_fascia"),
    ("left_foot_inlay", [0.1, 1.3, 2.0], [0.9, 2.05, 14.0], "table_inlay"),
    ("right_foot_inlay", [31.1, 1.3, 2.0], [31.9, 2.05, 14.0], "table_inlay"),
)


def write_json(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def set_all_faces(element: dict, texture: str | int) -> None:
    for face in element["faces"].values():
        face["texture"] = texture


def bounds_key(element: dict) -> tuple[tuple[float, ...], tuple[float, ...]]:
    return tuple(element["from"]), tuple(element["to"])


def source_brown_plate_bounds() -> set[tuple[tuple[float, ...], tuple[float, ...]]]:
    project = json.loads(SOURCE_BBMODEL.read_text(encoding="utf-8"))
    result = {
        bounds_key(element)
        for element in project["elements"]
        if any(face.get("texture") == 5 for face in element["faces"].values())
    }
    assert len(result) == 21
    return result


def write_brown_plate_texture() -> Path:
    target = ROOT / "blockbench" / BROWN_PLATE_FILENAME
    Image.new("RGBA", (16, 16), DIALER_PLATE_COLOR).save(target)
    with Image.open(target) as image:
        assert image.size == (16, 16)
        assert image.convert("RGBA").getextrema() == tuple(
            (channel, channel) for channel in DIALER_PLATE_COLOR
        )
    return target


def write_front_shadow_texture() -> Path:
    target = ROOT / "blockbench" / STYLE_TEXTURES["table_shadow"]
    Image.new("RGBA", (16, 16), FRONT_SHADOW_COLOR).save(target)
    with Image.open(target) as image:
        assert image.size == (16, 16)
        assert image.convert("RGBA").getextrema() == tuple(
            (channel, channel) for channel in FRONT_SHADOW_COLOR
        )
    return target


def style_dhd_backplate(elements: list[dict], bbmodel: bool) -> set[int]:
    # Use the dedicated UV-safe copper-brown plate texture.  A full patterned
    # fascia image bleeds between these 21 small angled pieces in game.
    target_texture: str | int = 5 if bbmodel else "#5"
    target_bounds = source_brown_plate_bounds()
    changed = set()
    for index, element in enumerate(elements):
        if bounds_key(element) in target_bounds:
            set_all_faces(element, target_texture)
            changed.add(index)
    assert len(changed) == 21
    return changed


def style_crystal_divider(elements: list[dict], bbmodel: bool) -> int:
    index = next(
        index for index, element in enumerate(elements)
        if bounds_key(element) == CRYSTAL_DIVIDER_BOUNDS
    )
    fascia: str | int = 7 if bbmodel else "#table_fascia"
    deck: str | int = 8 if bbmodel else "#table_deck"
    set_all_faces(elements[index], fascia)
    elements[index]["faces"]["up"]["texture"] = deck
    return index


def without_face_textures(element: dict) -> dict:
    result = copy.deepcopy(element)
    for face in result["faces"].values():
        face.pop("texture", None)
    return result


def adjust_table_shell_geometry(elements: list[dict], bbmodel: bool) -> None:
    """Refine the side housings and widen/reverse the Z-shaped supports."""

    elements[5]["from"][0] = -SIDE_HOUSING_EXTENSION
    elements[6]["to"][0] = 32 + SIDE_HOUSING_EXTENSION

    for index, width in ((0, SLED_FOOT_WIDTH), (1, SLED_FOOT_WIDTH),
                         (2, DIAGONAL_LEG_WIDTH), (3, DIAGONAL_LEG_WIDTH)):
        center = (elements[index]["from"][0] + elements[index]["to"][0]) / 2
        elements[index]["from"][0] = center - width / 2
        elements[index]["to"][0] = center + width / 2

    for index in (2, 3):
        if bbmodel:
            elements[index]["rotation"] = [FLIPPED_LEG_ANGLE, 0, 0]
            elements[index]["origin"] = [
                elements[index]["origin"][0],
                FLIPPED_LEG_PIVOT_Y,
                FLIPPED_LEG_PIVOT_Z,
            ]
        else:
            elements[index]["rotation"]["angle"] = FLIPPED_LEG_ANGLE
            elements[index]["rotation"]["origin"] = [
                elements[index]["rotation"]["origin"][0],
                FLIPPED_LEG_PIVOT_Y,
                FLIPPED_LEG_PIVOT_Z,
            ]


def style_original_shell(elements: list[dict], bbmodel: bool) -> None:
    if bbmodel:
        shell, fascia, deck = 6, 7, 8
        shell_indices = BBMODEL_SHELL_INDICES
    else:
        shell, fascia, deck = "#table_shell", "#table_fascia", "#table_deck"
        shell_indices = CUSTOM_SHELL_INDICES

    for index in (0, 1, 2, 3):
        set_all_faces(elements[index], shell)

    # The central slab reads as a patterned copper fascia wrapped around a
    # darker Lantean work deck, matching the shield console without moving any
    # of the controls sitting on it.
    set_all_faces(elements[4], fascia)
    elements[4]["faces"]["up"]["texture"] = deck
    elements[4]["faces"]["down"]["texture"] = shell

    for index in (5, 6, 7):
        set_all_faces(elements[index], shell)
        elements[index]["faces"]["up"]["texture"] = fascia

    assert shell_indices == set(range(8))


def cube_faces(texture: str | int) -> dict:
    return {
        direction: {"uv": [0, 0, 16, 16], "texture": texture}
        for direction in ("north", "east", "south", "west", "up", "down")
    }


def custom_cube(start: list[float], end: list[float], texture: str) -> dict:
    return {"from": start, "to": end, "faces": cube_faces(f"#{texture}")}


def bbmodel_cube(name: str, start: list[float], end: list[float], texture: int) -> dict:
    element_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, f"sgjatlantis-dhd:{name}"))
    return {
        "name": name,
        "box_uv": False,
        "render_order": "default",
        "rescale": False,
        "locked": False,
        "shade": True,
        "light_emission": 0,
        "export": True,
        "scope": 0,
        "allow_mirror_modeling": True,
        "from": start,
        "to": end,
        "autouv": 0,
        "color": 0,
        "origin": [16, 8, 8],
        "faces": cube_faces(texture),
        "type": "cube",
        "uuid": element_uuid,
    }


def replace_iris_button(elements: list[dict], bbmodel: bool) -> tuple[int, list[str]]:
    index = next(
        index for index, element in enumerate(elements)
        if bounds_key(element) == IRIS_BUTTON_BOUNDS
    )

    texture_lookup: dict[str, str | int] = (
        {"table_fascia": 7, "triangle": 0}
        if bbmodel
        else {"table_fascia": "table_fascia", "triangle": "triangle"}
    )
    original_uuid = elements[index].get("uuid")
    name, start, end, texture = IRIS_BUTTON_PARTS[0]
    if bbmodel:
        replacement = bbmodel_cube(name, start, end, texture_lookup[texture])
        replacement["uuid"] = original_uuid
    else:
        replacement = custom_cube(start, end, texture_lookup[texture])
    elements[index] = replacement

    added_uuids = []
    for name, start, end, texture in IRIS_BUTTON_PARTS[1:]:
        if bbmodel:
            element = bbmodel_cube(name, start, end, texture_lookup[texture])
            added_uuids.append(element["uuid"])
        else:
            element = custom_cube(start, end, texture_lookup[texture])
        elements.append(element)
    return index, added_uuids


def insert_after_outliner_uuid(nodes: list, target_uuid: str, added_uuids: list[str]) -> bool:
    for index, node in enumerate(nodes):
        if node == target_uuid:
            nodes[index + 1:index + 1] = added_uuids
            return True
        if isinstance(node, dict) and insert_after_outliner_uuid(
            node.get("children", []), target_uuid, added_uuids
        ):
            return True
    return False


def embedded_texture(name: str, identifier: str, path: Path, use_as_default: bool = False) -> dict:
    image_bytes = path.read_bytes()
    with Image.open(path) as image:
        width, height = image.size
    return {
        "name": name,
        "path": "",
        "folder": "",
        "namespace": "",
        "id": identifier,
        "group": "",
        "scope": 0,
        "width": width,
        "height": height,
        "uv_width": 16,
        "uv_height": 16,
        "particle": False,
        "use_as_default": use_as_default,
        "layers_enabled": False,
        "sync_to_project": "",
        "file_format": "png",
        "render_mode": "default",
        "render_sides": "auto",
        "wrap_mode": "limited",
        "pbr_channel": "color",
        "fps": 7,
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": str(uuid.uuid5(uuid.NAMESPACE_URL, f"sgjatlantis-dhd:texture:{identifier}")),
        "source": "data:image/png;base64," + base64.b64encode(image_bytes).decode("ascii"),
    }


def write_runtime_models() -> None:
    brown_plate = write_brown_plate_texture()
    write_front_shadow_texture()
    with zipfile.ZipFile(BASELINE) as baseline:
        for namespace in NAMESPACES:
            custom_path = f"assets/{namespace}/models/custom/atlantisdhd.json"
            block_path = f"assets/{namespace}/models/block/atlantisdhd.json"
            texture_map_path = f"assets/{namespace}/models/block/atlantisdhd.json.textures"

            original_custom = json.loads(baseline.read(custom_path))
            styled_custom = copy.deepcopy(original_custom)
            style_original_shell(styled_custom["elements"], bbmodel=False)
            adjust_table_shell_geometry(styled_custom["elements"], bbmodel=False)
            backplate_indices = style_dhd_backplate(
                styled_custom["elements"], bbmodel=False
            )
            divider_index = style_crystal_divider(styled_custom["elements"], bbmodel=False)
            iris_index, _ = replace_iris_button(styled_custom["elements"], bbmodel=False)
            for _, start, end, texture in DECORATIVE_CUBES:
                styled_custom["elements"].append(custom_cube(start, end, texture))

            # Buttons and crystal controls stay frozen.  Only the eight table
            # shell pieces may change geometry; the two requested control-area
            # corrections remain texture-only.
            texture_only_indices = backplate_indices | {divider_index}
            for index, before in enumerate(original_custom["elements"]):
                after = styled_custom["elements"][index]
                if index in CUSTOM_SHELL_INDICES:
                    continue
                if index == iris_index:
                    continue
                if index in texture_only_indices:
                    assert without_face_textures(after) == without_face_textures(before)
                else:
                    assert after == before

            original_block = json.loads(baseline.read(block_path))
            styled_block = copy.deepcopy(original_block)
            for key, filename in STYLE_TEXTURES.items():
                styled_block["textures"][key] = f"{namespace}:block/{Path(filename).stem}"
            styled_block["textures"]["5"] = f"{namespace}:block/brown_plate"

            original_texture_map = json.loads(baseline.read(texture_map_path))
            styled_texture_map = copy.deepcopy(original_texture_map)
            texture_map = styled_texture_map["mappings"]["default"]["map"]
            for key, filename in STYLE_TEXTURES.items():
                texture_map[key] = Path(filename).stem
            texture_map["5"] = "brown_plate"

            namespace_root = RESOURCE_ROOT / "assets" / namespace
            write_json(namespace_root / "models/custom/atlantisdhd.json", styled_custom)
            write_json(namespace_root / "models/block/atlantisdhd.json", styled_block)
            write_json(namespace_root / "models/block/atlantisdhd.json.textures", styled_texture_map)
            for filename in STYLE_TEXTURES.values():
                target = namespace_root / "textures/block" / filename
                target.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(ROOT / "blockbench" / filename, target)
            shutil.copyfile(brown_plate, namespace_root / "textures/block/brown_plate.png")

        # This built-in compatibility pack overrides SGJ Additions at runtime.
        # Keep the custom model, its child block-model texture bindings, and the
        # referenced textures together in this higher-priority pack.  Supplying
        # only the custom model leaves #table_* unresolved against SGJ
        # Additions' original block model and produces Minecraft's magenta/black
        # missing-texture fallback.
        override_root = (
            RESOURCE_ROOT
            / "resourcepacks/sgjatlantis_sgjadditions_override/assets/sgjadditions"
        )
        override_source = json.loads(baseline.read(
            "resourcepacks/sgjatlantis_sgjadditions_override/assets/sgjadditions/models/custom/atlantisdhd.json"
        ))
        style_original_shell(override_source["elements"], bbmodel=False)
        adjust_table_shell_geometry(override_source["elements"], bbmodel=False)
        style_dhd_backplate(override_source["elements"], bbmodel=False)
        style_crystal_divider(override_source["elements"], bbmodel=False)
        replace_iris_button(override_source["elements"], bbmodel=False)
        for _, start, end, texture in DECORATIVE_CUBES:
            override_source["elements"].append(custom_cube(start, end, texture))
        write_json(
            override_root / "models/custom/atlantisdhd.json",
            override_source,
        )

        override_block = json.loads(baseline.read(
            "assets/sgjadditions/models/block/atlantisdhd.json"
        ))
        for key, filename in STYLE_TEXTURES.items():
            override_block["textures"][key] = f"sgjadditions:block/{Path(filename).stem}"
        override_block["textures"]["5"] = "sgjadditions:block/brown_plate"
        write_json(override_root / "models/block/atlantisdhd.json", override_block)

        override_texture_map = json.loads(baseline.read(
            "assets/sgjadditions/models/block/atlantisdhd.json.textures"
        ))
        override_map = override_texture_map["mappings"]["default"]["map"]
        for key, filename in STYLE_TEXTURES.items():
            override_map[key] = Path(filename).stem
        override_map["5"] = "brown_plate"
        write_json(
            override_root / "models/block/atlantisdhd.json.textures",
            override_texture_map,
        )

        for filename in STYLE_TEXTURES.values():
            target = override_root / "textures/block" / filename
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(ROOT / "blockbench" / filename, target)
        shutil.copyfile(brown_plate, override_root / "textures/block/brown_plate.png")


def write_blockbench_model() -> None:
    project = json.loads(SOURCE_BBMODEL.read_text(encoding="utf-8"))
    original = copy.deepcopy(project)

    brown_plate = write_brown_plate_texture()
    write_front_shadow_texture()

    # Restore the actual patterned crystal panel image in the supplied source
    # so the protected left-side panels preview exactly as they do in game.
    project["textures"][4] = embedded_texture("5_console.png", "2", ROOT / "blockbench/5_console.png")
    project["textures"][5] = embedded_texture(BROWN_PLATE_FILENAME, "5", brown_plate)

    for texture_id, filename in STYLE_TEXTURES.items():
        project["textures"].append(embedded_texture(filename, texture_id, ROOT / "blockbench" / filename))
    assert len(project["textures"]) == 11

    style_original_shell(project["elements"], bbmodel=True)
    adjust_table_shell_geometry(project["elements"], bbmodel=True)
    backplate_indices = style_dhd_backplate(project["elements"], bbmodel=True)
    divider_index = style_crystal_divider(project["elements"], bbmodel=True)
    iris_index, iris_added = replace_iris_button(project["elements"], bbmodel=True)
    assert insert_after_outliner_uuid(
        project["outliner"], project["elements"][iris_index]["uuid"], iris_added
    )
    added = []
    texture_indices = {key: index for index, key in enumerate(STYLE_TEXTURES, start=6)}
    for name, start, end, texture in DECORATIVE_CUBES:
        element = bbmodel_cube(name, start, end, texture_indices[texture])
        project["elements"].append(element)
        added.append(element["uuid"])

    # Protect every unselected original control.  The shell and iris button may
    # change geometry; the DHD backplate and crystal divider change texture
    # only.  Triangle-button and patterned-crystal geometry stays frozen.
    for index in range(len(original["elements"])):
        if index in BBMODEL_SHELL_INDICES | {iris_index}:
            continue
        before = copy.deepcopy(original["elements"][index])
        after = copy.deepcopy(project["elements"][index])
        if index in backplate_indices | {divider_index}:
            assert without_face_textures(after) == without_face_textures(before)
        else:
            assert after == before

    group_uuid = str(uuid.uuid5(uuid.NAMESPACE_URL, "sgjatlantis-dhd:shield-style-shell-details"))
    project["outliner"].append({
        "name": "shield_style_shell_details",
        "origin": [16, 8, 8],
        "color": 0,
        "uuid": group_uuid,
        "export": True,
        "isOpen": True,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": added,
    })
    project["name"] = "atlantis_dhd_table_shield_style"
    project["model_identifier"] = "atlantis_dhd_table_shield_style"
    write_json(OUTPUT_BBMODEL, project)


def main() -> None:
    write_runtime_models()
    write_blockbench_model()
    print(f"Wrote {OUTPUT_BBMODEL}")
    print("Protected DHD controls verified unchanged.")


if __name__ == "__main__":
    main()
