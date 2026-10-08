# Goraya Video Edition - project info

## Build
    bash build.sh        ->  dist/GorayaVideoEdition.apk
(also app/build/outputs/apk/release/app-release.apk). GitHub Actions builds the same on every push.

## Android
minSdk 24 (Android 7.0), targetSdk/compileSdk 34. Best on Android 10+ (gallery save uses MediaStore).

## Structure
- app/ - UI (Compose), edit state, text renderer, exporter, project store
- video-engine/, effects/, timeline/ - small library modules (effect registry, timeline model)
- audio/, captions/, ai/, export/, assets/ - reserved for later features
- .github/workflows/build.yml - CI

## Implemented
Import (video/photo), Media3 preview, timeline (trim, split, copy, delete, reorder), 10 colour filters +
brightness/contrast/saturation/temperature (GPU), text layers (Urdu/RTL, fonts, colour, background, position, rotation),
emoji stickers, canvas aspect ratios, music track (volume, fade in/out), per-clip mute, project save/auto-restore,
MP4 export 480p-1080p with progress/cancel/error handling.

## Not implemented yet (see FUTURE.md)
Speed/reverse/freeze, rotate/flip/crop/zoom, keyframes, transitions, extra GPU effects (blur, glitch, VHS...),
text animations, auto captions, chroma key, background removal, FPS/bitrate choice, voice recording.

## Adding features
- Filter: add a FilterPreset in edit/Look.kt.
- Sticker: add an emoji (or later an image asset pack) in ui/AudioPanels.kt.
- Effect: implement a Media3 GlEffect (GLSL shader) and add it to the effect list in Exporter + PreviewPanel.
