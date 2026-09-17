"""Sequential CPU-only launcher. Uses nix shell if Blender was garbage-collected."""
from pathlib import Path
import os
import subprocess
import sys

ROOT = Path(__file__).resolve().parent
NAMES = ['magic-carpet-01-enchant','magic-carpet-02-end-rod','magic-carpet-03-glyphs','brainage-lib-01-matched','brainage-lib-02-refined']
BLENDER = Path('/nix/store/j57qch19aq4rs4fg728vlilgb0dcxs7z-blender-5.1.1/bin/blender')
names = sys.argv[1:] or NAMES
assert all(name in NAMES for name in names)
environment = dict(os.environ)
for key in ['DISPLAY','WAYLAND_DISPLAY','CUDA_VISIBLE_DEVICES','HIP_VISIBLE_DEVICES','ROCR_VISIBLE_DEVICES']:
    environment[key] = ''
for key in ['OMP_NUM_THREADS','OPENBLAS_NUM_THREADS']:
    environment[key] = '2'
command = [str(BLENDER)] if BLENDER.exists() else ['nix','shell','nixpkgs#blender','--command','blender']
for name in names:
    with (ROOT/(name+'.log')).open('w') as log:
        subprocess.run(command+['--background','-noaudio','--threads','2','--python-exit-code','1','--python',str(ROOT/(name+'.py'))],env=environment,cwd=ROOT,stdout=log,stderr=subprocess.STDOUT,check=True)
    print('COMPLETE '+name,flush=True)
