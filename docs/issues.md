# Issue Map

This repository uses issue title prefixes as the primary planning taxonomy.

## Priority

- **P0** — required for the first reliable product loop or release safety.
- **P1** — Soine differentiation: companion, night memories, dreams, relationship.
- **P2** — optional enrichment and post-MVP integrations.

Finish P0 reliability work before expanding P2.

## Epics

| Epic | Priority | Scope |
| --- | --- | --- |
| #1 | P0 | Bedtime → Sleeping → Wake → Morning Summary |
| #2 | P0 | Ambient audio and sleep timer |
| #77 | P0 | Engineering foundation and release quality |
| #3 | P1 | 3D companion and signature bedtime |
| #6 | P1 | Relationship and progression |
| #76 | P1 | Night memories and dreams |
| #4 | P2 | Health Connect / HealthKit |
| #5 | P2 | Optional overnight sound events |

## Category prefixes

- `[CORE]` — session/domain/navigation
- `[UI]` — product UI
- `[AUDIO]` — ambient audio/timer
- `[3D]` — renderer/animation
- `[NIGHT]` — night-event engine
- `[RELATIONSHIP]` — familiarity/progression/routine
- `[DREAM]` — dream system
- `[CONTENT]` — authored dialogue/events/dreams
- `[HEALTH]` — Health Connect / HealthKit
- `[SOUND]` — microphone-derived events
- `[PRIVACY]` — data controls
- `[TEST]` / `[QUALITY]` / `[CI]` — release reliability
- `[DATA]` — persistence/migration
- `[A11Y]` — accessibility
- `[ANALYTICS]` — product analytics

## Recommended implementation order

### Phase 0 — Build safety
#7 → #8 → #9 → #10 → #11 → #63 → #80 → #81.

In parallel: #83 persistence migration design.

### Phase 1 — First usable sleep loop
#12 → #13 → #14 → #15 → #16, then #75 and #78 device recovery checks.

### Phase 2 — Bedtime utility
#20 → #21 → #22/#23 → #24 → #25, then #79 battery benchmark.

### Phase 3 — Signature companion
#26 → #27/#28 → #29 → #30 → #31/#32.

### Phase 4 — Soine-only entertainment
Relationship:
#17 → #18 → #19 → #39 → #40 → #41 → #42 → #74.

Night memories:
#33 → #34 → #35 → #36 → #37 → #38 → #71.

Dream/content:
#43 → #44 → #46 → #45 → #47 → #48/#49 → #73.

### Phase 5 — Optional enrichment
Health: #50 → #51/#52 → #53.

Sound analysis: #54 → #57 → #55/#56 → #58.

Analytics #62 comes after the core event vocabulary is stable.

## Rules for new issues

Each implementation issue should answer:
1. What user or system behavior changes?
2. Which epic owns it?
3. What does it depend on?
4. What is the failure behavior?
5. What proves it is done?
6. Does it affect privacy, battery, lifecycle, accessibility, or both platforms?

Do not create large issues that combine domain, platform, UI and content work unless the issue is explicitly an Epic.

## AI-agent workflow

When handing an issue to a coding agent:

1. Read `AGENTS.md`.
2. Read the parent Epic and linked design docs.
3. Work on one issue at a time.
4. Keep renderer/platform types out of common domain code.
5. Add or update tests.
6. Run relevant checks.
7. Update documentation if behavior changes.
8. Open a PR referencing the issue.

Prefer small PRs that can be reviewed and reverted independently.
