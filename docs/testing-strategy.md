# Testing Strategy

## commonMain unit tests

Must cover:
- legal/illegal session transitions
- idempotent start/end
- duration edge cases
- process recovery decisions
- progression increments exactly once
- milestone thresholds
- deterministic night-event generation
- event repetition constraints
- dream eligibility
- summary formatting

## Platform integration

Android/iOS:
- local persistence
- background/foreground
- audio interruption
- timer expiry
- permission denied/revoked
- renderer lifecycle

## Physical overnight matrix

At minimum test:
- screen locked overnight
- app backgrounded
- low battery mode
- audio enabled/disabled
- process killed and relaunched
- time zone / wall clock change
- incoming audio interruption

Record battery delta, thermal state, memory and whether the session was recoverable.

## Release blocker

Losing or duplicating a completed sleep session is a release-blocking defect.
