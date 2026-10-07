# Sound event domain model

`SoundEvent` is the commonMain representation of a sound pattern derived from
optional overnight microphone analysis.

## Fields

- `type` — normalized product-facing category
- `occurredAtEpochMillis` — wall-clock occurrence time
- `confidence` — normalized detector confidence from `0.0` to `1.0`
- `source` — provenance of the derived event
- `modelVersion` — detector/model version used to produce the event

## Initial types

- `VOCALIZATION`
- `LOUD_SOUND`
- `SNORE_LIKE`
- `COUGH_LIKE`
- `OTHER`

The `*_LIKE` names intentionally describe an acoustic pattern rather than a
medical diagnosis.

## Privacy boundary

The domain model contains no raw-audio file path, recording identifier, or
audio payload. Platform detection spikes (#55 / #56) must convert microphone
input into derived `SoundEvent` values before data enters common domain logic.

Raw audio remains non-persistent by default.
