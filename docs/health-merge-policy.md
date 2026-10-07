# Manual session + Health sleep merge policy

Soine keeps two concepts deliberately separate:

1. **一緒に寝た時間** — the canonical manual Soine session.
2. **推定睡眠** — optional Health Connect / Apple Health enrichment.

Health data never rewrites the manual session's start, end, duration, source, or
relationship/progression input.

## Source precedence

The manual `SleepSessionRecord` is always authoritative for the Soine
experience.

Health providers are not silently blended with each other. If signals from
multiple providers are supplied, Soine returns one independent estimate per
provider.

UI labels:

- manual: `一緒に寝た時間`
- Health Connect: `推定睡眠（Health Connect）`
- HealthKit: `推定睡眠（Apple Health）`

## Session boundary

Only the portion of a Health signal overlapping the completed manual session is
considered. Health timestamps outside the manual session are clipped to the
manual interval.

This prevents external records from expanding the period that counts as
sleeping together.

## Overlap and conflict precedence

Within one provider, the interval is split at all signal boundaries. For each
resulting segment the state precedence is:

`AWAKE > ASLEEP > IN_BED > UNKNOWN`

Consequences:

- explicit awake time is never counted as asleep;
- detailed asleep stages may overlap `IN_BED` without double counting;
- `IN_BED` alone is not considered proof of sleep;
- unknown signal types do not become sleep implicitly.

Adjacent/overlapping asleep intervals therefore contribute only their resolved
wall-clock duration, not the sum of raw record durations.

## Estimated fields

A provider estimate contains:

- estimated sleep start: first resolved `ASLEEP` segment start
- estimated wake: last resolved `ASLEEP` segment end
- resolved asleep duration
- resolved awake duration
- provider
- optional confidence

If a provider has no resolved asleep segment, no sleep estimate is emitted.

## Confidence

Soine does not manufacture confidence.

If every contributing `ASLEEP` signal has a confidence value, the estimate
uses the minimum contributing confidence as a conservative aggregate. If any
contributing signal has unknown confidence, the combined confidence remains
unknown.

## No silent overwrite

`HealthEnrichedSleepSummary` wraps the canonical `SleepSummary` instead of
mutating its manual fields. Tests explicitly assert that manual duration,
bedtime, wake time, and source are unchanged after Health enrichment.

Relationship progression, Dream probability, and the core sleep lifecycle must
continue to use the manual session unless a future product decision explicitly
changes that rule.
