"""Inset only the dialer backplate's UVs in the accepted hotfix4 release.

Usage: python3 fix_backplate_uv.py HOTFIX4_JAR HOTFIX4_SOURCE_ZIP OUTPUT_DIR
"""
import copy
import hashlib
import io
import json
from pathlib import Path
import sys
import zipfile

from PIL import Image

OLD = '2.0.2-beta.2-hotfix4'
NEW = '2.0.2-beta.2-hotfix5'
MODELS = [
    'assets/sgjadditions_capacity_patch/models/custom/atlantisdhd_controls.json',
    'assets/sgjadditions/models/custom/atlantisdhd_controls.json',
    'resourcepacks/sgjatlantis_sgjadditions_override/assets/sgjadditions/models/custom/atlantisdhd_controls.json',
]
NETWORK = 'com/mustangdoc/sgjpatch/network/SGJPatchNetwork.class'
PLATE_INDICES = [1, 2, 3, 4, 5] + list(range(256, 272))
SAFE_UV = [4, 4, 12, 12]


def encode(obj):
    return (json.dumps(obj, indent=2) + '\n').encode()


def read_archive(path):
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None
        return {n: archive.read(n) for n in archive.namelist() if not n.endswith('/')}


def main():
    jar_path, source_path, output = map(Path, sys.argv[1:])
    output.mkdir(parents=True, exist_ok=True)
    originals = read_archive(jar_path)
    source = read_archive(source_path)
    runtime = dict(originals)
    changes = []

    for name in MODELS:
        model = json.loads(originals[name])
        before = copy.deepcopy(model)
        assert len(model['elements']) == 283
        namespace, texture = model['textures']['5'].split(':')
        assert texture == 'block/brown_plate'
        texture_name = f'assets/{namespace}/textures/{texture}.png'
        with Image.open(io.BytesIO(originals[texture_name])) as image:
            assert image.size == (16, 16)
            assert image.convert('RGBA').getcolors() == [(256, (112, 57, 47, 255))]

        indices = [i for i, e in enumerate(model['elements'])
                   if any(f['texture'] == '#5' for f in e['faces'].values())]
        assert indices == PLATE_INDICES, indices
        face_count = 0
        for i in indices:
            element = model['elements'][i]
            assert len(element['faces']) == 6
            for direction, face in element['faces'].items():
                assert face['texture'] == '#5'
                face['uv'] = list(SAFE_UV)
                # Only UV sampling changes: keep rotations, culling, tint and geometry.
                restored = dict(face)
                restored['uv'] = before['elements'][i]['faces'][direction]['uv']
                assert restored == before['elements'][i]['faces'][direction]
                face_count += 1
        assert face_count == 126
        restored = copy.deepcopy(model)
        for i in indices:
            for direction in restored['elements'][i]['faces']:
                restored['elements'][i]['faces'][direction]['uv'] = before['elements'][i]['faces'][direction]['uv']
        assert restored == before
        # Retain the exact accepted half-gap flat-crystal positions.
        assert [model['elements'][i]['from'][1] for i in range(8, 14)] == [18.7]*3 + [17.625]*3
        runtime[name] = encode(model)
        assert source['src/main/resources/' + name] == originals[name]
        source['src/main/resources/' + name] = runtime[name]
        changes.append({'model': name, 'plate_elements': indices, 'faces': face_count,
                        'old_uv_min': min(min(f['uv']) for i in indices for f in before['elements'][i]['faces'].values()),
                        'new_uv': SAFE_UV, 'geometry_unchanged': True})

    # Match the existing exact-version handshake; no method instructions change.
    for name in ('META-INF/mods.toml', 'META-INF/MANIFEST.MF', NETWORK):
        assert OLD.encode() in originals[name]
        if name == NETWORK:
            assert len(OLD) == len(NEW) and originals[name].count(OLD.encode()) == 1
        runtime[name] = originals[name].replace(OLD.encode(), NEW.encode())
    old_description = f'Version {NEW} lowers the six flat crystals by half of their existing clearance above their respective table surfaces. Crystal sizes, horizontal positions, dialer controls, shield-style frame, GUI, and gameplay behavior are preserved.'
    description = f'Version {NEW} insets the triangular dialer backplate UVs to prevent edge sampling into neighboring atlas textures. All geometry, crystal heights, buttons, shield-style frame, GUI, and gameplay behavior are preserved.'
    assert old_description.encode() in runtime['META-INF/mods.toml']
    runtime['META-INF/mods.toml'] = runtime['META-INF/mods.toml'].replace(old_description.encode(), description.encode())
    for name in ('gradle.properties', 'src/main/resources/META-INF/mods.toml',
                 'src/main/java/com/mustangdoc/sgjpatch/network/SGJPatchNetwork.java'):
        assert OLD.encode() in source[name]
        source[name] = source[name].replace(OLD.encode(), NEW.encode())
    source['src/main/resources/META-INF/mods.toml'] = source['src/main/resources/META-INF/mods.toml'].replace(old_description.encode(), description.encode())

    # Synchronize the editable assembled reference without moving any vertex.
    bb = json.loads(source['blockbench/atlantis_dhd_shield_frame_hotfix4.bbmodel'])
    before_bb = copy.deepcopy(bb)
    bb['name'] = 'Atlantis DHD shield frame - inset backplate UVs - hotfix5'
    texture_id = next(i for i, t in enumerate(bb['textures']) if t['name'] == 'block/brown_plate.png')
    controls = next(e for e in bb['elements'] if e['name'] == 'DHD_controls_unchanged')
    faces = [f for f in controls['faces'].values() if f['texture'] == texture_id]
    assert len(faces) == 126
    for face in faces:
        old_uvs = copy.deepcopy(face['uv'])
        for axis in (0, 1):
            lo = min(v[axis] for v in old_uvs.values())
            hi = max(v[axis] for v in old_uvs.values())
            for key in face['uv']:
                face['uv'][key][axis] = round(4 + 8*(old_uvs[key][axis]-lo)/(hi-lo), 8) if hi > lo else 8
    restored_bb = copy.deepcopy(bb)
    restored_bb['name'] = before_bb['name']
    for new_element, old_element in zip(restored_bb['elements'], before_bb['elements']):
        for key, face in new_element['faces'].items():
            if face['texture'] == texture_id:
                face['uv'] = old_element['faces'][key]['uv']
    assert restored_bb == before_bb
    source['blockbench/atlantis_dhd_shield_frame_hotfix5.bbmodel'] = encode(bb)
    source['tools/fix_backplate_uv.py'] = Path(__file__).read_bytes()
    source['CHANGELOG.md'] = (f'# {NEW}\n\n'
        '- Insets all 126 dialer-backplate face UVs into the solid-brown texture interior to address white edge bleed.\n'
        '- Changes no geometry or button positions; preserves hotfix4 half-gap crystal heights.\n\n').encode() + source['CHANGELOG.md']
    source['README-BACKPLATE-UV.md'] = (f'# {NEW}\n\n'
        'Based on the accepted hotfix4 JAR and source. The dedicated 16x16 backplate\n'
        'texture is uniformly opaque brown. Its 21 small cuboids previously used UVs\n'
        'starting at zero, including very thin border faces. All 126 faces now sample\n'
        'the interior rectangle [4,4,12,12], leaving a four-pixel texture-edge margin.\n'
        'This addresses atlas-edge sampling without changing shape, position, color,\n'
        'face rotation or culling. It does not modify any triangular dialer button.\n\n'
        'All three runtime/compatibility model variants and the new hotfix5 Blockbench\n'
        'reference are synchronized. Runtime resource JSONs are authoritative. Older\n'
        'model files and reports are retained as history, not current export inputs.\n'
        'The accepted lowered flat crystals and shield-style frame are unchanged.\n\n'
        'Validation: all non-UV model properties unchanged; all other resource bytes\n'
        'unchanged; matching source/runtime resources; ZIP integrity checks. Existing\n'
        'working classes are preserved except for the exact-match network version\n'
        'string. No full Gradle rebuild or Minecraft runtime test was performed.\n\n'
        'Install this JAR instead of hotfix4, not alongside it. Update the server too\n'
        'where applicable, since the existing protocol requires matching versions.\n').encode()

    allowed = set(MODELS) | {'META-INF/mods.toml', 'META-INF/MANIFEST.MF', NETWORK}
    actual = {n for n in runtime if runtime[n] != originals[n]}
    assert actual == allowed
    assert runtime[NETWORK].replace(NEW.encode(), OLD.encode()) == originals[NETWORK]
    assert source['src/main/resources/META-INF/mods.toml'].replace(b'${mod_version}', NEW.encode()) == runtime['META-INF/mods.toml']
    result = {'version': NEW, 'baseline_sha256': hashlib.sha256(jar_path.read_bytes()).hexdigest(),
              'changes': changes, 'changed_runtime_entries': sorted(actual),
              'all_geometry_and_buttons_unchanged': True,
              'all_other_runtime_entries_byte_identical': True, 'in_game_tested': False}
    source['backplate-uv-validation.json'] = encode(result)
    for suffix, data in [('.jar', runtime), ('-source.zip', source)]:
        path = output / f'sgjatlantis-dhd-1.20.1-{NEW}{suffix}'
        assert not path.exists(), f'Refusing to overwrite {path}'
        with zipfile.ZipFile(path, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
            for name, contents in data.items():
                archive.writestr(name, contents)
        assert read_archive(path) == data
        print(path)
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
