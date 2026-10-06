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

DreamDiscovery stores:

- dreamId
- sessionId
- discoveredAtEpochMillis

Discovery probability and duplicate control are separate from catalog eligibility and are implemented by the discovery domain.

## Rules

- probability does not scale with extreme sleep duration
- duplicates should be controlled
- seasonal/special dreams may exist
- collection remains optional
- no paid random draw mechanic
- unknown season does not unlock seasonal-only dreams
- content updates preserve dream identity

Start with authored dreams. Generative dreams can be explored later only with strict tone, privacy, cost and safety controls.
