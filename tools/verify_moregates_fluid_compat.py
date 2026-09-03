#!/usr/bin/env python3
import json
import sys
import zipfile
from pathlib import Path


PREFIX = "data/moregates/recipes/crystallizing/"
EXPECTED_FLUID = {"id": "sgjourney:liquid_naquadah", "amount": 100}


def main() -> int:
    if len(sys.argv) != 3:
        raise SystemExit("usage: verify_moregates_fluid_compat.py MOREGATES_JAR RESOURCES_DIR")

    moregates_jar = Path(sys.argv[1])
    resources_dir = Path(sys.argv[2])
    checked = 0

    with zipfile.ZipFile(moregates_jar) as archive:
        names = sorted(
            name for name in archive.namelist() if name.startswith(PREFIX) and name.endswith(".json")
        )
        if len(names) != 30:
            raise RuntimeError(f"expected 30 source recipes, found {len(names)}")

        for name in names:
            original = json.loads(archive.read(name))
            patched = json.loads((resources_dir / name).read_text(encoding="utf-8"))

            expected_conditions = [{"type": "forge:mod_loaded", "modid": "moregates"}]
            if patched.pop("conditions", None) != expected_conditions:
                raise RuntimeError(f"unexpected conditions: {name}")
            if patched.get("type") != "sgjourney:crystallizing":
                raise RuntimeError(f"recipe type is hidden from fluid unifiers: {name}")
            actual_fluid = patched.pop("input_fluid", None)
            if actual_fluid != EXPECTED_FLUID:
                raise RuntimeError(f"unexpected input fluid: {name}: {actual_fluid!r}")
            if patched != original:
                raise RuntimeError(f"recipe changed beyond input_fluid: {name}")
            checked += 1

    print(f"verified {checked} visible SGJourney recipes; only condition and input_fluid were added")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
