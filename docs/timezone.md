# Time, Timezone, and Wall-Clock Safety

This document is the canonical description of how Soine handles
absolute time, display timezones, and wall-clock skew. It is the
contract slice of [#196][issue-196] (timezone・日付変更・wall-clock
変化に強いsleep/dream/night-memory時刻処理へ統一する).

Re-evaluate when:

- a new persistence surface stores time,
- a new display surface needs to render a date or time,
- the launch region leaves Japan,
- the device is expected to travel across a timezone boundary
  while a sleep session is in progress.

## Why this exists

A build that stores absolute epoch millis everywhere can still
ship a regression: a hard-coded JST offset, a duration that flips
negative when the user manually moves the clock back, or an active
session that re-renders the wrong morning summary because the
device timezone changed mid-night. The acceptance criteria for
#196 are:

- timezone 変更で session data 自体を書き換えない
- duration が負にならない
- 現在timezoneに応じて日付/時刻を表示
- Japan-only V1 でも旅行時に破綻しない

The boundary this document defines is the one place that enforces
those four lines.

## Data model — absolute time only

All persisted time is `Long` epoch millis. The storage-facing types
in `commonMain`:

- `SleepSession.startedAtEpochMillis`, `endedAtEpochMillis`,
  `createdAtEpochMillis`, `updatedAtEpochMillis`
- `SleepSessionRecord.*` (same shape, plus `schemaVersion`)
- `SoundEvent.occurredAtEpochMillis`
- `DreamDiscovery.discoveredAtEpochMillis`
- `NightEvent.occurredAtEpochMillis`

None of these fields consult a timezone. The renderer is the only
place that converts an absolute instant into a wall-clock string,
and the formatter is the only place that picks a localized style.

## Display boundary — LocalTimeZone

`app.soine.time.LocalTimeZone` is the single point where the device
offset is queried. It is a stateless interface with one method:

```kotlin
fun utcOffsetMillisAt(epochMillis: Long): Int
```

V1 ships two implementations:

- `JapanLocalTimeZone` — fixed UTC+09:00. Japan has not observed
  DST since 1951, so a constant is sufficient.
- `UtcTimeZone` — used by tests to verify the abstraction actually
  switches behaviour when the offset changes.

`LocalTimeZones.current` is a mutable binding that the app wires
at startup. Renderer code reads it at call time so a swap is
reflected immediately without any cache invalidation. The binding
is a plain `var`; the renderer is single-threaded (the Compose UI
dispatcher) and the binding is only mutated at app start. KMP
`commonMain` does not have `@Volatile`; a plain `var` is sufficient
and is the choice documented in `LocalTimeZone.kt`.

## Display boundary — DisplayFormatter

`app.soine.time.format.DisplayFormatter` is the single point where
an absolute instant becomes a user-readable string. It is a
stateless interface with two methods:

```kotlin
fun formatDiscoveryDate(epochMillis: Long, timeZone: LocalTimeZone): String
fun formatNightEventTime(epochMillis: Long, timeZone: LocalTimeZone): String
```

The formatter does not consult `LocalTimeZones.current` on its
own; the caller passes the zone. That keeps the formatter pure and
testable, and it makes the contract between the two bindings
explicit.

V1 ships one implementation:

- `JapaneseDisplayFormatter` — emits the existing Soine V1 shape
  (`yyyy年MM月dd日` for dates, `HH:mm` for night times).

`DisplayFormatters.current` is a mutable binding that mirrors
`LocalTimeZones.current`. The two bindings are independent so the
launch region and the display style can move at different
cadences; e.g. a future build that supports `en-US` swaps the
formatter but keeps `JapanLocalTimeZone` for users in Japan.

## Wall-clock safety in SleepSession

`SleepSession.durationMillis()` is the only place the session
materializes a duration. It uses `.coerceAtLeast(0)` so a
wall-clock backward jump cannot emit a negative duration:

```kotlin
fun durationMillis(): Long? {
    val start = startedAtEpochMillis ?: return null
    val end = endedAtEpochMillis ?: return null
    return (end - start).coerceAtLeast(0)
}
```

`SleepSession.start()` is guarded so a stray CTA tap cannot
reset an in-flight or finished session:

```kotlin
fun start(now: Long = currentTimeMillis()): SleepSession =
    if (state != SleepState.READY) this
    else copy(state = SleepState.SLEEPING, startedAtEpochMillis = now, endedAtEpochMillis = null)
```

Together, these two guards satisfy the four acceptance criteria:

- a timezone change never touches a session field because the
  session has no timezone-aware state to read
- duration is mathematically non-negative
- the formatter reads `LocalTimeZones.current` so the displayed
  time follows the user's current offset
- the contract is locale-agnostic; the V1 Japan-only launch is a
  binding choice, not a hard-coded offset

## What the UI must not do

The UI layer is the only place a wall-clock string is rendered.
A future contributor adding a new screen or recomposition that
shows a date / time must:

1. Read the value through a formatter — never compute a wall
   clock inline.
2. Pass the timezone explicitly to the formatter — never assume
   `JapanLocalTimeZone` directly.
3. Persist the absolute epoch millis, never the wall-clock string.

The smoke tests in `commonTest` pin each of these:

- `SleepSessionWallClockSafetyTest` — forward / backward clock,
  duration coercion, no negative `SleepSummary` derived fields.
- `ActiveSessionTimezoneChangeTest` — in-flight session survives a
  `LocalTimeZones.current` swap byte-for-byte; `SleepSessionRecord`
  equality is preserved across swaps.
- `DisplayFormatterBoundaryTest` — `DisplayFormatters.current` can
  be swapped to a non-default style without touching call sites.
- `LocalTimeZoneTest` — `formatNightEventTime` and
  `formatJapaneseDiscoveryDate` follow the binding; the same
  epoch produces different strings under JST vs UTC; midnight
  crossing arithmetic stays correct.

## Locale extension — the path for V1.x

V2 or a future V1.x patch that ships in a second region adds:

1. A new `LocalTimeZone` implementation, e.g.
   `AmericaNewYorkLocalTimeZone` (with DST awareness).
2. A new `DisplayFormatter` implementation, e.g.
   `EnglishDisplayFormatter` (`"Oct 8, 2026"` / `"7:13 AM"`).
3. App-start wiring that reads the device locale and timezone,
   picks the right pair, and binds them to
   `LocalTimeZones.current` and `DisplayFormatters.current`.

No call site in the UI changes. No storage schema changes. No
session data is rewritten.

## JST offset audit

The only places the literal JST offset lives in the codebase:

- `composeApp/.../time/LocalTimeZone.kt` — the
  `JapanLocalTimeZone` constant.
- `composeApp/src/commonTest/.../time/LocalTimeZoneTest.kt` — the
  test that pins the constant.

Every other call site routes through `LocalTimeZones.current` or
takes a `LocalTimeZone` parameter explicitly. The audit is a
single `grep` and is checked at code review.

[issue-196]: https://github.com/SMRI2170/soine/issues/196
