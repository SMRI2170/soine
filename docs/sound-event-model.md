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


## Local persistence and deletion

Derived sound events are stored locally in a versioned snapshot grouped by the
Soine sleep-session id. The snapshot contains only:

- session id
- derived event type
- occurrence timestamp
- normalized confidence
- on-device source
- detector/model version

It contains no raw-audio path, recording identifier, or audio payload.

The privacy screen allows the user to delete all derived events for one sleep
session or delete every stored derived event. Both actions require explicit
confirmation and provide result feedback. Deleting derived sound events does
not delete the underlying sleep session.

"Soineのすべてのローカルデータを削除" also clears the derived sound-event
store.
