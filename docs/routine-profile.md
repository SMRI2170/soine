# Routine Profile

Soine may learn a small set of local routine facts so the companion can react naturally without turning sleep into a score.

## Inputs

A RoutineObservation contains only the information needed for routine inference:

- completed sleep-session ID
- local bedtime minute of day
- local wake minute of day
- weekday/weekend classification
- ambient sound ID when an ambient sound was actually used

Calendar/time-zone conversion belongs at the platform boundary. The shared generator receives local clock values and contains no platform date types.

## Minimum evidence

Soine deliberately returns null rather than making a weak claim.

- typical bedtime: at least 5 completed observations
- typical wake time: at least 5 completed observations
- frequent ambient sound: at least 3 sessions where a sound was used, no top tie, top sound used in at least half
- weekday/weekend tendency: at least 3 weekday and 3 weekend observations
- weekday/weekend difference: at least 45 minutes in bedtime or wake time

10 or more time observations raise the bedtime/wake estimate to high confidence. Day-type confidence becomes high at 6 or more weekdays and 4 or more weekends.

## Time-of-day behavior

Clock time is circular. 23:50 and 00:10 are treated as nearby rather than averaging toward noon. The typical range uses the central 60% of observations so one unusually early or late night does not define the routine.

## Privacy

Routine inference is local-only. A profile is a derived convenience signal, not a medical or sleep-quality assessment. Missing or irregular observations must fall back to generic dialogue rather than negative feedback.
