"""Local-only iteration 5. Run with Blender background -noaudio --python ENTRY.py.
Uses packed approved scenes unchanged except documented particle edits/plus removal.
Scripts are saved and hashed, never Git committed (explicit parent clarification).
"""
from pathlib import Path
import hashlib
import json
import math
import os
import sys
import time
import bpy
import numpy as np
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view

ROOT = Path(__file__).resolve().parent
CONFIGS = {
    'magic-carpet-01-enchant': {'project':'MagicCarpet', 'label':'Vanilla enchant glyphs + end rods · 1.65×', 'scale':1.65, 'mode':0, 'samples':96},
    'magic-carpet-02-end-rod': {'project':'MagicCarpet', 'label':'Vanilla end rods + enchant glyphs · 1.85×', 'scale':1.85, 'mode':1, 'samples':96},
    'magic-carpet-03-glyphs': {'project':'MagicCarpet', 'label':'Vanilla enchant glyphs + glints · 2×', 'scale':2.0, 'mode':2, 'samples':96},
    'brainage-lib-01-matched': {'project':'BrainageLib', 'label':'Approved NPCAddons head minus plus · matched 64 samples', 'samples':64},
    'brainage-lib-02-refined': {'project':'BrainageLib', 'label':'Approved NPCAddons head minus plus · refined 128 samples', 'samples':128},
}


def digest(value):
    return hashlib.sha256(json.dumps(value,sort_keys=True,separators=(',',':')).encode()).hexdigest()


def file_hash(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def matrix_values(matrix):
    return [list(row) for row in matrix]


def object_record(obj):
    record = {'type':obj.type, 'matrix_world':matrix_values(obj.matrix_world), 'parent':obj.parent.name if obj.parent else None,
              'hide_render':obj.hide_render, 'materials':[slot.material.name if slot.material else None for slot in obj.material_slots]}
    if obj.type == 'MESH':
        mesh = obj.data
        record['mesh'] = {'vertices':[list(v.co) for v in mesh.vertices], 'edges':[list(e.vertices) for e in mesh.edges],
                          'polygons':[[list(p.vertices),p.material_index,p.use_smooth] for p in mesh.polygons],
                          'uvs':{uv.name:[list(item.uv) for item in uv.data] for uv in mesh.uv_layers}}
    elif obj.type == 'CURVE':
        record['curve'] = {'bevel_depth':obj.data.bevel_depth, 'bevel_resolution':obj.data.bevel_resolution,
                           'splines':[{'type':s.type,'cyclic':s.use_cyclic_u,'points':[list(p.co) for p in s.points]} for s in obj.data.splines]}
    elif obj.type == 'CAMERA':
        record['camera'] = {key:getattr(obj.data,key) for key in ['type','ortho_scale','lens','sensor_width','clip_start','clip_end','shift_x','shift_y']}
    elif obj.type == 'LIGHT':
        record['light'] = {'type':obj.data.type,'energy':obj.data.energy,'color':list(obj.data.color),'size':getattr(obj.data,'size',None)}
    return record


def scene_records():
    bpy.context.view_layer.update()
    return {o.name:object_record(o) for o in bpy.context.scene.objects}


def node_records(tree):
    if not tree:
        return None
    nodes = []
    for n in tree.nodes:
        inputs = {}
        for socket in n.inputs:
            if hasattr(socket,'default_value'):
                value = socket.default_value
                if isinstance(value,(int,float,str,bool)):
                    inputs[socket.identifier] = value
                else:
                    try:
                        inputs[socket.identifier] = list(value)
                    except TypeError:
                        inputs[socket.identifier] = str(value)
        nodes.append({'name':n.name,'type':n.bl_idname,'inputs':inputs,
                      'image_sha256':hashlib.sha256(n.image.packed_file.data).hexdigest() if n.type=='TEX_IMAGE' and n.image and n.image.packed_file else None})
    return {'nodes':nodes,'links':sorted([f'{l.from_node.name}:{l.from_socket.identifier}>{l.to_node.name}:{l.to_socket.identifier}' for l in tree.links])}


def state_records():
    s = bpy.context.scene
    return {'materials':{m.name:node_records(m.node_tree) for m in bpy.data.materials if m.use_nodes},
            'world':node_records(s.world.node_tree),
            'compositor':node_records(s.compositing_node_group),
            'view':{k:getattr(s.view_settings,k) for k in ['view_transform','look','exposure','gamma']}}


def bounds(objects):
    scene = bpy.context.scene
    pts = [world_to_camera_view(scene,scene.camera,obj.matrix_world@v.co) for obj in objects for v in obj.data.vertices]
    return [min(p.x for p in pts),min(p.y for p in pts),max(p.x for p in pts),max(p.y for p in pts)]


def change_particles(cfg):
    particles = sorted([o for o in bpy.context.scene.objects if '-enchantment-' in o.name],key=lambda o:o.name)
    assert len(particles) == 60
    glyphs = 'abdefhkmnopr'
    materials = {}
    records = []
    for obj in particles:
        tier, index = obj.name.split('-enchantment-')
        j = int(index)
        if cfg['mode'] == 0:
            glyph = j % 5 < 3
            filename = f'sga_{glyphs[j%len(glyphs)]}.png' if glyph else 'glitter_6.png'
        elif cfg['mode'] == 1:
            filename = f'sga_{glyphs[j%len(glyphs)]}.png' if j%3==0 else ('glitter_6.png' if j%2 else 'glitter_4.png')
        else:
            filename = f'sga_{glyphs[j%len(glyphs)]}.png' if j%5!=4 else 'glint.png'
        key = (tier,filename)
        if key not in materials:
            mat = obj.data.materials[0].copy()
            mat.name = f'{tier} vanilla {Path(filename).stem}'
            tex = next(n for n in mat.node_tree.nodes if n.type == 'TEX_IMAGE')
            tex.image = bpy.data.images.load(str(ROOT/'textures'/filename),check_existing=True)
            tex.image.pack()
            tex.interpolation = 'Closest'
            materials[key] = mat
        original_width = max(v.co.x for v in obj.data.vertices)-min(v.co.x for v in obj.data.vertices)
        original_scale = list(obj.scale)
        original_matrix = matrix_values(obj.matrix_world)
        obj.data.materials.clear()
        obj.data.materials.append(materials[key])
        obj.scale *= cfg['scale']
        obj['vanilla_asset_path'] = 'assets/minecraft/textures/particle/'+filename
        obj['particle_size_factor'] = cfg['scale']
        obj['role'] = 'real-vanilla-particle-artistic-placement'
        records.append({'object':obj.name,'tier':tier,'texture':obj['vanilla_asset_path'],
                        'original_mesh_width_model_pixels':original_width,'original_scale':original_scale,
                        'original_matrix':original_matrix,'size_factor':cfg['scale'],
                        'new_width_model_pixels':original_width*cfg['scale'],
                        'new_width_blocks':original_width*cfg['scale']/16,
                        'position':list(obj.location)})
    bpy.context.view_layer.update()
    for obj,rec in zip(particles,records):
        rec['projected_bounds'] = bounds([obj])
        b = rec['projected_bounds']
        rec['projected_width_pixels'] = (b[2]-b[0])*1024
        assert b[0] > .005 and b[1] > .005 and b[2] < .995 and b[3] < .995, (obj.name,b)
        assert len(obj.data.vertices)==4 and len(obj.data.polygons)==1
        assert obj.visible_shadow is False
    return {'count':len(particles),'particles_per_tier':20,'size_factor':cfg['scale'],'records':records,
            'textures_used':sorted({r['texture'] for r in records}),
            'treatment':'Unmodified vanilla PNGs, Closest filtering and original alpha; existing approved RGB-to-luminance tier tint and emission strength 6 preserved. Camera-facing quads, artistic placement, not a captured in-game particle simulation.',
            'unchanged':'All 60 centers, billboard rotations, carpet geometry/UV/materials, tier spacing, camera, lights, luminous rims, backdrop and dense bloom.'}


def remove_plus():
    names = ['Plus horizontal bar','Plus vertical bar','GREEN PLUS on front top-right']
    plus = bpy.data.objects[names[0]]
    depth = max(v.co.y for v in plus.data.vertices)-min(v.co.y for v in plus.data.vertices)
    assert abs(depth-.06) < 1e-6
    for name in names:
        bpy.data.objects.remove(bpy.data.objects[name],do_unlink=True)
    head = bpy.data.objects['Supplied skin head']
    bpy.context.view_layer.update()
    local = head.matrix_world.inverted()@bpy.context.scene.camera.location
    yaw = math.degrees(math.atan2(local.x,-local.y))
    pitch = math.degrees(math.atan2(local.z,math.hypot(local.x,local.y)))
    assert abs(yaw+45)<1e-4 and abs(pitch-math.degrees(math.atan(1/math.sqrt(2))))<1e-4
    return {'removed_objects':names,'removed_plus_depth_blocks':depth,'nmsr_equivalent_yaw_degrees':-yaw,
            'camera_elevation_degrees':pitch,'preserved':'Every non-plus object, vertex, polygon, UV, world transform, parent, material, skin texel, camera, light, world, compositor and view transform.',
            'candidate_difference':'Candidate 1 preserves approved 64-sample setting; candidate 2 only raises maximum samples to 128. Both use original seed, denoising and framing.'}


def render(name):
    cfg = CONFIGS[name]
    assert bpy.app.background and '-noaudio' in sys.argv
    assert not os.environ.get('DISPLAY') and not os.environ.get('WAYLAND_DISPLAY')
    assert os.environ.get('CUDA_VISIBLE_DEVICES') == ''
    carpet = cfg['project'] == 'MagicCarpet'
    source = ROOT/'sources'/('magic-carpet-approved.blend' if carpet else 'npc-addons-approved.blend')
    bpy.ops.wm.open_mainfile(filepath=str(source))
    before = scene_records()
    state_before = state_records()
    changes = change_particles(cfg) if carpet else remove_plus()
    after = scene_records()
    state_after = state_records()
    removed = sorted(set(before)-set(after))
    added = sorted(set(after)-set(before))
    changed = sorted(n for n in set(before)&set(after) if before[n]!=after[n])
    if carpet:
        assert not removed and not added and changed==sorted(r['object'] for r in changes['records'])
        assert all(state_before['materials'][n]==state_after['materials'][n] for n in state_before['materials'])
    else:
        assert removed==sorted(changes['removed_objects']) and not added and not changed
        assert state_before == state_after
    for key in ['world','compositor','view']:
        assert state_before[key] == state_after[key]
    retained = sorted(set(before)&set(after)-set(changed))
    geometry_diff = {'removed_objects':removed,'added_objects':added,'changed_objects':changed,
                     'retained_objects':len(retained),'retained_geometry_identical':True,
                     'before_retained_sha256':digest({n:before[n] for n in retained}),
                     'after_retained_sha256':digest({n:after[n] for n in retained}),
                     'before_object_hashes':{n:digest(r) for n,r in before.items()},
                     'after_object_hashes':{n:digest(r) for n,r in after.items()},
                     'world_compositor_view_unchanged':True,'all_original_materials_unchanged':True}
    scene = bpy.context.scene
    subjects = [o for o in scene.objects if o.type=='MESH' and (o.get('tier') if carpet else not o.get('flat_backdrop'))]
    if carpet and not subjects:
        subjects = [o for o in scene.objects if o.type=='MESH' and '-enchantment-' not in o.name and 'floor' not in o.name.lower()]
    screen = bounds(subjects)
    assert min(screen)>.015 and max(screen)<.985, screen
    scene.render.engine = 'CYCLES'
    scene.cycles.device = 'CPU'
    scene.cycles.samples = cfg['samples']
    scene.render.compositor_device = 'CPU'
    scene.render.threads_mode = 'FIXED'
    scene.render.threads = 2
    scene.render.resolution_x = scene.render.resolution_y = 1024
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = 'PNG'
    scene.render.image_settings.color_mode = 'RGBA'
    scene.render.filepath = str(ROOT/(name+'.png'))
    bpy.context.preferences.filepaths.save_version = 0
    scene['reproducible_script'] = name+'.py + author.py'
    scene['iteration5_changes'] = json.dumps(changes)
    for text in list(bpy.data.texts):
        bpy.data.texts.remove(text)
    for filename in ['author.py',name+'.py']:
        text = bpy.data.texts.load(str(ROOT/filename))
        text.use_fake_user = True
    bpy.ops.file.pack_all()
    packed = [{'name':im.name,'sha256':hashlib.sha256(im.packed_file.data).hexdigest(),'bytes':len(im.packed_file.data)} for im in bpy.data.images if im.source=='FILE' and im.users]
    assert packed and all(r['bytes']>0 for r in packed)
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/(name+'.blend')))
    start = time.perf_counter()
    bpy.ops.render.render(write_still=True)
    duration = time.perf_counter()-start
    decoded = bpy.data.images.load(str(ROOT/(name+'.png')),check_existing=False)
    assert tuple(decoded.size)==(1024,1024)
    rgba = np.asarray(decoded.pixels[:],dtype=np.float32).reshape((1024,1024,4))
    assert np.isfinite(rgba).all() and rgba[:,:,:3].std()>.08 and rgba[:,:,3].min()>.99
    metadata = {'name':name,'project':cfg['project'],'label':cfg['label'],'source':str(source.relative_to(ROOT)),
                'blender_version':bpy.app.version_string,'engine':scene.render.engine,'device':scene.cycles.device,
                'samples':scene.cycles.samples,'adaptive_sampling':scene.cycles.use_adaptive_sampling,
                'adaptive_threshold':scene.cycles.adaptive_threshold,'seed':scene.cycles.seed,
                'render_duration_seconds':duration,'resolution':[1024,1024],'subject_screen_bounds':screen,
                'changes':changes,'geometry_diff':geometry_diff,'packed_textures':packed,
                'resources':{'display':None,'audio':None,'gpu':None,'background':True,'noaudio':True,'threads':2},
                'hashes':{'source_blend':file_hash(source),'script':file_hash(ROOT/'author.py'),'entrypoint':file_hash(ROOT/(name+'.py')),
                          'blend':file_hash(ROOT/(name+'.blend')),'png':file_hash(ROOT/(name+'.png'))},
                'png_opened':True,'decoded_pixel_standard_deviation':float(rgba[:,:,:3].std())}
    (ROOT/(name+'-metadata.json')).write_text(json.dumps(metadata,indent=2)+'\n')
    print('RENDER_COMPLETE '+json.dumps({'name':name,'seconds':duration,'blender':bpy.app.version_string,'engine':'CYCLES','device':'CPU','samples':cfg['samples']}),flush=True)


if __name__ == '__main__':
    names = sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else list(CONFIGS)
    for name in names:
        render(name)
