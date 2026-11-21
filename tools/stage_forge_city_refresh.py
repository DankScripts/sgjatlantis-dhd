"""Stage the final Forge city delta on the existing NeoForge city templates."""
import gzip
import struct
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / "sgjatlantis-dhd-2.0.5" / "tools"))
from refresh_atlantis_city import FORGE, NEO, ROOT, NEO_BLOCK_IDS, nbt, save, template, wipe_spans
from export_atlantis_city import t_int


def convert_entity(raw, previous):
    if not raw:
        return raw
    entity_id = nbt.Reader(raw).payload(10)["id"]
    if entity_id in {"sgjourney:naquadah_generator_mark_ii", "sgjatlantis_cityshield:shield_controller",
                     "sgjadditions_capacity_patch:triple_zpm_hub"}:
        assert previous and previous[1], entity_id
        raw = previous[1]
        before = nbt.Reader(raw).payload(10)
        if entity_id.endswith(":triple_zpm_hub"):
            assert before["id"] == "sgjatlantis_dhd:triple_zpm_hub"
            items = before["inventory"]["Items"]
            assert [i["components"]["sgjourney:entropy"] for i in items] == [1000, 1000, 999]
            old = t_int("sgjourney:entropy", 999)
            assert raw.count(old) == 1
            raw = raw.replace(old, t_int("sgjourney:entropy", 930))
            items[2]["components"]["sgjourney:entropy"] = 930
            assert nbt.Reader(raw).payload(10) == before
        else:
            assert before["id"] == entity_id
    elif entity_id in {"sgjadditions_capacity_patch:atlantis_floor_conduit",
                       "sgjadditions_capacity_patch:powered_light"}:
        old = entity_id.encode()
        new = entity_id.replace("sgjadditions_capacity_patch:", "sgjatlantis_dhd:").encode()
        old = struct.pack(">H", len(old)) + old
        assert raw.count(old) == 1
        raw = raw.replace(old, struct.pack(">H", len(new)) + new)
    else:
        assert entity_id in {"sgjadditions_capacity_patch:atlantis_transporter",
                             "sgjadditions_capacity_patch:atlantis_sliding_door",
                             "sgjourney:large_naquadah_cable", "minecraft:bed", "minecraft:banner"}, entity_id
    return raw


def stage(destination):
    base = ROOT.parent / "work" / "city-refresh-dev89" / "forge"
    templates = destination / "structure" / "atlantis_city"
    manifest_lines = {}
    wipe = destination / "atlantis_city_wipe"
    spaces = destination / "atlantis_city_spaces"
    wipe.mkdir(parents=True, exist_ok=True)
    spaces.mkdir(parents=True, exist_ok=True)
    for file in sorted(FORGE.glob("*.nbt")):
        if gzip.decompress(file.read_bytes()) == gzip.decompress((base / file.name).read_bytes()):
            continue
        _, cx, cz = file.stem.split("_")
        cx, cz = int(cx), int(cz)
        old, forge, neo = (template(path / file.name, cx, cz) for path in (base, FORGE, NEO))
        converted = neo.copy()
        for pos in old.keys() - forge.keys():
            converted.pop(pos, None)
        for pos, (state, raw) in forge.items():
            if old.get(pos) == (state, raw):
                continue
            name = state["Name"]
            namespace, path = name.split(":")
            if namespace == "sgjadditions_capacity_patch" and path in NEO_BLOCK_IDS:
                state = {**state, "Name": f"sgjatlantis_dhd:{path}"}
            elif namespace == "sgjadditions_capacity_patch":
                assert path in {"atlantis_sliding_door", "atlantis_transporter",
                                "atlantis_transporter_panel", "atlantis_door_control"}, name
            else:
                assert namespace in {"minecraft", "sgjourney", "sgjatlantis_cityshield"}, name
            converted[pos] = state, convert_entity(raw, neo.get(pos))
        for x, y, z in ((-3876, 413, 5885), (-3899, 418, 5876), (-3899, 418, 5873)):
            if (x // 16, z // 16) == (cx, cz):
                pos = (x % 16, y, z % 16)
                if pos in neo:
                    converted[pos] = neo[pos]
        output = templates / file.name
        manifest_lines[cx, cz] = save(output, converted, cx, cz)
        assert template(output, cx, cz) == converted, file.name
        map_name = file.with_suffix(".txt").name
        (wipe / map_name).write_text(wipe_spans(converted), encoding="utf-8")
        source_space = FORGE.parents[1] / spaces.name / map_name
        if source_space.exists():
            lines = []
            for line in source_space.read_text(encoding="utf-8").splitlines():
                x, z, low, high, fluid = map(int, line.split())
                start = low
                for y in range(low, high + 1):
                    if (x, y, z) in converted:
                        if start < y:
                            lines.append(f"{x} {z} {start} {y - 1} {fluid}")
                        start = y + 1
                if start <= high:
                    lines.append(f"{x} {z} {start} {high} {fluid}")
            (spaces / map_name).write_text("\n".join(lines) + "\n", encoding="utf-8")

    assert len(manifest_lines) == 82
    for x, y, z in ((-3830, 299, 5756), (-3885, 256, 5885), (-3937, 264, 5973), (-4090, 252, 5546)):
        cx, cz = x // 16, z // 16
        name = f"chunk_{cx}_{cz}.nbt"
        root = templates if (cx, cz) in manifest_lines else NEO
        state, raw = template(root / name, cx, cz)[x % 16, y, z % 16]
        assert state["Name"] == "sgjadditions_capacity_patch:atlantis_transporter_panel"
        print((x, y, z), nbt.Reader(raw).payload(10)["StationName"])
    manifest = NEO.parents[1] / "atlantis_city_manifest.txt"
    header, *rows = manifest.read_text(encoding="utf-8").splitlines()
    keys = [tuple(map(int, row.split()[:2])) for row in rows]
    assert len(set(keys)) == 2009 and manifest_lines.keys() <= set(keys)
    (destination / manifest.name).write_text(
        header + "\n" + "\n".join(manifest_lines.get(key, row) for key, row in zip(keys, rows)) + "\n",
        encoding="utf-8", newline="\n")
    print("Verified 82 converted templates and 2,009 manifest rows.")


if __name__ == "__main__":
    stage(Path(sys.argv[1]))
