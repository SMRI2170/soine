# V1 Relationship-Stage Visual Presentation

This document is the canonical description of the
relationship-stage visual presentation. It is the contract
slice of [#175][issue-175] (関係性 progression を数値ではなく
見た目と行動で感じられるようにする). It is the fifth EPIC #167
polish slice that builds on the V1 design system foundation
([#168][issue-168]) and the bedtime ([#169][issue-169]),
sleeping ([#170][issue-170]), morning ([#171][issue-171]),
and Dream Album ([#172][issue-172]) redesigns.

Re-evaluate when:

- a new relationship stage is added
- a new placeholder screen needs stage-aware visuals
- the relationship progression rule is rebalanced and the
  visuals need to follow
- the future 3D renderer replaces the static fallback

## Why this exists

Before this slice, the relationship stage was a persistence
value with a one-line visible effect: the bedtime hero scene
grew with closeness (220 / 230 / 250 / 280 dp). The user
could not feel the change without reading the design intent.
The stage value itself was never shown in the chrome, which
is the right call, but the visual payoff was too thin to
match the "寝るほど仲良くなる" promise.

The redesign gives the stage a visible voice across the
bedtime, sleeping, and morning screens — without ever
exposing the stage value, the session count, or the
cumulative hours in the chrome.

## Single source of truth

`app.soine.companion.CompanionStagePresentation` is the
single struct that owns the stage→visual mapping. The
`CompanionStagePresentationPolicy.forStage(stage)` factory
returns a deterministic value for each
[CompanionRelationshipStage][issue-167-foundation]:

| Field | NEW | WARMING_UP | FAMILIAR | CLOSE |
| --- | --- | --- | --- | --- |
| `bedtimeHeroSizeDp` | 220 | 230 | 250 | 280 |
| `bedtimeGlowAlpha` | 0.18 | 0.22 | 0.26 | 0.30 |
| `bedtimeGreetingSuffix` | `null` | "、少しわかってきたね" | "、この時間、なんだか落ち着くね" | "、今日はもっとそばにいるね" |
| `sleepingBedOffsetFraction` | 0.78 | 0.60 | 0.38 | 0.18 |
| `sleepingAccentAlpha` | 0.10 | 0.14 | 0.18 | 0.22 |
| `morningGlowColor` | `cream` | `cream` | `sunrise` | `sunrise` |
| `morningGlowAlpha` | 0.18 | 0.20 | 0.20 | 0.22 |
| `morningGreetingSuffix` | `null` | "、少しわかってきたね" | "、この時間、なんだか落ち着くね" | "、ずっといたかった" |
| `stageAdvancedReveal` | `null` | `null` | "少し、近づけた気がする。" | "もっと近くに来たよ。" |

Every value moves monotonically with closeness. NEW is the
quietest; CLOSE is the warmest. The bedtime hero size
grows; the sleeping bed offset shrinks; the morning glow
alpha and color shift; the greeting copy and the
relationship-change reveal add one quiet phrase per
promotion.

The values are pinned by `CompanionStagePresentationPolicy.CURRENT_VERSION`
so a future polish slice can rebalance the curves without
invalidating the existing screenshots.

## How the screens consume it

The three main screens (bedtime, sleeping, morning) all
consume the policy. There is no inline `when` block on
stage anywhere in the screen code.

- **BedtimeScreen** reads `bedtimeHeroSizeDp` and
  `bedtimeGlowAlpha` to size and warm the hero scene, and
  appends `bedtimeGreetingSuffix` to the default
  "今日も一緒に眠ろう" headline. The headline itself
  stays the same on every stage so the bedtime rhythm is
  not broken.
- **SleepingScreen** reads `sleepingBedOffsetFraction`
  (through the existing `CompanionSleepingDistancePolicy.forStage`)
  and `sleepingAccentAlpha` to warm the bedside scene.
- **MorningScreen** reads `morningGlowColor` and
  `morningGlowAlpha` to color and warm the sunrise glow,
  appends `morningGreetingSuffix` to the default
  "今日も一緒に起きられたね" greeting, and renders the
  `RelationshipChangeReveal` panel when the stage has
  crossed since the last wake.

`CompanionSceneContent` is unchanged. The presentation is
purely a 2D fallback; the future 3D renderer will read the
same struct and map the values into world / screen
coordinates.

## Relationship change reveal

The relationship-change reveal is a quiet one-shot notice
in the morning screen. The contract:

- The notice surfaces only when the user wakes and the
  relationship stage has crossed since they fell asleep.
  `previousStage` is captured at the moment the user taps
  "一緒に寝る"; the morning screen compares the post-wake
  stage against `previousStage`.
- The notice copy is a single phrase ("少し、近づけた
  気がする。" for FAMILIAR; "もっと近くに来たよ。" for
  CLOSE). It is not a celebration, not a number, and not a
  progress bar.
- The notice is wrapped in a `SoinePanel` with a quiet
  "少しだけ、近づいた朝" section header. The copy is in
  `SoineColors.sunrise` so the panel reads as a new-day
  accent, not as a system notification.
- The notice is genuinely one-shot. The `previousStage`
  variable is reset to `null` after the user taps "今日を
  はじめる", so the next wake does not re-show the same
  reveal.

The WARMING_UP stage has no reveal copy. The first
promotion (NEW → WARMING_UP) is intentionally without a
notice so the user does not feel celebrated for completing
a single night. A future polish slice can add a WARMING_UP
reveal if the analytics data suggests the first promotion
goes unnoticed.

## Color composition

The morning glow color shifts from `cream` (NEW / WARMING_UP)
to `sunrise` (FAMILIAR / CLOSE). Both colors come from the
Soine palette (`SoineColors.cream` and `SoineColors.sunrise`);
the presentation never introduces a new color. The
`CompanionStagePresentationTest.presentationColorsAreComposedFromSoinePalette`
check pins the contract.

The hero glow color stays `cream` on every stage; the
warmth comes from the alpha, not the hue. The morning
glow color shift is the only place the morning scene
introduces a different color.

## What this slice does NOT do

- **No XP / level / session count UI** — the chrome never
  shows the stage value, the session count, or the
  cumulative hours. The progression is felt, not measured.
  `presentationDoesNotEmbedXpLevelOrSessionCount` pins the
  contract.
- **No aggressive streak** — a relationship that does not
  cross the threshold for a while does not surface any
  urgency. The relationship state can be `NEW` for an
  arbitrary long time; the user is not penalized for short
  or irregular nights (see
  `docs/relationship-system.md` guardrails).
- **No stage-gated dream / dialogue change in this slice** —
  the bedtime and morning dialogue catalogs already
  gate dialogue on `minimumFamiliarity`. The presentation
  slice does not touch the dialogue pipeline; it adds the
  one-line greeting suffix on top of the existing default
  greeting. A future polish slice can swap the greeting
  entirely through the same struct.
- **No motion easing for the stage transition** — the
  bedtime hero scene jumps from one size to the next at
  the next wake. The future 3D renderer EPIC can add a
  transition; the V1 fallback stays still.
- **No BONDED stage** — the issue mentions BONDED in the
  examples, but the V1 ships the four-stage scale (NEW,
  WARMING_UP, FAMILIAR, CLOSE). A future polish slice can
  add a BONDED stage through the same struct.

## Re-evaluation triggers

- A new relationship stage is added — the enum grows and
  the policy's `when` must add a branch. The
  monotonicity tests fail loudly if a new stage breaks the
  trend.
- The morning glow color shift becomes too obvious — the
  test pins the policy against the Soine palette, so a
  new color must come from the palette. The
  monotonicity tests still hold.
- The user wants the WARMING_UP reveal — the policy's
  WARMING_UP branch can grow a `stageAdvancedReveal`
  field; the test for "FAMILIAR and CLOSE stages carry
  reveal copy" can be relaxed.
- A future screen needs stage-aware visuals — the screen
  consumes `CompanionStagePresentationPolicy.forStage(stage)`
  through the same struct. No new field is needed; the
  screen picks the values it wants from the existing
  presentation.

[issue-175]: https://github.com/SMRI2170/soine/issues/175
[issue-172]: https://github.com/SMRI2170/soine/issues/172
[issue-171]: https://github.com/SMRI2170/soine/issues/171
[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
