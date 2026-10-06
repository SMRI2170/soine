# Relationship System

## Goal

Make the companion feel increasingly familiar with the user's routine without exposing a conventional XP grind.

## Internal state

```
RelationshipState
- totalCompletedSleepMillis
- completedSessions
- familiarity (persisted stage value)
- familiarityRuleVersion
- discoveredBehaviors
- achievedMilestones
- processedSessionIds
```

Familiarity is numeric only as a persisted compatibility value. Product/UI code should use the semantic stage.

## Familiarity stages (rule version 2)

| Stage | Requirement |
| --- | --- |
| NEW | no completed nights |
| WARMING_UP | at least 1 completed night |
| FAMILIAR | at least 7 completed nights **and** 24 total hours |
| CLOSE | at least 30 completed nights **and** 100 total hours |

Later stages require both repeated nights and accumulated time. This prevents one abnormally long session from rapidly advancing the relationship and avoids directly rewarding excessive sleep duration.

The thresholds are versioned by `FamiliarityPolicy.CURRENT_VERSION`. Persisted v1 state is re-evaluated against the current balanced rule during decode; processed session IDs, discovered behavior IDs and achieved milestone IDs are preserved.

## Observable changes

- sleeping distance
- time before settling
- probability of looking at the user
- morning dialogue familiarity
- unlocked sleep poses
- dream/event pools
- small room interactions

## Routine memory

The app may derive local, non-sensitive routine facts:
- typical bedtime range
- typical wake range
- frequently selected sound
- weekday/weekend tendency

Examples:
- "今日はいつもより早いね"
- "雨の音にする？"

Do not generate claims that are unsupported by stored observations.

## Progression landmarks

- first night
- 7 completed nights
- 30 completed nights
- 100 slept-together hours
- 300 hours
- 500 hours

Landmarks unlock possibilities, not mandatory content.

## Guardrails

Do not:
- increase rewards linearly with sleep duration
- penalize short/irregular nights
- create loss-aversion streaks
- make the companion disappointed by health-related absence
