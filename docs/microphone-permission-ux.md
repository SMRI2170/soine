# Microphone permission UX

Night sound analysis is optional and opt-in. The core sleep flow never depends
on microphone permission.

## Enable flow

1. The feature is off by default.
2. The user explicitly turns on "夜間の音解析" in Settings.
3. Soine shows a pre-permission explanation before invoking the OS prompt.
4. Only after the user accepts that explanation does Soine request microphone
   permission.
5. The feature becomes enabled only when OS permission is granted.

The copy states that processing is for derived night sound events, raw audio is
not stored by default, and sleep recording remains usable without permission.

## Permission states

- `NOT_REQUESTED`: show pre-permission explanation.
- `GRANTED`: the user may enable or disable the feature independently.
- `DENIED`: keep the feature off and allow an explicit retry when the OS can
  still present the permission prompt.
- `PERMANENTLY_DENIED`: direct the user to the app's system settings.
- `UNAVAILABLE`: keep the feature disabled.

On iOS, a previously denied microphone permission requires Settings and is
therefore normalized as `PERMANENTLY_DENIED`.

## Revocation

Android refreshes permission state whenever the activity resumes. iOS observes
the app becoming active. If permission is revoked while sound analysis is
enabled, the persisted feature toggle is automatically turned off.

## Storage and privacy

The opt-in toggle is stored locally. Local-data deletion clears the toggle.
The OS permission itself remains owned by Android/iOS.

No raw-audio file path is introduced by this feature.
