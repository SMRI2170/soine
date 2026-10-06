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
