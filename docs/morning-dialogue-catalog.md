# Morning Dialogue Catalog

The initial morning catalog contains at least 50 authored Japanese lines. It is split across five content kinds:

- GENERIC: quiet fallback lines that require no inferred context
- EVENT: two or more variants for every current NightEventType
- DREAM: lines that are eligible only after an actual dream discovery
- ROUTINE: variants attached to confidence-gated routine triggers
- RELATIONSHIP: lines unlocked by familiarity stage

The shared CompanionDialogueSelector keeps the existing priority order:

1. dream discovery
2. notable night event
3. supported routine observation
4. relationship familiarity
5. generic fallback

The catalog never turns short sleep into negative feedback and does not contain medical, diagnostic, treatment, or productivity judgement. Routine-specific variants are only reached after RoutineDialogueSelector has already verified sufficient evidence.

Recent dialogue IDs are filtered by cooldown before selection. Multiple event, relationship, routine, and generic variants make it possible to rotate wording instead of repeating the same sentence on consecutive mornings.

Dream-specific content uses the authored DreamDefinition short line first. Catalog dream lines provide additional authored variation in deterministic test mode and future presentation flows, while the dream source as a whole has a longer cooldown.
