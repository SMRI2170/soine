# 3D companion plan

The first milestone is intentionally renderer-agnostic. The shared UI exposes a companion scene boundary while platform rendering remains replaceable.

## Required renderer contract

- load one mobile-friendly GLB character
- play idle, settle, sleep, roll, yawn and wake clips
- support a subtle looping breathing animation or morph target
- fixed bedside camera
- simple key/fill lighting
- tap hit-testing for small reactions
- pause or reduce rendering work when the app is not visible

## Performance gate

Before committing to a renderer, test the exact production-like GLB on a physical Android device and iPhone. Record startup time, steady-state memory, FPS while visible, battery/thermal behavior, and audio coexistence.

The sleep session must continue correctly even when the 3D scene is suspended.
