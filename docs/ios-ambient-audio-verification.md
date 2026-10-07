# iOS Ambient Audio Physical Verification

This document is the physical-device verification checklist for the
[iOS ambient audio implementation][ios-ctrl]. It is the verification
slice of [#23][issue-23].

The software side was implemented in #103, #107, #109 and #110 and
the iOS compile-check CI step ([`.github/workflows/ios.yml`][ios-ci])
verifies that the source still compiles. The acceptance criteria in
#23 are real-device scenarios that can only be observed on hardware.

## Why this exists

The implementation handles interruption handling, AVAudioSession category
transitions, and PlayAndRecord preservation when the optional
microphone analysis is active. These behaviours depend on iOS
framework callbacks that only fire on a real device. CI can prove
that the code compiles; only a device run can prove that the
behaviour is correct.

## Test conditions

For each scenario:

- device: at least one iPhone class (iPhone 13 / 14 / 15)
- iOS version: latest two major releases
- build type: `Release` configuration with the same `versionName`
  shipped to TestFlight
- audio resource: bundled rain / waves / white-noise loops
- volume: fixed at 0.5 for repeatability unless stated otherwise
- microphone analysis: off, unless the scenario is the PlayAndRecord
  coexistence check

## Scenarios

### A. Lock-screen playback

Goal: confirm that ambient audio continues while the device is
locked.

| Step | Expected |
| --- | --- |
| 1. Start bedtime flow with bundled rain loop. | `AmbientPlaybackStatus.PLAYING`. |
| 2. Lock the device. | Audio keeps playing. |
| 3. Hold for 30 minutes. | Audio still playing. Battery delta < 1 % per 30 minutes. |
| 4. Wake the device. | Playback continues without restart. |

### B. Other-audio interruption (incoming call / Siri)

Goal: confirm interruption handling.

| Step | Expected |
| --- | --- |
| 1. Start ambient playback. | `PLAYING`. |
| 2. Receive an incoming call. | Audio pauses; `AmbientPlaybackStatus.PAUSED`. |
| 3. End the call. | If the system signals `shouldResume`, audio resumes; otherwise stays paused. |
| 4. Trigger Siri. | Audio pauses while Siri is active. |
| 5. Dismiss Siri. | Audio resumes (Apple convention is to resume when `shouldResume` is set). |

### C. Long-duration playback (overnight)

Goal: confirm 6-hour playback stability.

| Step | Expected |
| --- | --- |
| 1. Start ambient playback at 23:00 local. | `PLAYING`. |
| 2. Lock the device. | Plays through the night. |
| 3. Stop the playback manually at 06:00. | State returns to `STOPPED`. |
| 4. Record battery delta, thermal state, and any console warnings. | Battery delta < 2 % per hour; thermal nominal. |

### D. PlayAndRecord coexistence

Goal: confirm that ambient playback does not tear down microphone
input when the optional overnight sound analysis is enabled.

| Step | Expected |
| --- | --- |
| 1. Grant microphone permission. | `MicrophonePermissionState.GRANTED`. |
| 2. Enable overnight sound analysis in Settings. | `SoundAnalysisPreferences.enabled == true`. |
| 3. Start a sleep session. | `OvernightSoundAnalysisService` runs. |
| 4. Start ambient playback. | `AVAudioSession.category` stays `PlayAndRecord`. |
| 5. Stop ambient playback. | `AVAudioSession.category` remains `PlayAndRecord` while analysis is active. |

### E. Lifecycle restoration

Goal: confirm that a relaunched app preserves audio state.

| Step | Expected |
| --- | --- |
| 1. Start ambient playback. | `PLAYING`. |
| 2. Force-quit. | Process killed. |
| 3. Relaunch. | If recovery returns `Sleeping`, the bedtime flow resumes; ambient audio preference re-attaches but does not auto-start until the user action. |

## Result template

| Field | Value |
| --- | --- |
| Date | |
| Device | |
| iOS version | |
| Build SHA | |
| Build type | debug / release |
| Scenario | A / B / C / D / E |
| Result | pass / partial / fail |
| Notes | |

## Recording the result

Append one row per scenario per device per release candidate to the
release record file. Do not retroactively edit earlier entries.

[issue-23]: https://github.com/SMRI2170/soine/issues/23
[ios-ctrl]: ../composeApp/src/iosMain/kotlin/app/soine/audio/IosAmbientAudioController.kt
[ios-ci]: ../.github/workflows/ios.yml