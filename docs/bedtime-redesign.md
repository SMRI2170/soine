# V1 Bedtime Screen Redesign

This document is the canonical description of the bedtime
screen redesign. It is the contract slice of
[#169][issue-169] (就寝開始画面を「一緒に寝る」体験の入口として
再設計する). It is the first EPIC #167 polish slice that builds
on the V1 design system foundation ([#168][issue-168]).

Re-evaluate when:

- the bedtime screen adds a new visual section
- a new relationship stage is added to the V1 progression
- the V1 design system gains a new component that the bedtime
  screen should adopt
- the future 3D renderer replaces the static fallback

## Why this exists

Before this slice, the bedtime screen was a vertical column of
generic Material3 components: a top nav, a static companion
glyph, a headline, a sub-label, an OutlinedCard with ambient
sound + timer, and a Button. The screen was functional but it
did not say "Soine". Two screens that should feel like one
product felt like two different products.

The redesign treats the bedtime screen as the V1 signature
moment. The companion is the hero, the layout reads at a
glance, the secondary actions are quiet, and the primary CTA
is the bottom of the column.

## Layout

The bedtime screen is a single vertical column with five
sections, in order from top to bottom:

1. **Top navigation** — the `soine` wordmark on the left, the
   `夢のアルバム` and `設定` quiet buttons on the right. The
   buttons are `SoineQuietButton` (text-only, 48dp touch target)
   so they do not compete with the primary CTA.
2. **Hero companion scene** — a soft cream-tinted glow sits
   behind the companion, with the companion glyph in the
   center. The scene size grows with the relationship stage so
   a closer relationship feels closer to the camera.
3. **Headline + body** — "今日も一緒に眠ろう" and the
   permission-free sub-label.
4. **Compact status row** — two side-by-side `SoinePanel`
   surfaces: ambient sound on the left, sleep timer on the
   right. The split is intentional; a single settings card
   regresses the redesign.
5. **Primary CTA** — `SoinePrimaryButton` with
   `AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION`. One
   tap to sleep start.

The screen is wrapped in a `verticalScroll` so the layout
remains usable at the largest Dynamic Type setting. The
primary CTA stays reachable without scroll on a 6.1" phone at
the default text scale.

## Relationship-stage variation

The bedtime screen accepts a `relationshipStage` parameter
typed `CompanionRelationshipStage`. The four stages
(`NEW`, `WARMING_UP`, `FAMILIAR`, `CLOSE`) drive the hero
scene size:

| Stage | Hero scene size |
| --- | --- |
| `NEW` | 220 dp |
| `WARMING_UP` | 230 dp |
| `FAMILIAR` | 250 dp |
| `CLOSE` | 280 dp |

A fresh install defaults to `NEW` so the user sees the
smallest hero scene. The relationship stage is fetched from
`CompanionProgressRepository.familiarityStage` and refreshed
on every wake cycle; the screen reacts at the next composition
without a manual recompose trigger.

## Hero scene internals

The hero scene is composed of two layers:

1. A soft radial gradient (`Brush.radialGradient`) that fades
   from `SoineColors.cream` at 20% alpha in the center to
   transparent at the edges. The gradient sits behind the
   companion so the screen reads as warm and quiet, not as a
   flat midnight surface.
2. The companion surface, drawn through
   `CompanionSceneContent`, which is a shared component with
   the sleeping and morning screens. A future 3D renderer can
   replace the static fallback without touching the bedtime
   screen.

The `CompanionSceneContent` is reusable across the bedtime,
sleeping, and morning screens so the visual identity stays
consistent and a renderer swap is a one-line change.

## Accessibility

- **Touch targets** — the primary CTA uses `SoinePrimaryButton`
  (56dp minimum height). The top nav uses `SoineQuietButton`
  (48dp minimum height). Both inherit from the design system
  so a future bump to the touch-target tokens moves the bedtime
  screen with the rest of the product.
- **Content descriptions** — the primary CTA uses
  `AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION`. The
  top nav uses
  `AccessibilityPolicy.BEDTIME_DREAM_ALBUM_CONTENT_DESCRIPTION`
  and
  `AccessibilityPolicy.BEDTIME_SETTINGS_CONTENT_DESCRIPTION`.
- **Screen reader path** — one tap to the primary CTA, two
  taps to Dream Album / Settings. The textual source checks
  in `BedtimeScreenLayoutTest` pin the path.
- **Reduce motion** — the layout itself does not depend on
  motion. The companion scene uses the zero-duration
  placement when `reduceMotion = true`; the rest of the layout
  stays still. `ReduceMotionCoverageTest` (from #195) covers
  the contract.

## What this slice does NOT do

- The hero scene is the static fallback, not a 3D scene. The
  3D renderer is owned by EPIC #27 / #28 and will replace the
  fallback through the shared `CompanionSceneContent` surface.
- The "small idle reaction" listed in the issue is owned by
  the renderer EPIC. The redesign pre-wires the surface so the
  future renderer swap is a one-line change.
- The time-of-night background treatment is a follow-up; the
  redesign ships a single dusk palette through the design
  system. A future polish issue can extend the palette with a
  `SoineColors.dawn` etc. without changing the screen.

## Re-evaluation triggers

- A new relationship stage is added to
  `CompanionRelationshipStage` — the hero scene size map
  must be extended.
- The primary CTA wording changes — the content description
  constant must move with it.
- The `SoineQuietButton` / `SoinePanel` / `SoinePrimaryButton`
  components change shape — the bedtime screen inherits the
  new visual language automatically; this document only
  needs an update if the redesign's *layout* (five sections
  in this order) changes.

[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
