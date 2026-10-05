# Relationship System

## Goal

Make the companion feel increasingly familiar with the user's routine without exposing a conventional XP grind.

## Internal state

```
RelationshipState
- totalCompletedSleepMillis
- completedSessions
- familiarity
- discoveredBehaviors
- lastMilestone
- recentRoutineProfile
```

Familiarity may be numeric internally, but UI primarily communicates it through behavior.

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

## Progression

Candidate landmarks:
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
