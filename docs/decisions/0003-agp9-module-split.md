# ADR 0003: Split Android application from the KMP shared module

## Status
Accepted

## Context

The original `composeApp` module combined:
- `org.jetbrains.kotlin.multiplatform`
- `com.android.application`
- the Android launcher activity
- shared Compose/domain code

AGP 9 no longer supports combining the Kotlin Multiplatform Gradle plugin with the Android application/library plugins in the same module.

## Decision

Use:
- `composeApp`: Kotlin Multiplatform shared module using `com.android.kotlin.multiplatform.library`
- `androidApp`: standalone Android application using `com.android.application`
- `iosApp`: native iOS application consuming the KMP framework

Version baseline:
- Kotlin 2.4.20
- Compose Multiplatform 1.12.1
- AGP 9.3.1
- Gradle 9.5.0

The AGP choice stays inside Kotlin 2.4.20's documented compatibility range rather than following the newest AGP independently.

## Consequences

Positive:
- removes the deprecated KMP/Android application plugin combination
- keeps platform entry points outside shared code
- aligns the project with the supported Android-KMP plugin
- prepares CI for AGP 9

Trade-offs:
- one additional Gradle module
- Android-only dependencies belong in `androidApp` unless they are implementation details of the Android KMP target
- run configurations must target `androidApp`

## Follow-up

The Gradle wrapper properties are pinned to Gradle 9.3.1. The generated wrapper scripts/JAR must be committed before CI is considered complete.
