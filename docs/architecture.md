# Architecture

Soine keeps sleep-domain logic independent from UI, sensors, storage, audio, health APIs and 3D rendering.

## Layers

- Domain: sleep session and companion state.
- UI: Compose Multiplatform.
- Platform adapters: audio, notifications, health and microphone.
- Companion renderer: narrow platform boundary for 3D.

## 3D direction

The renderer consumes semantic states such as awake, settling, sleeping and waking. Model-specific animation names stay inside the renderer.

Initial target: mobile-friendly GLB, idle/sleep/roll/yawn/wake animations, subtle breathing, and no continuous expensive physics overnight.

## Sleep direction

MVP records explicit start/end first. Sensor inference comes later so battery, privacy and accuracy can be measured independently.
