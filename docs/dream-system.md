# Dream System

Dreams are collectible memories belonging to the companion.

## Experience

Occasionally, the morning summary reveals a dream card:

```
昨夜の夢
「雲の上で昼寝した」
ふわふわだった。
```

The user obtains dreams by naturally completing sleep sessions, not by active grinding.

## Definition

DreamDefinition contains:

- stable id
- title
- shortLine
- rarity
- minimumFamiliarity
- eligibleSeasons
- artKey
- contentVersion

An empty eligibleSeasons set means the dream is available all year. A seasonal dream is eligible only when the current local season is known and included in that set.

The stable id is the collection identity. Copy or art may evolve without creating a second collectible; such an update keeps the same id and increments contentVersion.

## Catalog repository

DreamDefinitionRepository exposes:

- all authored definitions in stable id order
- lookup by stable id
- eligibility filtering by familiarity and season

The initial implementation is an in-memory repository intended for authored content bundled with the app. It does not require a network or account.

## Discovery

Dream discovery is evaluated once for each completed sleep session.

The default discovery chance is a fixed 30% per completed session. It does not increase with session duration, so unusually long sleep is not rewarded with better odds. The deterministic roll is derived from session identity and the discovery algorithm version, not the amount of time slept.

A discovery evaluation creates a DreamDiscoveryDecision whether or not a dream is found. Persisting that decision makes reopening or retrying the same session idempotent: a previous no-dream result cannot be rerolled.

Before selection:

1. the session must be completed
2. the catalog applies familiarity and season gates
3. already discovered dream IDs are removed
4. the fixed per-session discovery roll is evaluated
5. if successful, one remaining eligible dream is selected using authored rarity weights

Current rarity weights are COMMON 70, UNCOMMON 25, RARE 5. These affect which eligible dream is selected after a successful discovery roll; they do not change the 30% per-session chance itself.

DreamDiscovery stores:

- dreamId
- sessionId
- discoveredAtEpochMillis

DreamDiscoveryDecision stores:

- sessionId
- algorithmVersion
- dreamId or null
- evaluatedAtEpochMillis

## Rules

- discovery is evaluated only for completed sessions
- probability does not scale with extreme sleep duration
- the same session cannot be rerolled after its decision is persisted
- already discovered dreams are excluded from later draws
- relationship and seasonal eligibility are applied before selection
- identical session identity and algorithm version produce the same result
- seasonal/special dreams may exist
- collection remains optional
- no paid random draw mechanic
- unknown season does not unlock seasonal-only dreams
- content updates preserve dream identity

Start with authored dreams. Generative dreams can be explored later only with strict tone, privacy, cost and safety controls.


## Initial authored catalog

InitialDreamCatalog ships 30 authored dreams with stable IDs. Every entry has title, shortLine, rarity, minimum familiarity, season eligibility, artKey, and contentVersion.

The first set deliberately mixes ordinary and slightly magical scenes rather than making rare dreams inherently "better." All four relationship stages are represented, every season has at least two seasonal dreams, and all-year dreams remain the majority so the catalog is useful even when season is unknown.

The initial catalog is bundled locally and exposed through the same DreamDefinitionRepository used by discovery. Copy avoids medical judgement, productivity coaching, guilt, streak language, and rewards for extreme sleep duration.


## Dream Album

The shared Compose Dream Album presents the authored catalog as a calm collection rather than a progression grind. See [`docs/dream-album-redesign.md`](dream-album-redesign.md) for the V1 visual-identity redesign slice ([#172][issue-172]).

- discovered dreams show title, authored short line, a subdued rarity label, and discovery date
- undiscovered dreams hide title, copy, rarity, and art identity behind a silhouette tile (same shape, dimmer palette) so the empty album still has a world-feel
- discovered entries are ordered before undiscovered entries; recent discoveries appear first
- selecting a discovered entry opens an immersive sheet (a `ModalBottomSheet`, not a dialog) with a larger art tile, the dream's short line, rarity, and discovery date
- an explicit empty state explains that dreams are found occasionally after sleeping and does not pressure the user to collect them
- collection cards expose merged accessibility descriptions; undiscovered entries are announced simply as undiscovered
- the first Japanese UI formats discovery dates using the Japan calendar day
- the screen consumes DreamDiscovery records but does not introduce a new persistence layer; persistence/integration remains separate from presentation

## Visual identity (V1, #172)

Each `DreamDefinition.artKey` is mapped deterministically to a `DreamMotif` and a `DreamPalette` so the album reads as a constellation of distinct dreams rather than a row of identical "夢" cards.

- `DreamMotif` is one of eight abstract shapes (`ORB`, `STAR`, `PATH`, `RECTANGLE`, `ARC`, `TRIANGLE`, `CLUSTER`, `CRESCENT`). The shapes are visual mnemonics, not literal illustrations.
- `DreamPalette` is one of six two-color gradients with an accent. Every color comes from the Soine palette (`SoineColors`); the album never introduces a new color.
- The mapping is hash-based and stable across runs, sessions, and devices. A null or blank artKey falls back to the neutral palette and the `ORB` motif so the layout never breaks.
- Undiscovered dreams use the same shape in a dimmer silhouette (the gradient direction is preserved, the accent is muted, the surface is dimmed) so an empty album still has a world-feel instead of a row of "？" placeholders.
- No progress bar, no streak counter, no "login bonus" copy. The header is a single line "X / N 見つけた" so the user can see the count without feeling pressured to collect.

The visual identity is owned by `app.soine.dream.DreamAlbumArt` (the `DreamAlbumArtTile` composable, the `motifFor` / `paletteFor` mappers, and the `DreamPalette` data class). The album screen consumes the tile through `LazyVerticalGrid` so the visual flows as a scrapbook / constellation.


## Local persistence and app integration

Dream discovery state is stored locally as one versioned snapshot containing both evaluation decisions and successful discoveries.

- every completed sleep session may have at most one persisted DreamDiscoveryDecision
- a no-dream result is persisted as a decision with no dream id, preventing rerolls after reopen or retry
- a successful evaluation persists its decision and DreamDiscovery in the same snapshot write
- Dream Album reads discoveries from this repository and refreshes after wake and app startup
- app startup may evaluate the latest completed session if it has no prior decision, covering interruption between sleep completion and dream persistence
- Android uses SharedPreferences and iOS uses NSUserDefaults behind the same common DreamDiscoveryStore contract
- dream storage failure or corruption is isolated from the core sleep lifecycle; a completed sleep session remains completed
- local "delete all" clears dream discovery state together with sleep, audio, and relationship data
