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
