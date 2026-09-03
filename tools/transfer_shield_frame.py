"""Model-only transfer from the accepted city-shield JAR to the supplied DHD JAR.

Run from this directory after extracting the inputs into dhd/, shield/, source/.
No Java bytecode, GUI assets, button positions, recipes or blockstate are changed.
"""
import copy
import hashlib
import json
import math
from pathlib import Path
import shutil
import zipfile

ROOT = Path(__file__).resolve().parent
DHD = ROOT / 'dhd'
SHIELD = ROOT / 'shield/assets/sgjatlantis_cityshield'
SOURCE = ROOT / 'source'
OUT = ROOT / 'output'
NS = 'sgjadditions_capacity_patch'
VERSION = '2.0.2-beta.2-hotfix3'
DONOR = json.loads((SHIELD / 'models/block/shield_controller_body.json').read_text())
# Preserve the exact accepted joints by applying the same affine transform to
# feet, sockets, continuous legs and side covers. No independent leg yaw edits.
SCALE = [29 / (20.26481 + 4.26481), 17.4 / 16.74371,
         (15.12 - 0.25) / (12.29055 - 0.14102)]
OFFSET = [16 - 8 * SCALE[0], 0, 0.25 - 0.14102 * SCALE[2]]
PARTS = [0, 1, 2, 3, 4, 5, 9, 10, 12, 13, 14, 15, 16, 17, 18,
         25, 26, 27, 28, 85, 86]
REMOVE = {0, 1, 2, 3, 5, 6, 7, 290, 291}
FACE_VERTS = {
    'down': [4, 0, 1, 5], 'up': [2, 6, 7, 3],
    'north': [3, 1, 0, 2], 'south': [6, 4, 5, 7],
    'west': [2, 0, 4, 6], 'east': [7, 5, 1, 3],
}


def transform(p):
    return [p[i] * SCALE[i] + OFFSET[i] for i in range(3)]


def cube_faces(e, textures):
    lo, hi = e['from'], e['to']
    points = [[(hi if n & (1 << i) else lo)[i] for i in range(3)] for n in range(8)]
    r = e.get('rotation')
    if r:
        a = 'xyz'.index(r['axis'])
        u, v = (a + 1) % 3, (a + 2) % 3
        c, s = math.cos(math.radians(r['angle'])), math.sin(math.radians(r['angle']))
        origin = r['origin']
        for p in points:
            x, y = p[u] - origin[u], p[v] - origin[v]
            p[u], p[v] = origin[u] + c * x - s * y, origin[v] + s * x + c * y
    for direction, f in e['faces'].items():
        tex = f['texture']
        while tex.startswith('#'):
            tex = textures[tex[1:]]
        uv = f.get('uv', [0, 0, 16, 16])
        # Vanilla face order, with UV rotation applied around the face.
        coords = [[uv[0], uv[1]], [uv[0], uv[3]], [uv[2], uv[3]], [uv[2], uv[1]]]
        shift = int(f.get('rotation', 0)) // 90
        coords = coords[shift:] + coords[:shift]
        yield {'points': [points[i] for i in FACE_VERTS[direction]], 'uv': coords,
               'texture': tex, 'direction': direction}


def remap_groups(nodes, mapping):
    result = []
    for node in nodes:
        if isinstance(node, int):
            if node in mapping:
                result.append(mapping[node])
        else:
            node = copy.deepcopy(node)
            node['children'] = remap_groups(node.get('children', []), mapping)
            result.append(node)
    return result


def save_json(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n')


def main():
    OUT.mkdir(exist_ok=True)
    donor_faces = []
    for i in PARTS:
        for face in cube_faces(DONOR['elements'][i], DONOR['textures']):
            face['points'] = [transform(p) for p in face['points']]
            face['part'] = f'shield_part_{i}'
            donor_faces.append(face)

    vertices, uv = [], []
    for line in (SHIELD / 'models/block/shield_controller_solid_legs.obj').read_text().splitlines():
        fields = line.split()
        if not fields:
            continue
        if fields[0] == 'v':
            vertices.append(transform([float(v) * 16 for v in fields[1:4]]))
        elif fields[0] == 'vt':
            uv.append([float(fields[1]) * 16, (1 - float(fields[2])) * 16])
        elif fields[0] == 'f':
            ids = [list(map(int, f.split('/'))) for f in fields[1:]]
            donor_faces.append({'points': [vertices[i[0]-1] for i in ids],
                                'uv': [uv[i[1]-1] for i in ids],
                                'texture': DONOR['textures']['shell'], 'part': 'solid_leg'})

    # Every copied material lives inside the DHD mod; no city-shield dependency.
    tex_map = {}
    for face in donor_faces:
        original = face['texture']
        name = original.split('/')[-1]
        target = f'{NS}:block/shield_frame/{name}'
        tex_map[original] = target
        face['texture'] = target
        src = SHIELD / f'textures/block/{name}.png'
        dest = DHD / f'assets/{NS}/textures/block/shield_frame/{name}.png'
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(src, dest)

    mats = {t: f'material_{i}' for i, t in enumerate(sorted(tex_map.values()))}
    obj = ['# Shield frame fitted to the DHD; positions in block units.',
           'mtllib atlantis_shield_frame.mtl']
    for i, face in enumerate(donor_faces):
        obj += [f'g {face["part"]}_{i}', f'usemtl {mats[face["texture"]]}']
        obj += ['v ' + ' '.join(f'{v / 16:.9f}' for v in p) for p in face['points']]
        obj += ['vt ' + ' '.join(f'{v / 16:.9f}' for v in p) for p in face['uv']]
        obj += ['f ' + ' '.join(f'{4*i+j}/{4*i+j}' for j in range(1, 5))]
    mtl = []
    for t, name in mats.items():
        mtl += [f'newmtl {name}', 'Kd 1 1 1',
                'Ka 1 1 1' if t.endswith('/atlantis_prop_lightbar') else 'Ka 0 0 0',
                f'map_Kd {t}', '']
    modeldir = DHD / f'assets/{NS}/models/custom'
    (modeldir / 'atlantis_shield_frame.obj').write_text('\n'.join(obj) + '\n')
    (modeldir / 'atlantis_shield_frame.mtl').write_text('\n'.join(mtl) + '\n')

    variants = [('assets/sgjadditions_capacity_patch', NS),
                ('assets/sgjadditions', 'sgjadditions'),
                ('resourcepacks/sgjatlantis_sgjadditions_override/assets/sgjadditions', 'sgjadditions')]
    preserved_count = None
    preview_faces = None
    original = ROOT.parent / 'upload/sgjatlantis-dhd-1.20.1-2.0.2-beta.2-hotfix2(1).jar'
    for prefix, namespace in variants:
        custom = DHD / prefix / 'models/custom/atlantisdhd.json'
        with zipfile.ZipFile(original) as original_jar:
            model = json.loads(original_jar.read(f'{prefix}/models/custom/atlantisdhd.json'))
        # Refuse to silently apply index-based surgery to an unknown baseline.
        assert len(model['elements']) == 292
        assert model['elements'][7]['from'] == [0, 17, 14.25]
        block = json.loads((DHD / prefix / 'models/block/atlantisdhd.json').read_text())
        textures = {**block['textures'], **model.get('textures', {})}
        retained = [(i, e) for i, e in enumerate(model['elements']) if i not in REMOVE]
        body = copy.deepcopy(model)
        body['elements'] = [e for _, e in retained]
        body['groups'] = remap_groups(body.get('groups', []), {old: new for new, (old, _) in enumerate(retained)})
        body['textures'] = textures
        body['render_type'] = 'solid'
        # Keep the tabletop/dialer as standard elements, not re-meshed controls.
        save_json(DHD / prefix / 'models/custom/atlantisdhd_controls.json', body)
        composite = {'loader': 'forge:composite', 'ambientocclusion': True,
                     'textures': textures, 'display': model['display'],
                     'children': {
                         'controls': {'parent': f'{namespace}:custom/atlantisdhd_controls'},
                         'shield_frame': {'loader': 'forge:obj',
                                          'model': f'{NS}:models/custom/atlantis_shield_frame.obj',
                                          'flip_v': False, 'automatic_culling': False,
                                          'shade_quads': True, 'emissive_ambient': True,
                                          'render_type': 'solid'}}}
        save_json(custom, composite)
        assert all(body['elements'][n] == model['elements'][old] for n, (old, _) in enumerate(retained))
        preserved_count = len(retained)
        if prefix == 'assets/sgjadditions_capacity_patch':
            preview_faces = [f for e in body['elements'] for f in cube_faces(e, textures)] + donor_faces

    save_json(OUT / 'preview-mesh.json', preview_faces)
    # Runtime/source parity for every changed model and bundled material.
    original = ROOT.parent / 'upload/sgjatlantis-dhd-1.20.1-2.0.2-beta.2-hotfix2(1).jar'
    changed = []
    with zipfile.ZipFile(original) as jar:
        for p in sorted(DHD.rglob('*')):
            if not p.is_file():
                continue
            name = p.relative_to(DHD).as_posix()
            if name in ('META-INF/mods.toml', 'META-INF/MANIFEST.MF', 'com/mustangdoc/sgjpatch/network/SGJPatchNetwork.class'):
                continue
            if name not in jar.namelist() or p.read_bytes() != jar.read(name):
                assert name.startswith(('assets/', 'resourcepacks/'))
                dest = SOURCE / 'src/main/resources' / name
                dest.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(p, dest)
                changed.append(name)
    # The source and JAR metadata are updated together; class bytes stay exact.
    for path in [DHD / 'META-INF/mods.toml', DHD / 'META-INF/MANIFEST.MF', SOURCE / 'src/main/resources/META-INF/mods.toml', SOURCE / 'gradle.properties', SOURCE / 'src/main/java/com/mustangdoc/sgjpatch/network/SGJPatchNetwork.java']:
        text = path.read_text().replace('2.0.2-beta.2-hotfix2', VERSION)
        if path.name == 'mods.toml':
            start = text.find(f'Version {VERSION} makes')
            if start >= 0:
                end = text.index('\n\n', start)
                text = text[:start] + f'Version {VERSION} transfers the accepted city-shield legs, feet, side profile, and complete illuminated rear panel to the DHD. All tabletop controls, GUI, dialing logic, recipes, and compatibility code are unchanged.' + text[end:]
        path.write_text(text)
    # Preserve exact-release network enforcement. Only the UTF-8 protocol
    # constant changes; method bytecode and packet handling remain identical.
    network = 'com/mustangdoc/sgjpatch/network/SGJPatchNetwork.class'
    with zipfile.ZipFile(original) as baseline:
        original_class = baseline.read(network)
    old_protocol = b'2.0.2-beta.2-hotfix2'
    assert len(old_protocol) == len(VERSION.encode()) and original_class.count(old_protocol) == 1
    (DHD/network).write_bytes(original_class.replace(old_protocol, VERSION.encode()))
    shutil.copyfile(Path(__file__), SOURCE / 'tools/transfer_shield_frame.py')
    report = {'version': VERSION, 'removed_dhd_elements': sorted(REMOVE),
              'preserved_dhd_elements': preserved_count, 'donor_elements': PARTS,
              'donor_continuous_legs': 2, 'frame_quads': len(donor_faces),
              'scale': SCALE, 'offset': OFFSET, 'changed_resources': changed,
              'backlight_front_z': 15.12, 'rear_crystal_max_z': 15,
              'java_classes_changed': 1, 'class_change': 'Network protocol version string only; all method bytecode unchanged.',
              'note': 'Backlight is an emissive model surface; no new power-detection behavior.'}
    save_json(SOURCE / 'shield-frame-transfer-report.json', report)
    print(json.dumps(report, indent=2))


if __name__ == '__main__':
    main()
