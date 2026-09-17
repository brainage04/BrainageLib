"""Independently reopen each final blend; verify it matches recorded geometry and assets."""
from pathlib import Path
import hashlib
import json
import sys
import bpy
ROOT = Path(__file__).resolve().parent
sys.path.insert(0,str(ROOT))
from author import CONFIGS, scene_records, digest, file_hash, bounds

checks = []
names = sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else list(CONFIGS)
for name in names:
    cfg = CONFIGS[name]
    meta = json.loads((ROOT/(name+'-metadata.json')).read_text())
    bpy.ops.wm.open_mainfile(filepath=str(ROOT/(name+'.blend')))
    scene = bpy.context.scene
    actual = {n:digest(r) for n,r in scene_records().items()}
    assert actual == meta['geometry_diff']['after_object_hashes'], name
    assert scene.render.engine=='CYCLES' and scene.cycles.device=='CPU'
    assert scene.render.compositor_device=='CPU' and scene.render.threads==2
    assert scene.cycles.samples==cfg['samples']
    assert (scene.render.resolution_x,scene.render.resolution_y,scene.render.resolution_percentage)==(1024,1024,100)
    packed_images = [im for im in bpy.data.images if im.source=='FILE']
    active_images = [im for im in packed_images if im.users]
    assert active_images and all(im.packed_file and im.packed_file.data for im in packed_images)
    packed = {im.name:hashlib.sha256(im.packed_file.data).hexdigest() for im in packed_images}
    assert packed=={r['name']:r['sha256'] for r in meta['packed_textures']}
    assert bpy.data.texts['author.py'].as_string()==(ROOT/'author.py').read_text()
    assert bpy.data.texts[name+'.py'].as_string()==(ROOT/(name+'.py')).read_text()
    for ext in ['blend','png']:
        assert file_hash(ROOT/(name+'.'+ext))==meta['hashes'][ext]
    if cfg['project']=='MagicCarpet':
        for record in meta['changes']['records']:
            obj = bpy.data.objects[record['object']]
            assert list(obj.location)==record['position']
            for scale,original in zip(obj.scale,record['original_scale']):
                assert abs(scale-original*cfg['scale'])<1e-6
            tex = next(n for n in obj.data.materials[0].node_tree.nodes if n.type=='TEX_IMAGE')
            assert hashlib.sha256(tex.image.packed_file.data).hexdigest()==file_hash(ROOT/'textures'/Path(record['texture']).name)
            assert tex.interpolation=='Closest'
            assert len(obj.data.vertices)==4 and len(obj.data.polygons)==1
    else:
        assert not any('plus' in o.name.lower() for o in scene.objects)
        assert not meta['geometry_diff']['changed_objects']
        assert meta['geometry_diff']['before_retained_sha256']==meta['geometry_diff']['after_retained_sha256']
    checks.append({'name':name,'saved_geometry_matches':True,'active_textures_all_packed':True,'packed_scripts_match':True,
                   'blender_version':bpy.app.version_string,'engine':scene.render.engine,'device':scene.cycles.device,
                   'samples':scene.cycles.samples,'object_count':len(actual),'packed_texture_count':len(packed_images),
                   'active_texture_count':len(active_images),'unused_retained_source_textures':[im.name for im in packed_images if not im.users]})
(ROOT/'saved-scene-verification.json').write_text(json.dumps(checks,indent=2)+'\n')
print(json.dumps(checks,indent=2))
