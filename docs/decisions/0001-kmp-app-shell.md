# ADR 0001: KMP app shell with platform 3D adapters

Status: Accepted

## Decision

Use Kotlin Multiplatform for shared domain logic and Compose Multiplatform for the initial shared application UI.

Keep 3D rendering behind a platform boundary rather than making Unity the application shell.

## Why

The product is primarily a long-running mobile sleep utility. Session recovery, audio, permissions, health APIs, battery use, and OS lifecycle behavior are more important than game-engine features.

The character still remains a first-class product surface, but commonMain communicates with it through semantic companion states.

## Consequences

- business logic is testable in commonMain
- Android/iOS native APIs remain available
- 3D implementation can differ by platform
- renderer choice can be changed after a physical-device PoC
- some platform-specific code is expected and intentional

## Revisit when

Reconsider a game engine if interaction expands to continuous free movement, cloth/bed physics, complex procedural animation, or a large interactive scene.
