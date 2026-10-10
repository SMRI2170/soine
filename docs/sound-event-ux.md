# V1 Sound-Event UX

This document is the canonical description of the
overnight sound-event UX. It is the contract slice of
[#178][issue-178] (夜間音イベントを朝の記憶として自然に見せる).
It is the eighth EPIC #167 polish slice that builds on the
V1 design system foundation ([#168][issue-168]) and the
bedtime ([#169][issue-169]), sleeping ([#170][issue-170]),
morning ([#171][issue-171]), Dream Album ([#172][issue-172]),
relationship-stage presentation ([#175][issue-175]),
settings IA ([#176][issue-176]), and motion language
([#177][issue-177]) redesigns.

Re-evaluate when:

- the optional sound analysis ships on a real device
  (PoC for the platform detector)
- a new event type is added to [SoundEventType][sound-event-model]
- a future polish slice adds a per-event timeline view
- the user demands a confidence indicator in the chrome

## Why this exists

Before this slice, the optional overnight sound analysis
produced `SoundEvent` records that lived in the privacy /
data management surface. The user could not see the
events in the morning experience — the only way to know
that "昨夜こんな音があった" was to open Settings → プライバシー
とデータ and scroll to the per-session list. The data
was disconnected from the moment that gave it meaning.

The redesign introduces a single
[`SoundEventSummary`][sound-event-summary] aggregator
that collapses many events into one short phrase, and a
quiet `SoundSummaryReveal` panel in the morning screen
that surfaces the phrase between the night memory
timeline and the summary card. The privacy link "詳細
と削除は設定から" routes the user to the per-session
list when they want to inspect or delete.

## Privacy boundary (preserved)

The presentation layer is intentionally narrow. The
morning screen consumes a
[`SoundEventSummary`][sound-event-summary] that carries
six fields:

- `totalCount`
- `vocalizationCount`
- `loudSoundCount`
- `snoreLikeCount`
- `coughLikeCount`
- `latestOccurredAtEpochMillis`

The data class does not carry the raw `confidence` or
`modelVersion` values. The chrome never displays a
detector confidence, the model version, the audio file
path, or any other identifier. The presentation layer
is the single boundary that enforces the "no medical
claim" copy rule and the "no raw audio" privacy boundary.

The `SoundEvent` records still carry the raw `confidence`
and `modelVersion` for analytics and debugging; they
live behind the privacy-and-data screen and are not
exposed to the morning view.

## Copy pattern

The user-facing line uses the "*のような*" pattern so
the copy never makes a diagnosis. The
[`userFacingLine`][sound-event-summary] function picks
the dominant type (the one with the most events) and
returns one of five phrases:

| Dominant type | Copy |
| --- | --- |
| `SNORE_LIKE` | "寝言のような音や、大きめの音が聞こえたかも" |
| `VOCALIZATION` | "寝言のような小さな音がときどき聞こえたかも" |
| `COUGH_LIKE` | "せきのような音が聞こえたかも" |
| `LOUD_SOUND` | "大きめの音が聞こえたかも" |
| `OTHER` | "何かの音が聞こえたかも" |

The copy uses "かも" (might have) so the line is a
hypothesis, not a fact. The line never says "you snored"
or "you coughed" — the user reads a soft, observation-
level sentence and can move on.

The dominant type selection orders SNORE_LIKE →
VOCALIZATION → COUGH_LIKE → LOUD_SOUND so the calmest /
most-reassuring phrase wins ties. A user with both
snore-like and vocalization events sees the snore-like
phrase first; a user with only loud sounds sees the
loud-sound phrase.

## Privacy link

The `SoundSummaryReveal` panel includes a "詳細と削除は
設定から" `TextButton` that routes the user to the
privacy-and-data screen. The morning screen does not
expose a delete shortcut inline — the destructive
delete action lives behind the navigation, consistent
with the V1 destructive-action hierarchy from the
settings IA redesign ([#176][issue-176]). The
"data deletion must not erase the user's history on a
mis-tap" rule still holds.

## Empty state

When the optional sound analysis did not produce any
events for the session, the morning screen does not
surface a "no events" panel. The empty case is the
default state; the chrome is silent. The
`SoundEventSummary.hasEvents` flag is `false` when
`totalCount == 0`, and the morning screen does not call
the reveal composable in that case.

## What this slice does NOT do

- **Raw audio playback** — explicitly excluded by
  [#178][issue-178]. The presentation layer does not
  carry any audio path, and the user-facing copy never
  references raw audio.
- **Confidence percentage** — the detector confidence
  is not exposed to the morning screen. The user sees
  "寝言のような音", not "寝言 87%".
- **Medical claim** — the copy uses the "*のような*"
  pattern so it never says "you snored" or "you
  coughed". A future polish slice can extend the
  phrase dictionary; the contract is that no phrase
  ever makes a diagnosis.
- **Per-event timeline** — the V1 ships a single one-
  line summary. A future polish slice can add a per-
  session timeline view behind the privacy-and-data
  navigation; the morning screen does not need a
  timeline.
- **False-positive reporting UX** — the issue lists
  this as a future task. The V1 ships without a
  per-event feedback channel; a future polish slice
  can add a "this was wrong" affordance behind the
  privacy-and-data screen.
- **SOUND_REACTION night event integration** — the
  night event engine ([#167 polish EPIC][issue-167])
  ships without a sound-reaction type. A future
  polish slice can add a `SOUND_REACTION` event and
  wire it through `NightEventEngineInput`.

## Re-evaluation triggers

- The optional sound analysis ships on a real device —
  the `userFacingLine` function can grow a per-detector
  phrase if a detector is unusually noisy. The test
  `userFacingLineForEmptySummaryReturnsNull` still
  holds.
- A new event type is added — `SoundEventType` grows
  and `userFacingLine` adds a new phrase. The
  `dominantType` selector picks the new type at the
  end so the calmest phrase wins ties. The
  `userFacingLineDominantTypeSelection` test grows a
  new assertion.
- The user demands a confidence indicator — the data
  class is intentionally narrow; a future slice adds
  a `confidence: ConfidenceLevel` field where
  `ConfidenceLevel` is an enum (Low / Medium / High).
  The morning screen renders the level, not the
  percentage.
- The privacy-and-data screen adds a "this was wrong"
  affordance — the morning screen does not change. The
  feedback lives behind the navigation, consistent with
  the V1 destructive-action hierarchy.

[sound-event-summary]: ../composeApp/src/commonMain/kotlin/app/soine/sound/SoundEventSummary.kt
[sound-event-model]: ./sound-event-model.md

[issue-178]: https://github.com/SMRI2170/soine/issues/178
[issue-177]: https://github.com/SMRI2170/soine/issues/177
[issue-176]: https://github.com/SMRI2170/soine/issues/176
[issue-175]: https://github.com/SMRI2170/soine/issues/175
[issue-172]: https://github.com/SMRI2170/soine/issues/172
[issue-171]: https://github.com/SMRI2170/soine/issues/171
[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
