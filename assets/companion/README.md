# Soine companion GLB

Asset: `soine_companion.glb`

Purpose: shared lightweight PoC asset for #27 (Android) and #28 (iOS).

## Character

- cream-white rounded body
- short arms and legs
- round ears
- dot eyes
- small mouth
- no accessories
- low-poly geometry
- two PBR materials
- no external textures

## Format

- glTF 2.0 binary (`.glb`)
- single-file asset
- approximately 30 KB
- 7 reusable mesh primitives
- 12 scene nodes
- node-transform animation; no skinning dependency

## Animation clips

- `IDLE`
- `LOOK`
- `MOVE_CLOSER`
- `SETTLE`
- `SLEEP`
- `BREATHE`
- `WAKE`

These names intentionally match the current Soine companion intent / signature-bedtime flow.

## PoC scope

This asset is intended to remove the source-asset blocker for renderer integration.
It is not the final production art asset.

Validate on physical devices before closing #27/#28:

- GLB load
- fixed camera framing
- lighting
- animation clip selection
- tap hit-test
- lifecycle pause/resume
- static fallback
- startup time
- memory
- FPS
- battery
- thermal
