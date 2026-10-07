# Product Analytics Plan

Analytics is for validating the experience, not measuring sensitive sleep content.

## Core funnel

- bedtime_screen_viewed
- sleep_session_started
- session_recovered
- sleep_session_completed
- morning_summary_viewed
- night_memory_opened
- dream_discovered

## Product metrics

Primary:
- percentage of started sessions completed
- D1/D7/D30 return-to-bedtime
- morning summary open rate
- nights completed per active user

Experience:
- night-memory engagement
- dream collection engagement
- ambient sound usage
- companion interaction before sleep

Reliability:
- recovered-session rate
- renderer fallback rate
- audio failure/interruption rate

## Privacy

Do not send raw audio, detailed Health samples, user-written private text, or exact sleep timelines to analytics by default.


## Implementation boundary

`commonMain` owns the vendor-neutral `AnalyticsTracker` contract and the
fixed `AnalyticsEvent` vocabulary. Vendor SDK adapters belong in platform
source sets or a future integration layer.

The initial contract deliberately exposes no arbitrary event payload. This
prevents accidental analytics collection of exact sleep timelines, Health
samples, raw audio, user-written text, or other sensitive content.

`NoOpAnalyticsTracker` is the default-safe implementation until an explicit
analytics provider is selected.
