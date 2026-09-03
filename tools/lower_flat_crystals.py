"""Halve only the six flat-crystal clearances in the accepted hotfix3 release.

Usage: python3 lower_flat_crystals.py HOTFIX3_JAR HOTFIX3_SOURCE_ZIP OUTPUT_DIR
"""
import copy
import hashlib
import json
from pathlib import Path
import sys
import zipfile

OLD = '2.0.2-beta.2-hotfix3'
NEW = '2.0.2-beta.2-hotfix4'
MODELS = [
    'assets/sgjadditions_capacity_patch/models/custom/atlantisdhd_controls.json',
    'assets/sgjadditions/models/custom/atlantisdhd_controls.json',
    'resourcepacks/sgjatlantis_sgjadditions_override/assets/sgjadditions/models/custom/atlantisdhd_controls.json',
]
NETWORK = 'com/mustangdoc/sgjpatch/network/SGJPatchNetwork.class'


def encode(obj):
    return (json.dumps(obj, indent=2)+'\n').encode()


def main():
    jar_path, source_path, output = map(Path, sys.argv[1:])
    output.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(jar_path) as jar:
        originals = {n: jar.read(n) for n in jar.namelist() if not n.endswith('/')}
    with zipfile.ZipFile(source_path) as src:
        source = {n: src.read(n) for n in src.namelist() if not n.endswith('/')}
    runtime = dict(originals)
    changes = []
    for name in MODELS:
        model = json.loads(originals[name])
        before = copy.deepcopy(model)
        assert len(model['elements']) == 283
        indices = [i for i,e in enumerate(model['elements']) if any(f['texture']=='#2' for f in e['faces'].values())]
        assert indices == list(range(8,14)), indices
        for i in indices:
            e = model['elements'][i]
            old = before['elements'][i]
            rear = i < 11
            surface = 18.4 if rear else 17.4
            assert old['from'][1] == (19 if rear else 17.85)
            clearance = old['from'][1] - surface
            drop = clearance/2
            for endpoint in ('from','to'):
                e[endpoint][1] = round(e[endpoint][1]-drop,6)
            assert abs((e['from'][1]-surface)-clearance/2) < 1e-9
            assert e['from'][1] > surface
            assert abs(e['to'][1]-e['from'][1]-.25)<1e-9
            restored = copy.deepcopy(e)
            restored['from'][1], restored['to'][1] = old['from'][1], old['to'][1]
            assert restored == old  # UVs, X/Z, thickness and faces are untouched.
            if name == MODELS[0]:
                changes.append({'index':i,'row':'rear' if rear else 'front',
                                'from':old['from'],'to':old['to'], 'drop':round(drop,6),
                                'surface_y':surface, 'new_gap':round(clearance/2,6)})
        for i,e in enumerate(model['elements']):
            if i not in indices:
                assert e == before['elements'][i]
        check = copy.deepcopy(model); check['elements']=before['elements']
        assert check == before
        runtime[name] = encode(model)
        assert source['src/main/resources/'+name] == originals[name]
        source['src/main/resources/'+name] = runtime[name]

    # Keep release metadata and the existing exact-version check synchronized.
    for name in ('META-INF/mods.toml','META-INF/MANIFEST.MF',NETWORK):
        assert OLD.encode() in originals[name]
        if name == NETWORK:
            assert originals[name].count(OLD.encode()) == 1 and len(OLD)==len(NEW)
        runtime[name] = originals[name].replace(OLD.encode(), NEW.encode())
    old_description = f'Version {NEW} transfers the accepted city-shield legs, feet, side profile, and complete illuminated rear panel to the DHD. All tabletop controls, GUI, dialing logic, recipes, and compatibility code are unchanged.'
    description = f'Version {NEW} lowers the six flat crystals by half of their existing clearance above their respective table surfaces. Crystal sizes, horizontal positions, dialer controls, shield-style frame, GUI, and gameplay behavior are preserved.'
    runtime['META-INF/mods.toml'] = runtime['META-INF/mods.toml'].replace(old_description.encode(),description.encode())
    for name in ('gradle.properties','src/main/resources/META-INF/mods.toml',
                 'src/main/java/com/mustangdoc/sgjpatch/network/SGJPatchNetwork.java'):
        source[name] = source[name].replace(OLD.encode(),NEW.encode())
    source['src/main/resources/META-INF/mods.toml'] = source['src/main/resources/META-INF/mods.toml'].replace(old_description.encode(),description.encode())

    # Keep the editable assembled reference in agreement with the runtime.
    bb = json.loads(source['blockbench/atlantis_dhd_shield_frame_hotfix3.bbmodel'])
    bb['name'] = 'Atlantis DHD shield frame - half-gap crystals - hotfix4'
    controls = next(e for e in bb['elements'] if e['name']=='DHD_controls_unchanged')
    original_vertices = copy.deepcopy(controls['vertices'])
    for change in changes:
        lo,hi = change['from'],change['to']
        keys = [k for k,p in original_vertices.items() if all(lo[a]-1e-8<=p[a]<=hi[a]+1e-8 for a in range(3))]
        assert len(keys)==24,(change['index'],len(keys))
        for k in keys:
            controls['vertices'][k][1] = round(original_vertices[k][1]-change['drop'],6)
    source['blockbench/atlantis_dhd_shield_frame_hotfix4.bbmodel'] = encode(bb)
    source['tools/lower_flat_crystals.py'] = Path(__file__).read_bytes()
    source['CHANGELOG.md'] = (f'# {NEW}\n\n'
        '- Lowers all six flat crystals by half their previous clearance.\n'
        '- Rear-row gap: 0.60 to 0.30 model pixels; front-row gap: 0.45 to 0.225.\n'
        '- Preserves crystal thickness, horizontal positions, textures and all other geometry.\n\n').encode()+source['CHANGELOG.md']
    source['README-CRYSTAL-HEIGHT.md'] = (f'# {NEW}\n\n'
        'Based on the user-accepted hotfix3 JAR and source. Only the six flat-crystal\n'
        'Y positions and release identifiers change. The rear row moves down 0.30\n'
        'model pixels and the front row moves down 0.225; both keep half of their\n'
        'previous visible clearance above their respective deck surfaces.\n\n'
        'The shield-style sides, legs, feet, backlight, triangular dialer, GUI,\n'
        'recipes, and gameplay methods remain unchanged. The network version string\n'
        'is updated to enforce the same version on client and server.\n\n'
        'Runtime model JSONs and the new hotfix4 Blockbench reference are synchronized.\n'
        'Older model files/reports are retained as history, not current export inputs.\n'
        'No full Gradle rebuild or Minecraft runtime test was performed; this resource\n'
        'patch preserves the working class files except for the protocol version string.\n').encode()

    allowed = set(MODELS)|{'META-INF/mods.toml','META-INF/MANIFEST.MF',NETWORK}
    actual = {n for n in runtime if runtime[n] != originals[n]}
    assert actual == allowed
    assert runtime[NETWORK].replace(NEW.encode(),OLD.encode()) == originals[NETWORK]
    expected_metadata = source['src/main/resources/META-INF/mods.toml'].replace(b'${mod_version}',NEW.encode())
    assert expected_metadata == runtime['META-INF/mods.toml']
    result = {'version':NEW,'baseline_sha256':hashlib.sha256(jar_path.read_bytes()).hexdigest(),
              'crystals':changes,'changed_runtime_entries':sorted(actual),
              'all_other_runtime_entries_byte_identical':True,'in_game_tested':False}
    source['crystal-height-validation.json'] = encode(result)
    for suffix,data in [('.jar',runtime),('-source.zip',source)]:
        path = output/f'sgjatlantis-dhd-1.20.1-{NEW}{suffix}'
        with zipfile.ZipFile(path,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as archive:
            for n,b in data.items():
                archive.writestr(n,b)
        with zipfile.ZipFile(path) as archive:
            assert archive.testzip() is None
            assert all(archive.read(n)==b for n,b in data.items())
        print(path)
    print(json.dumps(result,indent=2))


if __name__ == '__main__':
    main()
