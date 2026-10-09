# V1 Accessibility Audit (October 2026)

This document is the audit report for [#195][issue-195]
(Polish 後の全主要画面を TalkBack・VoiceOver・Dynamic Type・
Reduce Motion で再監査する). It enumerates each acceptance
criterion, the source / test evidence that backs it, the items
that must be verified on a real device, and the follow-up issues
that track the device-only items.

Re-run the audit whenever any of the audited screens changes:
`App.kt`, `SettingsScreen.kt`, `DreamAlbumScreen.kt`,
`NightMemoryTimeline.kt`, or the `CompanionStaticFallback` /
`CompanionRenderRequest` surface.

## Scope

The audit covers the post-polish V1 surface area:

- Bedtime (`App.kt` `BedtimeScreen`)
- Sleeping (`App.kt` `SleepingScreen`)
- Morning (`App.kt` `MorningScreen`)
- Dream Album (`DreamAlbumScreen.kt`)
- Settings / Privacy (`SettingsScreen.kt`)
- dialogs / sheets (all `AlertDialog` callers)
- 3D / static fallback controls
  (`CompanionStaticFallback`, `CompanionRenderRequest`)

Out of scope for this audit (tracked separately):

- iOS / Android system settings flows (the user can always
  navigate to them via the OS; Soine is just a destination)
- third-party screen reader behavior outside the documented
  TalkBack / VoiceOver / Dynamic Type / Reduce Motion baselines

## Acceptance criteria vs. evidence

| Acceptance criterion | Source / test evidence | Status |
| --- | --- | --- |
| core sleep start / wake が TalkBack・VoiceOver だけで完遂可能 | `SleepStartWakeCtaSemanticsTest` + the manual screen-reader checklist below | ✅ textual / pending device |
| large text で CTA が消えない | `App.kt` `verticalScroll(rememberScrollState())` on the bedtime / sleeping content areas; settings sections are Column-only with `fillMaxSize` | ✅ textual / pending device |
| reduce-motion で情報が失われない | `AccessibilityBaselineTest` + `ReduceMotionCoverageTest` (15 intents) | ✅ automated |
| 3D scene が accessibility tree を壊さない | `DecorativeCompanionSemanticsTest` pins exactly-one contentDescription on the companion surface | ✅ automated |

The textual evidence above is the deliberate host-test substitute
documented in `docs/ci-quality-policy.md`. The device checklist
below is the follow-up that the textual checks cannot replace.

## Source audit results (automated)

The textual audit is run by `AccessibilitySourceAuditTest`. The
test covers:

- the bedtime "一緒に寝る" CTA and the sleeping "起きる" CTA are
  wired to `AccessibilityPolicy.SLEEP_START_CONTENT_DESCRIPTION`
  and `WAKE_CONTENT_DESCRIPTION` (covered by the #193 PR as well)
- primary sleep / wake CTAs use a 56.dp minimum height so the
  touch target stays reachable at large text scales
- `SettingsScreen` does not regress to icon-only buttons; every
  interactive control carries a visible text label
- `DreamAlbumScreen` and `NightMemoryTimeline` rows merge their
  descendant semantics so TalkBack reads one concise description
  per row instead of a stream of glyph + title + line + date
- the `invisibleToUser` modifier is never applied to a primary
  action; the screen reader must reach every CTAs

`ReduceMotionCoverageTest` walks all 15 `CompanionIntent` values
and asserts the static fallback's placement transition is zero
duration when reduce motion is enabled, and that every intent
exposes a non-blank content description. A future intent that
forgets the reduce-motion branch would be caught.

`DecorativeCompanionSemanticsTest` pins the "exactly one
announceable surface" contract on the companion scene: at least
one `contentDescription` is set, no more than two (one for the
surface, one for the textual overlay), and the `clearAndSetSemantics`
and `invisibleToUser` modifiers are not used on the scene. The
companion is helpfully announced, not stripped from the tree.

## Screen reader traversal (manual, on device)

The textual checks pin the contract; the actual traversal still
needs a real device and a human. The manual checklist for V1:

- [ ] Bedtime: TalkBack traversal reaches the headline, the
  companion surface (one announceable node), the audio / timer
  summary card, the "設定" link, the "夢のアルバム" link, the
  "一緒に寝る" CTA (announces "睡眠を開始する"), and the back
  gesture. Same on VoiceOver.
- [ ] Sleeping: TalkBack traversal reaches the companion surface,
  the audio toggle, the timer button, the "起きる" CTA (announces
  "起床して朝の記録を見る"), and the "設定" link. Same on
  VoiceOver.
- [ ] Morning: TalkBack traversal reaches the night memory
  timeline (one announceable node per row), the "今日をはじめる"
  CTA, and the optional dream discovery link.
- [ ] Dream Album: TalkBack traversal reaches the headline, the
  discovery count, each card (one announceable node per card),
  and the back button. The detail dialog is reachable from each
  card and the close button is announced.
- [ ] Settings: TalkBack traversal reaches the headline, every
  FilterChip, the Switch, the Slider (announces value changes),
  the "プライバシーとデータ" link, the "保存された音イベント"
  sections, and the "戻る" link.
- [ ] Privacy: TalkBack traversal reaches the data deletion CTA
  (announces "すべてのローカルデータを削除"), the per-session
  "削除" button, the "音イベントをすべて削除" button, and the
  confirmation dialogs. The destructive buttons must be the last
  item reached on the page so the user does not activate them
  by accident during a forward scroll.
- [ ] Dialogs: every `AlertDialog` has a labelled `confirmButton`
  and `dismissButton`. The destructive confirm announces
  explicitly (e.g. "削除する" rather than "OK").

## Dynamic Type / large text (manual, on device)

- [ ] At the system "Largest" text size, the bedtime "一緒に寝る"
  CTA must still be reachable on a single screen on a 6.1" phone.
  The bedtime content area is wrapped in
  `verticalScroll(rememberScrollState())` so the CTA can move
  off-screen but remain reachable by swipe.
- [ ] The same must hold for the sleeping "起きる" CTA and the
  morning "今日をはじめる" CTA.
- [ ] The Dream Album card text and the night memory timeline
  must not clip or overflow at "Largest" text size.
- [ ] The Settings FilterChip row ("オフ / 30分 / 60分 / 90分")
  uses a wrap-friendly layout, not a fixed Row, so larger text
  does not push the row off-screen.
- [ ] The Privacy data sections do not regress to a fixed-height
  dialog at large text sizes; the dialog must scroll if needed.

## Reduce motion (automated + manual)

Automated:

- `AccessibilityBaselineTest` pins the sleep intent's
  zero-duration placement.
- `ReduceMotionCoverageTest` pins the zero-duration placement for
  every `CompanionIntent` entry.
- `AccessibilitySourceAuditTest` pins the textual contract on
  the `CompanionScene` block.

Manual (on device, with system "Reduce Motion" enabled):

- [ ] The relationship-distance transition between Bedtime and
  Sleeping becomes zero-duration (no fade, no slide). The
  companion appears in the next placement immediately.
- [ ] The morning-night-memory timeline does not animate entries
  in. Each row appears statically.
- [ ] The "一緒に寝る" / "起きる" CTAs do not animate; they are
  present in their final state.
- [ ] Switching the system "Reduce Motion" preference while a
  session is in progress does not lose session data. The
  `ActiveSessionTimezoneChangeTest` (PR #208) covers the
  data-layer case; the device check covers the visual layer.

## 3D scene (automated + manual)

Automated:

- `DecorativeCompanionSemanticsTest` pins exactly-one
  contentDescription on the companion surface.
- The static fallback is renderer-independent: the
  `CompanionStaticFallback` is the only thing the V1 UI reads,
  so the future 3D renderer's presence or absence does not
  change the accessibility tree.

Manual:

- [ ] When the future 3D renderer is enabled, the accessibility
  tree must still expose exactly one announceable companion
  node. The 3D surface adapter is responsible for setting
  `contentDescription` on its root.
- [ ] The 3D renderer's continuous animation must respect the
  reduce-motion preference. The static fallback path remains
  the deterministic test target.

## Color-only cues (manual)

- [ ] The audio playing state is conveyed by both the speaker
  icon and the visible play/pause label, not by color alone.
- [ ] The recording / deleting progress is conveyed by the
  CircularProgressIndicator and the "削除中" text, not by
  button color alone.
- [ ] The morning summary sections do not rely on color to
  distinguish "今日" from "昨日"; they use the date label.

## Touch target (automated + manual)

Automated:

- `AccessibilityPolicy.MIN_TOUCH_TARGET_DP` is 48.dp. The audit
  source test pins that primary sleep / wake CTAs use 56.dp.
- Every interactive control in the core flow uses at least
  `AccessibilityPolicy.MIN_TOUCH_TARGET_DP.dp` for its
  `heightIn`.

Manual:

- [ ] The Settings FilterChip row chips are each at least 48.dp
  tall (Material3 default).
- [ ] The Slider thumb is at least 48.dp wide (Material3 default).
- [ ] The Switch track meets the 48.dp target.

## Follow-up issues

Items that require a physical device and are NOT part of this
slice:

- TalkBack traversal pass on Android — tracked as the V1
  release audit checklist, owner: release manager
- VoiceOver traversal pass on iOS — same checklist
- Dynamic Type pass at "Largest" — same checklist
- Reduce Motion live-device check — same checklist
- 3D renderer accessibility integration — deferred to the
  renderer EPIC (#27 / #28) so the surface adapter can set
  `contentDescription` on its own root

## Re-evaluation triggers

Re-run the audit when:

- a new screen is added to the V1 surface
- the `App.kt` bedtime / sleeping / morning CTAs are touched
- `CompanionStaticFallback` gains a new intent
- `DreamAlbumScreen` or `NightMemoryTimeline` row layout changes
- `SettingsScreen` adds a new section

[issue-195]: https://github.com/SMRI2170/soine/issues/195
