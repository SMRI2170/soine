# ADR 0002: Local-first MVP

Status: Accepted

## Decision

Do not require authentication or a backend for the MVP. Persist sleep sessions, settings, summaries, and companion progression locally.

## Why

Bedtime must work offline and with minimal friction. It also reduces privacy risk while sleep inference is still experimental.

## Consequences

Cloud sync and account recovery are deferred. Storage contracts must allow a future sync layer without changing domain models.
