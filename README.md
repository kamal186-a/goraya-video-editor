# Goraya Video Edition
Open-source Android video editor (Kotlin, Compose, Media3, OpenGL ES). 100% original code.

## Build (one command)
    bash build.sh
Output: `dist/GorayaVideoEdition.apk` (also `app/build/outputs/apk/release/app-release.apk`).
Requires JDK 17, curl, unzip. The script downloads the Android SDK (Linux) and Gradle if missing.

## Status
- Phase 1 (done): compiling skeleton, dark UI shell, timeline model, effect registry, CI.
- Phase 2a (done): photo/video import, Media3 preview with Play/Pause/seek.
- Phase 2b (done): timeline blocks, trim, split, copy, delete, reorder, MP4 export (480p-1080p, 16:9/9:16/1:1/4:5).
- Phase 3a (done): 10 colour filters + brightness/contrast/saturation/temperature (GPU, preview + export).
- Phase 3b+: more GPU effects, filters, transitions, text (Urdu/RTL), keyframes, audio.
- Phase 4: auto captions, chroma key, background removal, stickers, project save.

## Adding an effect
Register an `EffectPreset` (GLSL fragment shader + defaults) in `effects/EffectRegistry`.

## Licenses
See `LICENSE` and `docs/THIRD_PARTY_LICENSES.md`.
