# Overnight battery benchmark

Use this template to compare Soine releases under repeatable overnight conditions. Record raw observations; do not infer medical or sleep-quality conclusions from battery behavior.

## Fixed test conditions

- Start battery: target 80-100%; record the exact value.
- Power: unplugged, Low Power/Battery Saver off unless the scenario explicitly tests it.
- Radios: record Wi-Fi, cellular, Bluetooth and wearable connection state.
- Display: use the scenario-defined state; otherwise keep brightness and auto-lock unchanged between runs.
- Environment: record approximate room temperature and whether the device was charging before the run.
- Build: record git SHA, build type and whether a debugger/profiler was attached.
- Duration: target at least 6 hours for overnight comparison; shorter exploratory runs must be labeled as such.
- Repeatability: run each release/scenario at least twice on the same device before treating a difference as meaningful.

## Scenarios

### A. Session only

- Start a sleep session.
- Ambient audio off.
- Optional Health/microphone enrichment off.
- Leave the screen off for the measured period.
- End the session and confirm recovery/summary still works.

### B. Ambient audio enabled

- Start a sleep session with the same bundled ambient sound and fixed volume for every comparison.
- Use no timer unless the test specifically targets timer behavior.
- Lock the screen and confirm the foreground/background playback mechanism remains active.
- End the session and confirm audio resources are released.

### C. 3D visible

Run only after the 3D renderer exists.

- Keep the display on for a fixed duration, preferably 60 minutes because a full overnight screen-on run is not representative.
- Fix brightness, companion animation/state and scene complexity.
- Record average battery delta per hour, thermal state and visible frame-rate degradation.

### D. Screen off

- Start a normal sleep session.
- Lock the screen for the full test window.
- Record whether session persistence, audio timer expiration and wake flow still behave correctly.
- Note unexpected process death, playback interruption or delayed timer behavior.

## Result template

| Field | Value |
| --- | --- |
| Date | |
| Device | |
| OS version | |
| Build SHA | |
| Build type | debug / release |
| Scenario | A / B / C / D |
| Start time | |
| End time | |
| Duration | |
| Battery start | % |
| Battery end | % |
| Battery delta | percentage points |
| Delta per hour | percentage points/hour |
| Screen state | on / off |
| Brightness | |
| Ambient sound | off / rain / waves / white noise |
| Volume | |
| Timer | off / duration |
| Wi-Fi | on / off |
| Cellular | on / off |
| Bluetooth | on / off |
| Wearable connected | yes / no |
| Thermal observation | nominal / warm / hot / OS warning |
| Process death | none / observed |
| Playback interruption | none / observed |
| Timer failure | none / observed |
| Session loss/duplication | none / observed |
| Notes | |

## Comparison rule

Compare only runs on the same device and broadly equivalent OS/environment settings. Calculate battery delta per hour as:

battery delta per hour = (battery start - battery end) / duration hours

Flag a regression for investigation when the same scenario shows either:

- more than 2 percentage points/hour additional drain versus the current release baseline, or
- any new process death, lost/duplicate session, timer failure, thermal warning, or persistent playback failure.

The numeric threshold is an engineering alert, not a user-facing battery claim. Re-run once before filing a regression if the only difference is battery drain.

## Release record

For each release candidate, add one table per device to the relevant issue or release notes and link back to this document. Keep the benchmark procedure unchanged unless this file is versioned in the same PR that changes the method.
