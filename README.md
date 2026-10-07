# Goraya Video Edition
Open-source Android video editor (Kotlin, Compose, Media3, OpenGL ES). 100% original code.

## Build (one command)
    bash build.sh
Output: `dist/GorayaVideoEdition.apk` (also `app/build/outputs/apk/release/app-release.apk`).
Requires JDK 17, curl, unzip. The script downloads the Android SDK (Linux) and Gradle if missing.

## Status
- Phase 1 (done): compiling skeleton, dark UI shell, timeline model, effect registry, CI.
- Phase 2: import, preview, timeline UI, trim/split/merge, MP4 export.
- Phase 3: GPU effects, filters, transitions, text (Urdu/RTL), keyframes, audio.
- Phase 4: auto captions, chroma key, background removal, stickers, project save.

## Adding an effect
Register an `EffectPreset` (GLSL fragment shader + defaults) in `effects/EffectRegistry`.

## Licenses
See `LICENSE` and `docs/THIRD_PARTY_LICENSES.md`.
