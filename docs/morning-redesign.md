# V1 Morning Screen Redesign

This document is the canonical description of the morning
screen redesign. It is the contract slice of
[#171][issue-171] (Morning Summary を「昨夜を発見する」
リビール体験にする). It is the third EPIC #167 polish slice
that builds on the V1 design system foundation
([#168][issue-168]) and the bedtime ([#169][issue-169]) and
sleeping ([#170][issue-170]) redesigns.

Re-evaluate when:

- the morning screen adds a new visual section
- the dream discovery surface changes shape
- the morning greeting becomes dynamic
- a future 3D renderer replaces the static fallback

## Why this exists

Before this slice, the morning screen was a vertical column
that read like a generic sleep tracker: a "おはよう" headline,
a duration block, a list of night memory entries, and a
"今日をはじめる" CTA. The data was correct, but the user's
eye landed on the duration first. That puts the screen in the
same category as every other sleep app.

The redesign treats the morning screen as the "昨夜を発見する"
reveal. The companion wake reaction and the morning greeting
are the hero. The night memory and dream discovery are the
next layer. The sleep duration is a small summary card at
the bottom — present, but not the first thing the user sees.

## Layout

The morning screen is a single vertical column with five
layers, in order from top to bottom:

1. **Hero companion wake reaction** — the companion sits on a
   soft sunrise-tinted glow, the same companion surface that
   the bedtime and sleeping screens share through
   `CompanionSceneContent`. The state is `SleepState.FINISHED`
   so a future 3D renderer can swap in the "stretch / look at
   user" intent without touching the screen.
2. **Morning greeting + sub-label** — "今日も一緒に起きられた
   ね" in `headlineSmall` and "昨夜のふりかえり" in `bodyMedium`.
   The greeting is the emotional anchor; the sub-label invites
   the user to scroll the rest.
3. **Dream discovery special reveal** (conditional) — a
   `SoinePanel` with the `今朝の発見` section header and a
   short "夢を見つけた" caption. The reveal is rendered only
   when the dream discovery coordinator produced at least one
   `DreamDiscovery`; the panel surfaces *above* the night
   memory so the "見つけた" feeling lands first.
4. **Night memory timeline** — the existing
   `NightMemoryTimeline` rows. Renders only when there is at
   least one entry; the redesign does not regress to a glyph-
   only list.
5. **Compact summary card** — a `SoinePanel` with the sleep
   duration in `titleLarge`, plus the bedtime / wake time in
   two side-by-side `SummaryField` columns. The duration is
   not the hero anymore.
6. **Primary CTA** — `SoinePrimaryButton` with
   `AccessibilityPolicy.MORNING_DONE_CONTENT_DESCRIPTION`,
   "今日をはじめる", to dismiss the morning screen.

The screen is wrapped in a `verticalScroll` so the layout
stays usable at the largest Dynamic Type setting. The
Emotional layer (companion + greeting) is reachable without
scroll on a 6.1" phone at the default text scale.

## Emotion → Memory → Data order

The issue calls out the order explicitly. The redesign pins
the order in the source so a future contributor cannot
accidentally put the duration above the night memory:

| Layer | Source anchor | Position in column |
| --- | --- | --- |
| Emotion | `MorningHero(...)` + `text = morningGreeting,` | 1st and 2nd |
| Memory | `DreamDiscoveryReveal(...)` (when discoveries exist) + `NightMemoryTimeline(...)` | 3rd and 4th |
| Data | `SummaryCard(summary = summary)` | 5th |

`MorningScreenLayoutTest.morningScreenEmotionBeforeData` and
`morningScreenDreamDiscoveryRevealPrecedesNightMemory` pin the
order. The tests anchor on the *call sites* inside the
`MorningScreen` body, not on helper function definitions,
so a future refactor that moves the helper functions does not
silently change the visual order.

## Dream discovery special reveal

The dream discovery special reveal is the screen's
"見つけた感" surface. It is rendered only when
`dreamDiscoveries` is non-empty, and it is positioned above
the night memory so the user's eye lands on the discovery
before they read the rest of the morning summary.

The reveal is intentionally short. The panel does not list
the discoveries; the `SoineSectionHeader("今朝の発見")` and
the `"夢を見つけた"` caption are the celebration, and the
actual discoveries are surfaced in the dream album. The
caption text is rendered in `SoineColors.sunrise` so the
panel reads as a new-day accent, not as a system notification.

The default `dreamDiscoveries = emptyList()` keeps the
morning screen rendering even when the dream coordinator has
not produced any discoveries yet. The morning flow does not
block on the dream surface; a user with no discoveries still
sees the companion, greeting, night memory, and summary card.

## Hero scene internals

The morning hero is the same `CompanionSceneContent` that the
bedtime and sleeping screens use. The state is
`SleepState.FINISHED` so the future 3D renderer can map the
state to a `WAKE` / `STRETCH` intent without touching the
screen. The radial gradient is `SoineColors.sunrise` at 20%
alpha to transparent — the same recipe as the bedtime
cream-tinted glow, but in the sunrise accent so the screen
reads as a new day.

## Accessibility

- **Touch targets** — the primary CTA uses
  `SoinePrimaryButton` (56dp minimum height). The morning
  screen does not re-declare `heightIn(min = ...)`; the
  design system owns the touch target.
- **Content descriptions** — the primary CTA uses
  `AccessibilityPolicy.MORNING_DONE_CONTENT_DESCRIPTION`. The
  dream discovery reveal and the summary card have no
  custom descriptions; the screen reader reads the section
  header + caption in order.
- **Reduce motion** — the layout itself does not depend on
  motion. The sunrise-tinted glow is a static radial gradient
  and the panel rendering is plain `SoinePanel`. The
  `reduceMotion` parameter is plumbed into
  `CompanionSceneContent` so the future 3D renderer swap can
  use it; the V1 morning screen does not animate.
- **Screen reader path** — one tap to the primary CTA, two
  taps to the dream album and settings from the top nav (the
  nav lives in `App` and is shared with the bedtime screen).
  The textual source checks in `MorningScreenLayoutTest` and
  `AccessibilitySourceAuditTest` pin the path.
- **Default greeting** — `DEFAULT_MORNING_GREETING` is
  `internal const val` so a future locale change can replace
  the greeting through a single constant without touching
  the call sites. The default is "今日も一緒に起きられたね".

## What this slice does NOT do

- The `staged reveal` task (#171-1) is deferred. The V1
  morning screen renders all layers at once; a future
  polish slice can add a `LaunchedEffect`-driven stagger if
  the analytics data suggests the user needs more time to
  absorb the discovery.
- The `companion WAKE / STRETCH presentation` (#171-2) is
  owned by the renderer EPIC. The redesign pre-wires the
  surface through `CompanionSceneContent(SleepState.FINISHED)`
  so the future renderer swap is a one-line change.
- The `relationship 変化の subtle presentation` (#171-5) is
  owned by the relationship EPIC. The morning screen
  receives the relationship stage through the existing
  `App` boundary; a future polish slice can add a
  relationship-change toast without changing the screen
  layout.
- The `no-event morning の魅力を保つ` (#171-7) is covered by
  the conditional rendering of the dream discovery reveal
  and the night memory timeline — both panels omit
  themselves when there is no data, so an empty morning still
  has the companion and the greeting.
- The `optional health/detail` (#171-6) is deferred. The
  summary card shows the duration, bedtime, and wake time
  from the local `SleepSummary`; a future polish slice can
  add a Health-derived breakdown through the existing
  Health data contract without changing the screen layout.

## Re-evaluation triggers

- A new visual layer is added to the morning screen — the
  Emotion → Memory → Data order in this document must move
  with it, and `MorningScreenLayoutTest` needs an extra
  anchor.
- The dream discovery surface becomes dynamic (e.g. a
  carousel of discoveries instead of a count) — the special
  reveal needs a redesign, and the
  `morningScreenDreamDiscoveryRevealPrecedesNightMemory`
  test must move with the new render call site.
- The morning greeting becomes locale-aware — the
  `DEFAULT_MORNING_GREETING` constant moves to the
  localization pipeline, and the test must drop the literal
  check.
- The `SoineQuietButton` / `SoinePanel` / `SoinePrimaryButton`
  components change shape — the morning screen inherits the
  new visual language automatically.

[issue-171]: https://github.com/SMRI2170/soine/issues/171
[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
