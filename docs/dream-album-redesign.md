# V1 Dream Album Redesign

This document is the canonical description of the Dream Album
redesign. It is the contract slice of
[#172][issue-172] (夢のアルバムを収集したくなる visual collection
へ改善する). It is the fourth EPIC #167 polish slice that builds
on the V1 design system foundation ([#168][issue-168]) and the
bedtime ([#169][issue-169]), sleeping ([#170][issue-170]), and
morning ([#171][issue-171]) redesigns.

Re-evaluate when:

- the dream catalog grows beyond 30 dreams
- a new motif / palette primitive is needed
- the detail surface becomes a full page (e.g. with related
  night-memory references)
- the visual identity moves from abstract shapes to authored
  illustrations

## Why this exists

Before this slice, the Dream Album was a vertical list of
identical cards. Every discovered dream showed a "夢" glyph in
a soft circle; every undiscovered dream showed a "？" in the
same circle. The list read like a flat checklist, not a
collection. The user had to read the title to know what they
were looking at, and the empty album felt like an error state
rather than a quiet collection waiting to be filled.

The redesign treats the album as a constellation. Each dream
has its own visual identity — a gradient + a simple motif
shape — so a list of 30 dreams reads as 30 distinct memories.
The undiscovered state is a silhouette of the same shape in a
dimmer palette, so the empty album still has a world-feel and
the user can see the shape of what's still to come.

## Layout

The Dream Album is a 2-column `LazyVerticalGrid` so the visual
flows as a scrapbook / constellation. Each cell has two
layers:

1. **Art tile** — a square `DreamAlbumArtTile` that renders
   the dream's gradient + motif shape. The discovered tile is
   the full visual identity; the undiscovered tile is the
   silhouette (same gradient direction, same shape, dimmer).
2. **Title** — discovered dreams show the dream's title in
   `titleSmall`; undiscovered dreams show "？？？" in the
   `hush` color.

The screen header is a single `Row` with the "もどる" quiet
button, the screen title "夢のアルバム", and a one-line count
"N / 30 見つけた" in the hush color. There is no progress bar
and no streak counter — the count is a memory, not a goal.

The empty state (when no dreams have been discovered yet) is
a `SoinePanel` with two short lines:

- "まだ夢は見つかっていません"
- "眠った朝に、ときどき新しい夢を見つけます。急いで集めなくても
  大丈夫です。"

The second line is the loot-box-pressure guard. The album
never says "collect them all" or "login for a daily dream";
the empty state explicitly says the user does not need to
rush.

## Detail surface

The detail surface is a `ModalBottomSheet` (an immersive
sheet / page), not an `AlertDialog`. The sheet is opened by
tapping a discovered cell; tapping outside the sheet or the
"閉じる" button closes it.

The sheet shows:

1. A large art tile (1.6:1 aspect ratio) with the dream's
   full visual identity.
2. The dream's title in `headlineSmall`.
3. A "記憶" section header followed by the dream's short
   line in `bodyLarge`.
4. The dream's gentle rarity label in `labelLarge`.
5. The "見つけた日 YYYY年M月D日" in `bodyMedium`.
6. A "閉じる" text button aligned to the end of the sheet.

The sheet's `containerColor` is `SoineColors.dusk` so the
sheet feels like the album below, not a separate dialog. The
`skipPartiallyExpanded` flag (or the new `initialValue =
SheetValue.Hidden` initial state) keeps the sheet either
fully open or fully closed; there is no half-state to confuse
the screen reader.

## Visual identity

The visual identity is owned by `app.soine.dream.DreamAlbumArt`.
A `DreamDefinition.artKey` is mapped deterministically to a
`DreamMotif` and a `DreamPalette`:

- `DreamMotif` is one of eight abstract shapes: `ORB`,
  `STAR`, `PATH`, `RECTANGLE`, `ARC`, `TRIANGLE`, `CLUSTER`,
  `Crescent`. The shapes are visual mnemonics, not literal
  illustrations — a moon is an `ORB`, a constellation map is
  a `PATH` of small dots, a paper boat is a `TRIANGLE`.
- `DreamPalette` is one of six two-color gradients with an
  accent. Every color comes from the Soine palette; the album
  never introduces a new color.

The mapping is hash-based and stable across runs, sessions,
and devices. A null or blank artKey falls back to
`DreamMotif.ORB` and `DreamPalette.Neutral` so the layout
never breaks. The catalog can grow without re-bumping the
visual identity — the existing motifs and palettes cover the
new dreams.

The initial 30-dream catalog uses at least 4 different motifs
and 4 different palettes, so the visual variety is strong.
The exact distribution depends on the hash; the contract is
"at least 4 of each", not "exactly N of each", so a future
catalog growth does not need to re-pin the test.

## Silhouette state

The undiscovered tile is a silhouette, not a "？". The
gradient direction is preserved (start = the discovered
tile's end color, end = `SoineColors.midnight` at 0.85 alpha)
and the motif shape is the same, but the accent is dimmed
to the `hush` color at 0.6 alpha. The whole tile is wrapped
in `Modifier.alpha(0.85f)` so the silhouette reads as
"asleep, not yet awake" — the user can see the shape of
what's still to come, which makes the empty album feel like
a waiting collection, not a list of placeholders.

## Accessibility

- **Touch targets** — the back button is a `SoineQuietButton`
  (48dp minimum). The detail sheet's close button is a
  `TextButton` with `heightIn(min = SoineTokens.QuietCtaHeight)`.
- **Content descriptions** — every cell merges its descendant
  semantics so TalkBack / VoiceOver read the dream as one
  item: "title。rarity。見つけた日 YYYY年M月D日" for
  discovered cells, "未発見の夢" for the silhouette. The
  art tile is exposed with the same label so the screen
  reader does not double-read "image" + the title.
- **No invisibleToUser on primary action** — the cell's
  clickable is gated on `entry.discovered`, so a tap on a
  silhouette is a no-op rather than a hidden-button
  surprise. The textual check is already pinned by
  `AccessibilitySourceAuditTest.noScreenUsesInvisibleToUserOnPrimaryAction`.
- **Reduce motion** — the art tile is a static `Canvas`
  gradient; there is no animation. The `ModalBottomSheet`'s
  slide animation is the only motion in the screen, and it
  honors the platform's reduce-motion preference.

## What this slice does NOT do

- **Authored illustrations** — the album renders abstract
  shapes, not literal art. A future polish slice can swap
  the `DreamAlbumArtTile` for an `Image` with a per-dream
  asset, but the V1 album ships the abstract identity so the
  catalog is not gated on illustration production.
- **Discovery animation** — the issue task
  "dream 発見時の discovery animation" is deferred. The
  current path is: a dream is discovered in the morning
  reveal, the user sees the "今朝の発見" panel in
  `MorningScreen`, and the next time they open the album
  the new cell is in the discovered state. A future polish
  slice can add a `LaunchedEffect`-driven reveal in the
  album itself.
- **Constellation / star-map variant** — the issue mentions
  "grid / scrapbook / constellation". The V1 ship is the
  grid (2-column). A constellation variant that connects
  discovered dreams with faint lines is a future polish
  slice; the data layer is already shape-compatible.
- **Loot-box / collection pressure** — explicitly avoided.
  No progress bar, no streak counter, no "complete the
  set" copy, no login bonus. The empty state and the
  header copy say "you do not need to rush" and "you have
  found N of 30" so the album reads as a memory surface,
  not a goal.

## Re-evaluation triggers

- The catalog grows beyond 30 dreams — the test
  `dreamAlbumInitialCatalogUsesMoreThanOneMotifAndOnePalette`
  still pins the visual variety, but a future polish slice
  can add a curated "constellation layout" that groups
  dreams by season or relationship stage.
- A new motif is added — `DreamMotif` grows and the
  `motifFor` mapping is automatically diversified. The
  layout test does not need to change.
- A new color is added to the Soine palette — the
  `DREAM_PALETTES` list grows; the test
  `palettesAreComposedFromSoineColorsOnly` still passes
  because the test enumerates the Soine tokens dynamically.
- The detail surface becomes a full page (e.g. with
  related night-memory references) — the
  `ModalBottomSheet` swaps for a `NavHost` route. The
  `DreamAlbumEntry` data class carries everything the
  detail needs.

[issue-172]: https://github.com/SMRI2170/soine/issues/172
[issue-171]: https://github.com/SMRI2170/soine/issues/171
[issue-170]: https://github.com/SMRI2170/soine/issues/170
[issue-169]: https://github.com/SMRI2170/soine/issues/169
[issue-168]: https://github.com/SMRI2170/soine/issues/168
[issue-167]: https://github.com/SMRI2170/soine/issues/167
