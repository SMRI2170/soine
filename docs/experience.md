# Soine Experience Design

## Product fantasy

Soine is not primarily a sleep score app. It is the feeling that a small creature really spent the night beside you.

The unique loop is:

```
meet at night
-> companion notices you
-> settle down together
-> real-world sleep
-> unseen night events
-> wake together
-> discover what happened last night
-> relationship changes subtly
-> return tonight
```

## Signature moment

When the user taps "一緒に寝る":

1. the companion looks toward the user
2. it moves slightly closer
3. curls into its sleeping pose
4. closes its eyes
5. breathing begins
6. controls gradually become visually quiet
7. the display is allowed to dim/lock

This transition should become Soine's recognizable interaction.

## Morning hierarchy

Do not lead with a score.

1. companion reaction
2. memory from last night
3. duration / bedtime / wake time
4. relationship change or discovery
5. detailed data

Emotion -> memory -> data.

## Discovery instead of daily rewards

No generic login bonus. The reason to return is uncertainty about the companion:

- a new sleeping pose
- moving closer
- a tiny night event
- a dream
- a different morning line
- seasonal room behavior

Events should be small enough that missing one never feels punitive.

## Physical relationship

Relationship is expressed visually, not primarily as a numeric level.

Examples:
- early: companion sleeps farther away
- familiar: it settles closer
- bonded: it voluntarily moves near the user's side
- special moments: new sleeping orientation or reaction

Never imply romantic/sexual behavior. The fantasy is calm companionship.

## Real-world context

Optional context can influence presentation:
- local time
- season
- weather, if the user opts into location/weather use
- selected ambient sound
- recent sleep routine

Context changes flavor, not core functionality.

## Anti-patterns

Avoid:
- aggressive streaks
- sleep score shame
- loot-box pressure
- rewarding excessive sleep duration
- notifications designed to create anxiety
- pretending phone-only inference is medical truth


## Bedtime signature integration

After the sleep session has been persisted successfully, Soine starts the companion signature independently:

1. LOOK_AT_USER
2. MOVE_CLOSER
3. CURL_UP
4. SLEEP
5. BREATHE

The semantic sequence is renderer-neutral and uses the relationship stage for placement. The shared UI mirrors the current semantic intent while a platform renderer may render the same request.

The final BREATHE state marks the UI as quiet. Nonessential session labels are suppressed, while essential wake and audio/timer controls remain available.

The sleep session is created before this sequence starts. Renderer submission failures are swallowed by BedtimeSignatureController, so 3D/animation failure cannot roll back or block the active sleep session. Waking or deleting local data cancels an in-flight signature job.
