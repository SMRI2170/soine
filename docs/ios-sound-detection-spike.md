# iOS overnight sound detection spike

Issue: #56

## Scope

This spike adds an on-device microphone path for optional overnight sound-event
analysis without persisting raw audio.

- AVAudioEngine input tap
- AVAudioSession PlayAndRecord so ambient playback and microphone input can coexist
- UIBackgroundModes already includes audio
- derived SoundEvent values only
- 30-second per-category cooldown
- maximum 256 derived events per sleep session
- interruption pause/resume handling

## Privacy boundary

Raw PCM exists only in the AVAudioPCMBuffer callback and a temporary in-memory
ShortArray used by PrototypeSoundFrameClassifier.

No AVAudioFile is created. No audio path or PCM payload enters common persistence.

## Physical-device verification still required

The software spike does not prove overnight feasibility. Before #56 can close,
record real-device results for:

- lock-screen/background capture
- snore-like / vocalization / loud sound behavior
- phone-call / Siri / other audio interruption recovery
- false positives, including Soine ambient sound leakage
- battery drain
- thermal behavior
- route changes (speaker / headphones / Bluetooth)

Use device, iOS version, build SHA, duration, event counts, battery delta and
observed thermal state in the issue notes.
