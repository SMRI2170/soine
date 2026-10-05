# iOS ambient audio validation

Soine uses AVAudioSessionCategoryPlayback and enables the audio background mode in the iOS app plist. The Kotlin controller observes AVAudioSessionInterruptionNotification and resumes only when iOS supplies the should-resume option.

## Xcode integration

Use iosApp/iosApp/Info.plist as the app target plist and include the three WAV files under iosApp/iosApp/Resources in Copy Bundle Resources.

## Physical device checklist

Record device, iOS version and build SHA in issue #23.

1. Enable ambient audio and start a sleep session.
2. Lock the device for at least 10 minutes and confirm playback continues.
3. Set a short sleep timer, lock the device and confirm audio stops while the sleep session remains active.
4. Trigger a phone, FaceTime, or other audio interruption and confirm playback pauses.
5. End the interruption and confirm Soine resumes only when iOS indicates playback should resume.
6. Background and foreground the app several times and confirm UI playback state remains consistent.
7. Run at least 60 minutes screen-off and record battery delta, thermal notes and playback interruptions.

Do not close #23 until target bundling and physical-device checks are recorded.
