"""Decode every PNG and validate geometry-projected composition, then write manifest."""
from pathlib import Path
from PIL import Image, ImageStat
import colorsys
import hashlib
import json

ROOT = Path(__file__).resolve().parent
NAMES = ['magic-carpet-01-enchant','magic-carpet-02-end-rod','magic-carpet-03-glyphs','brainage-lib-01-matched','brainage-lib-02-refined']
checks = []
manifest = []
probes = json.loads((ROOT/'sources/carpet-color-probes.json').read_text())
color_buckets = {
    'basic': {'red':lambda h,s,v:s>.32 and (h<.065 or h>.95),'gold':lambda h,s,v:s>.4 and .065<h<.19},
    'advanced': {'charcoal':lambda h,s,v:s<.38 and .055<v<.67,'orange':lambda h,s,v:s>.5 and .025<h<.14},
    'legendary': {'purple':lambda h,s,v:s>.32 and .67<h<.88,'lime':lambda h,s,v:s>.48 and .17<h<.39},
}
for name in NAMES:
    meta = json.loads((ROOT/(name+'-metadata.json')).read_text())
    with Image.open(ROOT/(name+'.png')) as im:
        im.load()
        assert im.format=='PNG' and im.mode=='RGBA' and im.size==(1024,1024)
        assert im.getchannel('A').getextrema()==(255,255)
        assert min(ImageStat.Stat(im.convert('RGB')).stddev)>20
        corners = ImageStat.Stat(im.crop((0,0,80,80)).convert('RGB')).mean
        assert max(corners)<70
        check = {'name':name,'PNG_decoded':True,'resolution':[1024,1024],'opaque':True,'dark_corner_mean_RGB':corners}
        if meta['project']=='MagicCarpet':
            colors = {}
            for tier in probes:
                counts = {key:0 for key in color_buckets[tier['tier']]}
                for x,y in tier['visible_top_samples_normalized']:
                    pixel = (min(1023,int(x*1024)),min(1023,int((1-y)*1024)))
                    rgb = im.getpixel(pixel)[:3]
                    hsv = colorsys.rgb_to_hsv(*(c/255 for c in rgb))
                    for key,predicate in color_buckets[tier['tier']].items():
                        counts[key] += int(predicate(*hsv))
                assert min(counts.values())>=4, (name,tier['tier'],counts)
                colors[tier['tier']] = counts
            check['visible_three_tier_source_color_counts'] = colors
            notes = f"Approved blender-j/magic-carpet-04-icon-glow composition/glow preserved. 60 original particle centers and billboard rotations; sizes multiplied by {meta['changes']['size_factor']:g}; exact vanilla assets: "+', '.join(meta['changes']['textures_used'])+'. Original alpha unchanged, Closest filtering, approved tier tint/emission retained. Placement is artistic, not an in-game particle capture.'
            source = 'Approved packed blender-j/magic-carpet-04-icon-glow.blend; real MagicCarpet repository entity textures and model; Minecraft 26.2 client-jar particle textures (asset-provenance.json).'
        else:
            pixels = im.get_flattened_data()
            green = sum(1 for r,g,b,a in pixels if g>90 and g>r*1.35 and g>b*1.2)
            assert green==0, (name,green)
            yellow = [(i%1024,i//1024) for i,(r,g,b,a) in enumerate(pixels) if r>100 and g>90 and b<r*.7 and b<g*.7]
            yellow_bounds = [min(p[0] for p in yellow),min(p[1] for p in yellow),max(p[0] for p in yellow),max(p[1] for p in yellow)]
            assert 130<yellow_bounds[0]<160 and 90<yellow_bounds[1]<120 and 860<yellow_bounds[2]<900 and 920<yellow_bounds[3]<950
            check.update(saturated_green_plus_pixels=green,head_yellow_bounds_pixels=yellow_bounds,retained_geometry_identical=meta['geometry_diff']['retained_geometry_identical'])
            notes = 'Exact approved NPCAddons head-2 packed scene minus the two plus bars and their empty parent. Removed plus depth 0.06 block. NMSR-equivalent yaw +45°, elevation 35.264389682754654°. All retained geometry/UVs/transforms, supplied skin, materials, camera, lights, world and compositor unchanged, independently verified by saved-blend geometry hashes. Candidate 1 retains approved 64 samples; candidate 2 changes only maximum samples to 128.'
            source = 'Approved blender-h/npc-addons-head-2.blend; supplied skin /home/thomas/Downloads/6b252bfa5cbcdd98.png, SHA256 e9ebbeece495d9c96040e235dc865fdb1a530cf6a2243a6c8fcec22e72f03e3f, packed unchanged.'
        assert hashlib.sha256((ROOT/(name+'.png')).read_bytes()).hexdigest()==meta['hashes']['png']
        checks.append(check)
        manifest.append({'project':meta['project'],'label':meta['label'],'path':'blender-n/'+name+'.png',
                         'method':f"Blender {meta['blender_version']}; headless CLI -noaudio; Cycles CPU; {meta['samples']} maximum samples (adaptive sampling {meta['adaptive_sampling']}, threshold {meta['adaptive_threshold']:.5g}); 2 threads; 1024×1024 PNG; render duration {meta['render_duration_seconds']:.6f} seconds.",
                         'source':source,'notes':notes+f' Script {name}.py + author.py and packed {name}.blend adjacent; full metadata {name}-metadata.json.'})
(ROOT/'png-verification.json').write_text(json.dumps(checks,indent=2)+'\n')
(ROOT/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
(ROOT/'blockers.json').write_text('[]\n')
print(json.dumps(checks,indent=2))
