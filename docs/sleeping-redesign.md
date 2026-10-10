# V1 Sleeping Screen Redesign

This document is the canonical description of the sleeping
screen redesign. It is the contract slice of
[#170][issue-170] (睡眠中画面を静かな bedside scene へ
作り直す). It is the second EPIC #167 polish slice that
builds on the V1 design system foundation ([#168][issue-168])
and the bedtime redesign ([#169][issue-169]).

Re-evaluate when:

- a new secondary control is added to the sleeping screen
- the auto-hide duration needs to change
- a future 3D renderer replaces the static fallback
- the screen-off / wake-by-button path changes

## Why this exists

Before this slice, the sleeping screen was a vertical column
of always-on text and buttons: "おやすみ", "一緒に眠っています",
"開始済み", "環境音: ...", "タイマー: ...", audio toggle,
timer button, cancel-timer button, wake CTA. The information
was functional, but the screen did not feel like a bedside
scene. The user is supposed to fall asleep; the screen is
supposed to help them do that.

The redesign treats the sleeping screen as a quiet visual
state. The companion is the hero. The secondary controls auto-
hide after a tap-to-reveal. The wake CTA stays anchored at the
bottom so the user can always reach morning in one tap.

## Layout

The sleeping screen is a single Box with three layers:

1. **Hero companion** — centered, with a soft cream-tinted
   radial glow. The companion is rendered through
   `CompanionSceneContent`, the shared component that the
   bedtime and morning screens also use, so a future 3D
   renderer can replace the static fallback in one place.
2. **Top status row** — two compact indicators: an audio
   dot and a timer dot. The dots are the screen-reader's
   content description; the visual is a small filled circle.
3. **Bottom CTA stack** — the secondary controls (audio
   toggle, timer, settings, cancel-timer) sit above the
   `SoinePrimaryButton` "起きる" wake CTA. The wake CTA is
   always anchored; the secondary controls auto-hide.

A single tap on the scene brings the secondary controls back.
After six seconds (`AUTO_HIDE_AFTER_MILLIS`), the controls
fade out and the screen returns to the quiet bedside scene.

## Auto-hide and tap-to-reveal

The secondary controls live behind an `AnimatedVisibility`
that listens to a `controlsVisible` state. The first render
shows the controls (`!quietUi` is the initial value) so the
user can see the secondary actions. After the auto-hide
timeout, the controls fade out. A tap on the scene re-shows
the controls and resets the auto-hide timer.

When `reduceMotion = true`, the auto-hide is a no-op so the
user can read the labels without a fade. The screen also
respects the `quietUi` flag: if the caller passes
`quietUi = true`, the controls start hidden so the screen is
quiet from the first render.

The tap-to-reveal interaction is exposed through
`AccessibilityPolicy.SLEEPING_SCENE_REVEAL_CONTENT_DESCRIPTION`
so TalkBack / VoiceOver announce the action as "画面をタップ
して操作を表示".

## Status indicators

The top status row uses compact dots: a 8dp filled circle
in the `glow` color, paired with a label in the `cream` text
color. The dot itself has no content description; the label
does. The row's `heightIn(min = SoineTokens.MinTouchTargetDp.dp)`
keeps the row reachable for the screen reader even though the
dot is not interactive.

Two indicators are rendered by default:

- "環境音 ON" / "環境音 OFF" — reflects the current
  `audioPlaying` state.
- "タイマー: ..." — rendered only when a timer is set.

The timer indicator is omitted when no timer is active so the
row stays short.

## Wake CTA

The "起きる" wake CTA is a `SoinePrimaryButton` anchored at
the bottom of the screen. It is always reachable in one tap
regardless of the auto-hide state, so a screen-off / wake-by-
button never loses the session. The content description is
`AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION` so the screen-
reader contract is preserved.

The wake CTA is intentionally the only always-on chrome.
Every other visible element fades with the auto-hide so the
bedside scene stays quiet.

## Accessibility

- **Touch targets** — the wake CTA uses `SoinePrimaryButton`
  (56dp minimum height). The secondary actions use
  `SoineQuietButton` (48dp minimum height). Both inherit
  from the design system so a future bump to the touch-target
  tokens moves the sleeping screen with the rest of the
  product.
- **Content descriptions** — the wake CTA uses
  `AccessibilityPolicy.WAKE_CONTENT_DESCRIPTION`. The
  tap-to-reveal interaction uses
  `AccessibilityPolicy.SLEEPING_SCENE_REVEAL_CONTENT_DESCRIPTION`.
  The secondary actions use
  `AccessibilityPolicy.SLEEPING_AUDIO_TOGGLE_CONTENT_DESCRIPTION`,
  `SLEEPING_TIMER_CONTENT_DESCRIPTION`,
  `SLEEPING_TIMER_CANCEL_CONTENT_DESCRIPTION`, and
  `SLEEPING_SETTINGS_CONTENT_DESCRIPTION`.
- **Reduce motion** — the auto-hide is a no-op when
  `reduceMotion = true`. The fade is the only motion in the
  screen; the layout itself stays still.
- **Screen reader path** — one tap to wake, one tap to
  reveal the secondary controls, then one tap to the desired
  action. The textual source checks in
  `SleepingScreenLayoutTest` pin the path.

## What this slice does NOT do

- The "small idle reaction" is owned by the renderer EPIC
  (#27 / #28). The redesign pre-wires the surface through
  `CompanionSceneContent` so a future renderer swap is a
  one-line change.
- The screen-dim / lock transition polish (issue task
  #170-6) is owned by the platform layer. The redesign
  keeps the wake CTA reachable regardless of the platform's
  display state.
- The wake confirmation policy (issue task #170-5) is
  deferred. The V1 wake CTA is a single tap to morning; a
  future polish slice can add a long-press / hold-to-wake
  confirmation if the analytics data suggests it is needed.

## Re-evaluation triggers

- The auto-hide duration needs to change — bump
  `AUTO_HIDE_AFTER_MILLIS`. The test pins the value.
- A new secondary control is added — the
  `SleepingScreenLayoutTest` pin list needs an extra
  constant.
- The `SoineQuietButton` / `SoinePanel` / `SoinePrimaryButton`
  components change shape — the sleeping screen inherits the
  new visual language automatically.

[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
