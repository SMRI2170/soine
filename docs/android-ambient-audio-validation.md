# Android ambient audio validation

The Android implementation uses a foreground media-playback service. The service owns
the MediaPlayer, audio focus and the absolute sleep-timer deadline so playback does not
depend on the Activity staying visible.

## Physical device checklist

Record the device, Android version and build SHA in issue #22.

1. Enable ambient audio and start a sleep session.
2. Lock the screen for at least 10 minutes. Confirm audio continues and the foreground notification remains.
3. Set a short custom timer. Lock the screen and confirm audio stops at the deadline while the sleep session remains active.
4. Start another media app or a call. Confirm Soine pauses on transient focus loss and only resumes after focus gain.
5. Swipe Soine away from Recents while audio is playing. Confirm the foreground service behavior is acceptable for the target device/OS.
6. Run at least 60 minutes with the screen off and record battery delta, thermal behavior and any playback interruption.

Do not close #22 until these physical checks are recorded.
