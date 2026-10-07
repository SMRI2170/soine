# Android overnight sound detection spike

Issue: #55

This spike evaluates whether Soine can derive coarse overnight sound events on
Android without retaining raw audio and without making the core sleep lifecycle
depend on microphone analysis.

## Lifecycle

1. The user opts in to `夜間の音解析` from Settings.
2. Microphone permission must already be granted.
3. After the manual sleep session has been persisted successfully, Soine starts
   a foreground service with the Android `microphone` service type.
4. The service reads mono PCM16 audio into a reusable one-second in-memory
   buffer.
5. The prototype classifier emits only derived `SoundEvent` values.
6. At wake, capture stops and derived events are persisted by sleep-session id
   before the manual session is finalized.
7. If capture, classification, or sound-event persistence fails, the manual
   sleep session remains valid.

The service is started only from a user-visible flow. Android 14+ enforces
while-in-use microphone permission when creating a microphone foreground
service, so Soine does not attempt to silently start overnight capture from the
background after process death.

## Foreground-service requirements

The Android manifest declares:

- `RECORD_AUDIO`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_MICROPHONE`
- `android:foregroundServiceType="microphone"`

The foreground notification states that Soine is analyzing overnight sound on
device.

## Prototype signal pipeline

The spike uses 8 kHz mono PCM16 and classifies one-second frames.

Features:

- RMS energy
- peak amplitude
- zero-crossing rate
- coarse autocorrelation over roughly 50–300 Hz

Initial derived categories:

- `LOUD_SOUND`
- `SNORE_LIKE`
- `VOCALIZATION`

The thresholds are deterministic prototype heuristics, not a medical sleep or
snoring diagnosis. They are intended to make device feasibility measurable
before selecting or training a production model.

To reduce duplicate events and storage growth, identical event categories have
a 30-second cooldown and one sleep session is capped at 256 derived events.

## Privacy boundary

Raw PCM is never written to a file, preferences, database, repository, or the
common domain. A single `ShortArray` is reused in memory and overwritten by
the next capture frame.

Only these fields cross into persistence:

- sleep-session id
- derived event type
- timestamp
- confidence
- on-device source
- detector version

Disabling the feature during an active sleep session stops capture and persists
only the derived events accumulated so far.

## Known spike limitations

- Process death ends microphone capture. Soine intentionally does not
  auto-start a microphone foreground service from a background recovery path.
- The heuristic classifier has not yet been calibrated against a labeled
  overnight dataset.
- Ambient sound played by Soine may leak into the microphone path and must be
  measured as a false-positive condition.
- Battery and thermal cost cannot be established from CI or a simulator.

These limitations prevent #55 from being closed based on code/CI alone.

## Physical-device test matrix

Record device model, Android version, charging state, start/end battery,
maximum thermal status, and test duration for every run.

| Scenario | Minimum observation |
| --- | --- |
| silence / quiet room | false positives per hour |
| snore-like playback | detected / expected events |
| speech-like playback | detected / expected events |
| loud transient playback | detected / expected events |
| Soine ambient audio on | false positives per hour by category |
| screen off overnight | service survival and event count |
| interruption / app background | service behavior and recovery |
| 6–8 hour run | battery delta and thermal status |

Do not check off battery, thermal, or false-positive acceptance criteria until
real-device measurements are attached to #55.
