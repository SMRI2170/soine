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

## Data

```
DreamDefinition
- id
- title
- shortLine
- rarityBand
- requiredRelationshipStage?
- season?
- artKey

DreamDiscovery
- dreamId
- sessionId
- discoveredAt
```

## Rules

- probability does not scale with extreme sleep duration
- duplicates should be controlled
- seasonal/special dreams may exist
- collection remains optional
- no paid random draw mechanic

Start with authored dreams. Generative dreams can be explored later only with strict tone, privacy, cost and safety controls.
