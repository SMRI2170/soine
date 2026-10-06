# Night Event Reproducibility

Night events are keyed by `NightEventGenerationMetadata(sessionId, seed, algorithmVersion)`.

- Seed derivation is stable and based on the completed session identity and its start/end epoch timestamps.
- Selection code must use `NightEventRandom`, not platform/Kotlin default randomness.
- Once metadata exists for a session, re-opening that night reuses the stored `seed` and `algorithmVersion`.
- Shipping a new algorithm version affects only nights without existing generation metadata.
- Seed derivation itself is version-independent. Selection behavior changes behind `algorithmVersion`, preserving compatibility with previously generated nights.
