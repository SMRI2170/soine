# V1 Settings & Privacy IA

This document is the canonical description of the Settings
& Privacy information architecture. It is the contract
slice of [#176][issue-176] (設定・プライバシー画面を機能一覧
から分かりやすい information architecture へ改善する). It is the
sixth EPIC #167 polish slice that builds on the V1 design
system foundation ([#168][issue-168]) and the bedtime
([#169][issue-169]), sleeping ([#170][issue-170]), morning
([#171][issue-171]), Dream Album ([#172][issue-172]), and
relationship-stage presentation ([#175][issue-175]) redesigns.

Re-evaluate when:

- a new optional integration is added (e.g. cloud backup)
- the companion visual / motion options ship (#173)
- a destructive action is introduced outside the privacy
  boundary
- the privacy policy or data inventory changes

## Why this exists

Before this slice, the settings screen was a vertical list
of feature rows: ambient sound, sleep timer, Health,
overnight sound analysis, privacy-and-data navigation,
replay-onboarding, app version. The screen was functional
but it read like a developer-facing feature list. The user
had to know that "Health" and "夜間の音解析" are both
optional integrations, that "プライバシーとデータ" is a
navigation rather than a section, and that "オンボーディン
グをもう一度見る" is a teaching surface.

The redesign groups the screen into five named categories
that mirror the way a user actually thinks about Soine:
"what I touch every night", "what the companion is",
"what is connected", "what data lives on my device", and
"what is the app version".

## Hierarchy

The main `SettingsScreen` is a single `verticalScroll`
column divided into five named groups, in this exact order:

1. **睡眠について** — every-night touch settings. Contains
   two `SoinePanel` surfaces:
   - 環境音: ON / OFF switch + sound chips + volume slider
   - スリープタイマー: オフ / 30分 / 60分 / 90分 chips
2. **相棒について** — companion visual / motion options.
   The V1 ships a quiet "coming soon" placeholder inside a
   single `SoinePanel`. The future 3D renderer EPIC
   ([#173][issue-173]) owns the live options; the
   placeholder sets the expectation that the section will
   grow without becoming a dead end.
3. **連携（任意）** — optional integrations. A single
   `SoinePanel` with two integration rows (Health, 夜間の
   音解析) and a switch for the sound analysis toggle. The
   row label + status pair (e.g. "夜間の音解析 — オフ
   （任意）") is the screen-reader entry; the visual row is
   a one-line description.
4. **プライバシーとデータ** — privacy and data
   management. A single `SoinePanel` with one short
   summary sentence ("Soine のデータは端末内に保存しま
   す。") and a navigation button to the dedicated
   `PrivacyDataScreen`. The destructive "delete all data"
   action lives behind this navigation, not on the main
   screen, so a mis-tap does not erase the user's
   history.
5. **このアプリについて** — version, replay-onboarding
   action. A single `SoinePanel` with the version label
   and the replay-onboarding text button.

The five group labels are pinned in source order by
`SettingsScreenLayoutTest.settingsScreenFollowsIaHierarchy`,
which guards against an accidental reorder.

## Design system adoption

The main screen adopts the V1 design system throughout:

- `SoinePanel` for every group container
- `SoineSectionHeader` for sub-section labels (e.g.
  "寝るときの標準の音" inside the 環境音 group)
- `SoineTokens.Spacing*` for spacing
- `SoineColors.cream` for primary text, `SoineColors.hush`
  for sub-text
- `SoineQuietButton` is intentionally not used on the
  main screen because every primary surface is a full-
  width `SoinePanel`-wrapped row. The
  `SETTINGS_REPLAY_ONBOARDING_CONTENT_DESCRIPTION` is
  exposed on the replay-onboarding `TextButton` for screen
  reader access.

The previous "Card" wrapper in the PrivacyDataScreen
subscreen is preserved (it predates the design system
foundation and is not in scope for the V1 IA slice); a
future polish issue can migrate it to `SoinePanel` when
the privacy subscreen gets a full redesign.

## Permission state visual organization

The "夜間の音解析" row has three permission states that
the screen surfaces visually:

| Microphone state | Status label | Switch state | Inline note |
| --- | --- | --- | --- |
| `GRANTED` + analysis on | "オン（端末内で解析）" | checked, enabled | none |
| `GRANTED` + analysis off | "オフ（任意）" | unchecked, enabled | none |
| `DENIED` | "オフ（任意）" | unchecked, enabled | "寝言や大きな音…" hint |
| `PERMANENTLY_DENIED` | "マイク許可が必要です" | unchecked, enabled | OS-level deep-link note |
| `UNAVAILABLE` | "この端末では利用できません" | unchecked, disabled | "この端末では利用できません" |

The "unavailable" path is a soft-disabled state: the
switch is `enabled = false`, the row's status label says
"この端末では利用できません", and an inline note repeats
the same phrase below. The user can read the screen
without the switch appearing broken.

`SettingsScreenLayoutTest.settingsScreenPermissionStateHasUnavailablePolish`
pins the contract.

## Destructive action hierarchy

The destructive "delete all data" action lives behind the
"プライバシーとデータを開く" navigation. A mis-tap on the
main settings screen does not erase the user's history;
the user has to:

1. Tap "プライバシーとデータを開く" (a non-destructive
   navigation)
2. Scroll to the "データ削除" section
3. Tap "すべてのローカルデータを削除" (a styled `Button` in
   `SoineColors.ember` to signal the destructive intent)
4. Confirm in an `AlertDialog`

The four-tap path is intentional. A single-tap delete is
exactly the kind of "loss-aversion streak" the V1 product
direction refuses to introduce.

`SettingsScreenLayoutTest.settingsScreenDestructiveActionLivesBehindNavigation`
pins the contract.

## Privacy copy shortening

The new "プライバシーとデータ" main-screen copy is one short
sentence:

> Soine のデータは端末内に保存します。

The full inventory (sleep sessions, relationship state,
night memories, derived sound events) and the deletion
controls live behind the navigation. The main screen
gives the user a one-glance answer to "where does my data
live" without a wall of text.

The full privacy policy, data inventory, and deletion
controls continue to live in `PrivacyDataScreen`.

## About group

The "このアプリについて" group contains:

- the app version (e.g. "Soine v1.0.0") in `bodySmall`
  with `SoineColors.hush`
- the replay-onboarding action as a `TextButton` with the
  `SETTINGS_REPLAY_ONBOARDING_CONTENT_DESCRIPTION`

The group is intentionally last. The version is a small
label, not a primary surface; the replay-onboarding is a
teaching action that the user can reach but does not have
to.

## What this slice does NOT do

- **The companion visual / motion options** — the "相棒に
  ついて" group is a quiet placeholder until the future
  3D renderer EPIC ([#173][issue-173]) ships the live
  options. The placeholder sets the expectation without
  becoming a dead end.
- **The Health connection** — the integration row shows
  "未接続（任意）" with a status label, not a connect
  button. The Health Connect / HealthKit integration is
  a future polish slice; the V1 ships the row so the user
  knows the option exists.
- **The deep-link to OS settings** — the
  `PERMANENTLY_DENIED` path exposes the OS settings link
  through the existing `onOpenMicrophoneSettings`
  callback. A future polish slice can add a consistent
  "open OS settings" button to all permission rows.
- **The `PrivacyDataScreen` redesign** — the subscreen
  keeps its `Card`-based layout. A future polish issue
  can migrate the subscreen to `SoinePanel` and add a
  proper "stored data" inventory.

## Re-evaluation triggers

- The "相棒について" group grows live options (post-#173) —
  the placeholder copy is replaced; the test for the
  "coming in the next update" string is removed.
- A new optional integration is added — a new
  `IntegrationRow` is added to the "連携（任意）" group
  with the same label + status pattern. The order is
  alphabetical or by recency of enablement; the test
  pins the source-order of the group.
- A destructive action is added outside the privacy
  boundary — the action moves behind a navigation like
  the "delete all data" path, or the IA grows a new
  group.
- A new permission state is introduced — the
  `soundAnalysisStatusLabel` grows a new branch; the
  test for `UNAVAILABLE` is extended to the new state.

[issue-176]: https://github.com/SMRI2170/soine/issues/176
[issue-175]: https://github.com/SMRI2170/soine/issues/175
[issue-173]: https://github.com/SMRI2170/soine/issues/173
[issue-172]: https://github.com/SMRI2170/soine/issues/172
[issue-171]: https://github.com/SMRI2170/soine/issues/171
[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
