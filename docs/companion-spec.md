# Companion Specification

Working name: ねむ

## Visual identity

- original species-ambiguous small creature
- soft rounded silhouette
- cream/off-white body
- very short limbs
- tiny rounded ears
- bead-like dark eyes
- tiny mouth
- under three-head proportion
- no permanent decorative accessory
- calm enough to view every night

Avoid silhouettes, facial placement and signature traits strongly associated with existing character IP.

## Camera

The default scene is from the user's pillow-side perspective. This is not a third-person pet game camera.

## Semantic actions

- IDLE
- NOTICE_USER
- LOOK_AT_USER
- MOVE_CLOSER
- SETTLE
- CURL_UP
- SLEEP
- BREATHE
- EAR_TWITCH
- ROLL_OVER
- YAWN
- BRIEF_WAKE
- WAKE
- STRETCH

## Asset budget target

Keep the first production-like model intentionally small:
- one character
- compact skeleton
- low mobile triangle count
- compressed textures
- minimal materials
- animation clips separated by semantic action
- optional morph target for subtle breathing/squish

Exact budgets are determined by device PoC, not by desktop appearance.

## Animation principle

Breathing is the base layer. Tiny events are sparse overlays. Constant movement makes the companion feel game-like and wastes battery.

## Fallback

Every semantic state must have a static/2D fallback so failure to initialize 3D never blocks bedtime.

## Production-asset pipeline (V1, #173)

The V1 ships the static 2D fallback
([`CompanionStaticFallback`][companion-static-fallback]) as
the primary path. The production 3D asset lives in the art
pipeline and is owned by a future polish slice. The
[`CompanionAssetPipeline`][companion-asset-pipeline] is the
single source of truth for "which path is the V1 default?".

The V1 contract:

- [`CompanionAssetPipelineStatus.STATIC_FALLBACK`][companion-asset-pipeline]
  is the V1 default.
- The static 2D fallback is **not** a degraded path. It is
  the V1 voice: the same soft, kaomoji-style companion
  glyphs are the visual identity the user reads every night.
  The production 3D asset is a higher-fidelity version of
  the same identity, not a replacement.
- The 3D renderer reads the same
  [`CompanionRenderRequest`][companion-renderer] that the
  static fallback reads. The renderer swap is a one-line
  change in [`CompanionRenderCoordinator`][companion-render-coordinator].
- The renderer does not need to know which pipeline is
  active. The pipeline status is owned by
  [`CompanionAssetPipeline`][companion-asset-pipeline] and
  is read by the renderer coordinator.

## Production-asset acceptance criteria (V1 contract)

A future polish slice flips
[`CompanionAssetPipeline.current`][companion-asset-pipeline]
from `STATIC_FALLBACK` to `PRODUCTION_READY` when the
production 3D asset meets the V1 acceptance criteria. The
criteria are pinned in
[`CompanionAssetAcceptance`][companion-asset-pipeline]
so a future contributor cannot drift the bar:

| Criterion | Value | Note |
| --- | --- | --- |
| `MAX_TRIANGLES` | 8,000 | conservative mobile ceiling; the exact budget is determined by a real device PoC |
| `MAX_BONES` | 48 | mid-range Android animation budget |
| `MAX_GLB_BYTES` | 1,500,000 (1.5 MB) | after compression; the V1 PoC is ~30 KB |
| `REQUIRED_CLIP_IDS` | every `CompanionIntent` clip | missing clips fall back to the static path through [`CompanionRendererFallbackPolicy`][companion-static-fallback] |

A real device PoC must verify the asset meets the budgets
on a mid-range Android and a mid-range iPhone. The V1
ships without the production asset, so the device PoC is
a future polish slice's responsibility.

## What this slice does NOT do

- The production 3D asset itself. The artist session is
  owned by a future polish slice; this slice pins the
  contract and acceptance criteria.
- The device PoC verification. The exact budgets depend
  on a real device run; the V1 conservative ceilings are
  the upper bound, not the target.
- The 3D renderer's GLB loader. The renderer interface
  ([`CompanionRenderer`][companion-renderer]) is
  renderer-neutral; the platform-specific 3D loader lives
  in a future polish slice.

## Re-evaluation triggers

- The production 3D asset ships and a device PoC verifies
  the budgets — flip
  [`CompanionAssetPipeline.current`][companion-asset-pipeline]
  to `PRODUCTION_READY`. The static fallback remains the
  graceful-degradation path.
- A new `CompanionIntent` is added —
  [`REQUIRED_CLIP_IDS`][companion-asset-pipeline] grows
  and the production asset must include the new clip.
- The mobile triangle budget is re-evaluated on a new
  device class — update
  [`CompanionAssetAcceptance.MAX_TRIANGLES`][companion-asset-pipeline]
  and the contract updates with it.

[companion-static-fallback]: ../composeApp/src/commonMain/kotlin/app/soine/companion/CompanionStaticFallback.kt
[companion-asset-pipeline]: ../composeApp/src/commonMain/kotlin/app/soine/companion/CompanionAssetPipeline.kt
[companion-renderer]: ../composeApp/src/commonMain/kotlin/app/soine/companion/CompanionRenderer.kt
[companion-render-coordinator]: ../composeApp/src/commonMain/kotlin/app/soine/companion/CompanionRenderCoordinator.kt
