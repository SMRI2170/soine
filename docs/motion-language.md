# V1 Motion Language

This document is the canonical description of the Soine
motion language. It is the contract slice of
[#177][issue-177] (画面遷移・microinteraction・haptics を
Soine らしく統一する). It is the seventh EPIC #167 polish
slice that builds on the V1 design system foundation
([#168][issue-168]) and the bedtime ([#169][issue-169]),
sleeping ([#170][issue-170]), morning ([#171][issue-171]),
Dream Album ([#172][issue-172]), relationship-stage
presentation ([#175][issue-175]), and settings IA
([#176][issue-176]) redesigns.

Re-evaluate when:

- a new surface needs a motion token (e.g. a card-expand
  that is not fade)
- a new haptic moment is added (e.g. a milestone unlock)
- the future 3D renderer EPIC replaces the static fallback
  with animation clips

## Why this exists

Before this slice, the V1 polish surfaces each had their
own ad-hoc animation choices. The sleeping screen used a
bare `AnimatedVisibility` for the auto-hide secondary
controls; the buttons had no press feedback; the
relationship-stage presentation was a static 2D fallback.
The motion language was scattered across the codebase, and
a future polish slice that needed a "consistent fade"
would have to guess the right duration and easing.

The redesign introduces a single source of truth — the
[`MotionTokens`][motion-tokens] object — and a thin
wrapper — the
[`SoineAnimatedVisibility`][soine-animated-visibility] and
`SoineAnimatedVisibilityVertical` composables — so every
transition shares the same duration, easing, and
reduce-motion behavior. The buttons get a press feedback
through the same motion language. The haptics are
constrained to two intensities (Light, Medium) and three
moments (sleep start, wake, dream discovery).

## Motion tokens

`app.soine.motion.MotionTokens` owns three durations and
two easings:

| Token | Value | Used for |
| --- | --- | --- |
| `DURATION_QUICK` | 120 ms | short feedback (button press, micro-interaction) |
| `DURATION_NORMAL` | 240 ms | standard UI motion (sheet transition, status indicator fade) |
| `DURATION_SLOW` | 480 ms | large element transitions (companion scene shift, full-card reveal) |

| Easing | Used for |
| --- | --- |
| `EASING_SOFT` (`FastOutSlowInEasing`) | the default "soine" easing. Soft acceleration, soft deceleration. |
| `EASING_GENTLE` (`LinearOutSlowInEasing`) | fade-out and dismiss motion. Strong deceleration so the last frame settles. |

There is no bouncy, overshooting, or springy motion. A
`spring(dampingRatio = 0.4)` would read as gamified; the
V1 voice is calm and slow.

The motion tokens are pinned by `MotionTokensTest`. A new
duration must come with a documented meaning; a bouncy /
springy value would regress the V1 voice.

## Reduce-motion mappings

`MotionTokens.durationMillis(baseDuration, reduceMotion)`
collapses every duration to 0 ms when reduce-motion is on.
`MotionTokens.easing(reduceMotion)` returns a strict-step
easing in the reduce-motion case so the transition becomes
a single instantaneous state change. The functional
information (the content that toggles) is preserved; the
motion itself is removed.

The V1 surfaces read `reduceMotion` from
`AccessibilityPreferences` (the existing accessibility
preference). A user who has enabled reduce-motion sees
the same UI as a user who has not, but with no animation
between states. The audio session, the dream discovery,
and the relationship-stage reveal all stay intact; the
chrome just does not animate.

## SoineAnimatedVisibility

`app.soine.motion.SoineAnimatedVisibility` wraps the
Material 3 `AnimatedVisibility` with the V1 motion
tokens. The default enter / exit is a 240 ms fade with
`EASING_SOFT`. The wrapper accepts a `reduceMotion` flag
and an explicit `enter` / `exit` override; the override
still uses the V1 motion language for duration and easing.

`SoineAnimatedVisibilityVertical` is a vertical-collapse
variant used for the sleeping screen's secondary controls
reveal / dismiss.

The wrapper pins the motion language in source-audit form:
`MotionLanguageTest.sleepingScreenUsesSoineAnimatedVisibility`
asserts that `SleepingScreen.kt` does not import or call
the bare Material `AnimatedVisibility`.

## Button press feedback

`SoinePrimaryButton` and `SoineQuietButton` add a subtle
press feedback: a 0.97 scale-down on press, animated
through `animateFloatAsState` with `DURATION_QUICK`. When
reduce-motion is on, the scale-down is a discrete state
change with no animation.

The press feedback is driven by the Material 3
`InteractionSource` so the visual state tracks the actual
press state. The interaction source is hoisted to the
button so future polish slices can add additional press
behaviors (e.g. ripple) without changing the API.

## Haptics

`app.soine.motion.Haptics` is the platform-agnostic
interface. The V1 ships two intensities:

| Intensity | Used for |
| --- | --- |
| `Light` | sleep start, wake, primary CTA press |
| `Medium` | dream discovery (one new dream in the morning) |

The interface is intentionally narrow. A new intensity
must come with a documented meaning; the call site must
pick the right intensity for the moment, not a
one-size-fits-all default.

The platform impls are:

- `AndroidHaptics` (in `androidMain`) — uses
  `View.performHapticFeedback` with
  `HapticFeedbackConstants.CONTEXT_CLICK` (Light) or
  `HapticFeedbackConstants.LONG_PRESS` (Medium). The
  `FLAG_IGNORE_GLOBAL_SETTING` is intentionally NOT set;
  the system setting (Settings → Sounds → Touch feedback)
  gates every haptic.
- `IosHaptics` (in `iosMain`) — uses
  `UIImpactFeedbackGenerator` with `UIImpactFeedbackStyleLight`
  or `UIImpactFeedbackStyleMedium`. The generator respects
  the device's haptic setting (Settings → Sounds & Haptics
  → System Haptics).
- `NoOpHaptics` (in `commonMain`) — a no-op fallback for
  tests and the desktop / JVM target. Tests use
  `RecordingHaptics` to assert that the right intensities
  fired in the right order.

The three meaningful moments where haptics fire are:

1. **Sleep start** — `HapticsIntensity.Light` fires inside
   the `onStartSleep` callback in `SoineApp.kt`, after the
   previous-relationship-stage capture and the start of
   the new session. The user is committing to a night;
   the haptic confirms the start without competing with
   the companion motion.
2. **Wake** — `HapticsIntensity.Light` fires inside the
   `onWake` callback, after the session finishes and the
   morning destination is selected. The user is returning
   from a night; the haptic confirms the transition
   before the morning screen renders.
3. **Dream discovery** — `HapticsIntensity.Medium` fires
   when `dreamDiscoveries.size > previousDiscoveryCount`.
   A new dream is a stronger moment; the medium intensity
   signals "you found something" without a separate
   notification.

No haptic fires on every button press, on every state
change, or on any non-meaningful moment. The haptics are
the user's confirmation that "this is a moment that
matters".

`MotionLanguageTest.soineAppFiresHapticOnSleepStart`,
`soineAppFiresHapticOnWake`, and
`soineAppFiresMediumHapticOnDreamDiscovery` pin the
contract.

## What this slice does NOT do

- **Sheet / dialog transitions** — the V1 ships a single
  transition surface (the secondary controls on the
  sleeping screen). A future polish slice can add
  `SoineAnimatedVisibilitySheet` for the
  `ModalBottomSheet` on the Dream Album and the
  `AlertDialog` on the Settings / Privacy screens.
- **Companion motion** — the V1 motion language is for
  UI motion. The companion itself uses semantic intents
  (IDLE, NOTICE_USER, LOOK_AT_USER, MOVE_CLOSER, …) and
  the future 3D renderer EPIC owns the asset clips. The
  motion language document calls out "companion motion
  > UI motion" as a design principle, but the actual
  clip durations live in
  `app.soine.companion.CompanionAnimationMapping`.
- **Loading skeleton** — the V1 has a single
  `BedtimeDestination.Loading` state with a
  `CircularProgressIndicator`. A future polish slice can
  add a skeleton / placeholder while the controller
  resolves the initial destination.
- **Stage-advance haptic** — the relationship-stage
  presentation slice ([#175][issue-175]) introduces a
  quiet "stage advanced" notice. A future polish slice
  can add a `Medium` haptic on the morning screen when
  the stage has crossed, but the V1 ships without one
  so the first promotion does not feel like a
  celebration.

## Re-evaluation triggers

- A new surface needs a motion token — the surface reads
  `MotionTokens.durationMillis(DURATION_NORMAL, reduceMotion)`
  or `MotionTokens.durationMillis(DURATION_SLOW, reduceMotion)`
  through the same wrapper. No new token is needed; the
  existing scale covers the surface.
- A new haptic moment is added — the
  [`HapticsIntensity`][motion-tokens] enum grows a
  new case; the platform impls extend to support it. The
  call site must pick the right intensity; the
  `MotionLanguageTest` grows a new case to pin the
  moment.
- The future 3D renderer replaces the static fallback —
  the motion language stays for UI motion. The companion
  motion lives in the renderer's animation clips; the
  V1 motion language document is updated to reflect the
  split.

[motion-tokens]: ../composeApp/src/commonMain/kotlin/app/soine/motion/MotionTokens.kt
[soine-animated-visibility]: ../composeApp/src/commonMain/kotlin/app/soine/motion/SoineAnimatedVisibility.kt

[issue-177]: https://github.com/SMRI2170/soine/issues/177
[issue-176]: https://github.com/SMRI2170/soine/issues/176
[issue-175]: https://github.com/SMRI2170/soine/issues/175
[issue-172]: https://github.com/SMRI2170/soine/issues/172
[issue-171]: https://github.com/SMRI2170/soine/issues/171
[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
