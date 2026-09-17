# BrainageLib icon

## What this is

`icon.png` — the BrainageLib mod icon, 1024 x 1024 PNG, SHA-256
`c7c3092727db19dadb2a53aa8fc05efeccba3a584f0432ab8959655986ca6b78`.

Copied byte-identically from `.local-icon-variants/provenance/from-round3/blender-n/brainage-lib-01-matched.png`
(SHA-256 verified at the source, and again on the copy). The hash matches `hashes.png` in
`provenance/brainage-lib-01-matched-metadata.json`.

## How it was made

Blender render, not a screenshot and not an output of the NMSR skin-render service.

* Blender 5.1.1, headless `--background -noaudio --threads 2`, Cycles CPU (no GPU), 64 samples
  (adaptive sampling, threshold 0.01), seed 0, 1024 x 1024, 27.539 s, denoising and framing from
  the approved scene. No display server, no audio sink, no shader pack.
* Scene: `provenance/sources/npc-addons-approved.blend`, a packed copy of the approved NPCAddons
  head scene, SHA-256 `b948b5a03da4269ebed8e82246892e185ddf41b06f3421cf5d571b1f171099d1`.
  Exactly three objects were removed: the green "plus" symbol and its two bar meshes (the exported
  plus-depth block is 0.06). Every retained object, vertex, polygon, UV, world transform, parent,
  material, skin texel, camera, light, world, compositor and view transform is unchanged —
  retained-geometry digests are identical before and after (`geometry_diff` in the metadata).
* Camera: NMSR-equivalent yaw +45 deg, elevation 35.264389682754654 deg (atan(1/sqrt(2))), roll 0 —
  the same framing the existing skin-render service uses (unmodified NMSR build
  `bf87e8275005601c12768a9d74c016d0337d58b0`, documented for other renders in
  `provenance/from-round3/pixel/service-provenance.json`). Random access is by camera only; the image is a Cycles
  CPU path trace, so there is no world seed, no chunk coordinates and no shader pack.
* Imagery: the supplied player skin (SHA-256
  `e9ebbeece495d9c96040e235dc865fdb1a530cf6a2243a6c8fcec22e72f03e3f`, packed unchanged) applied to
  the vanilla player-head mesh (base + 1.125 outer hat layer). No vanilla texture files are shipped
  here; the head geometry is the vanilla player head model carried inside the packed scene.

## Provenance files

`provenance/` holds the author recipe and its verification:

* `brainage-lib-01-matched.py` — entry point; `author.py` — the shared author that opens the source
  scene, removes the plus, renders, packs and writes the metadata.
* `brainage-lib-01-matched.blend` (packed, embeds `author.py` and the entry point),
  `brainage-lib-01-matched-metadata.json` (geometry diff, packed-texture hashes, camera, render settings).
* `sources/npc-addons-approved.blend` — the immutable input scene.
* `render_all.py` — the recorded sequential launcher; `verify_saved.py` — reopens the saved scene and
  re-checks packed assets and geometry; `verify_pngs.py` — decodes and checks the PNG.
* `manifest.json`, `png-verification.json`, `saved-scene-verification.json`, `render-summary.json`,
  `visual-review.json`, `script-hashes.json`, `cleanup-report.json` — curated to this mod's candidate
  (see `CURATION.json` for the exact entries kept and dropped).
* `CURATION.json` — what was copied, what was filtered out of the aggregate JSONs, and what was left in
  round-3.

## How to regenerate

From `<repo>/docs/icon/provenance`:

```sh
nix shell nixpkgs#blender --command blender --background -noaudio --threads 2 \
  --python-exit-code 1 --python brainage-lib-01-matched.py
python3 verify_pngs.py
nix shell nixpkgs#blender --command blender --background -noaudio --python verify_saved.py
```

`author.py` asserts it runs backgrounded with `-noaudio`, with `DISPLAY`/`WAYLAND_DISPLAY` unset and
`CUDA_VISIBLE_DEVICES` empty, and needs `numpy`. The recorded launcher was
`python3 render_all.py brainage-lib-01-matched`, which falls back to `nix shell nixpkgs#blender` when
its pinned `/nix/store/.../blender-5.1.1` path has been garbage-collected. Re-running overwrites
`brainage-lib-01-matched.png` and `-metadata.json` in place.

## Notes

* The round-3 gallery shows BrainageLib's Blender renders under the display group "Existing skin
  renderer" (filter `service`). That is a gallery label, not the producing tool: this icon is a
  Blender 5.1.1 Cycles render that *matches* the existing skin-render service's framing. It was not
  produced by the service.
* The folder author's review recommended the sibling candidate `brainage-lib-02-refined.png`
  (128 samples); the owner selected this 64-sample "matched" variant, which is the one shipped.
  `brainage-lib-02-refined.*` is deliberately not copied.
* The shared `blender-n` folder also contained MagicCarpet candidates and the vanilla particle
  textures they use; none of those files belong to this mod and none were copied.
* Run logs and empty blocker lists were excluded.
* Nothing else in the mod repository was modified and nothing was committed.

## Working-tree note

The round-3 working tree that produced this icon was cleaned up after integration. Every file needed to regenerate the icon was copied into `provenance/`; the copies live under `provenance/from-round3/` when they came from the working tree. Any remaining `round3/...` mention records where something came from, not a path that still exists.
