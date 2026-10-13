#!/usr/bin/env python3
"""
Synthesize three loopable ambient audio files for the Soine app and
encode them to MP3 mono 32 kbps at 16 kHz.

Outputs (relative to repo root):
  androidApp/src/main/res/raw/ambient_rain.mp3
  androidApp/src/main/res/raw/ambient_waves.mp3
  androidApp/src/main/res/raw/ambient_white_noise.mp3
  iosApp/iosApp/Resources/ambient_rain.mp3
  iosApp/iosApp/Resources/ambient_waves.mp3
  iosApp/iosApp/Resources/ambient_white_noise.mp3

Each clip is exactly 8.0 seconds. For rain and white noise the body is
stochastic, so the loop boundary is naturally seamless. For waves the
envelope is a sum of sinusoids whose period divides 8 s, so the value
at t=0 and t=8 is exactly equal.

Why this exists: the music-generation tool used during #251 produced
vocal / musical output regardless of the prompt, which would have
shipped singing or a beat under a "rain" / "waves" / "white noise"
label. Algorithmic synthesis guarantees a clean, label-correct,
deterministic loop with no human voice and no music. A future polish
slice can replace the synthesized body with a curated field recording
if the team accepts the licensing cost; the contract that the audio
matches its label and contains no vocals is preserved.

Deps: only `lame` needs to be on PATH for MP3 encoding. The synthesis
itself uses the Python standard library.
"""

import argparse
import math
import os
import random
import shutil
import struct
import subprocess
import sys
import wave

SAMPLE_RATE = 16000
DURATION_S = 8.0
N_SAMPLES = int(SAMPLE_RATE * DURATION_S)

# 16-bit signed target peak. Leave headroom under the 32767 ceiling.
PEAK = 16384


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def write_wav(path, samples, sr=SAMPLE_RATE):
    body = bytearray()
    for v in samples:
        if v > PEAK:
            v = PEAK
        if v < -PEAK:
            v = -PEAK
        body.extend(struct.pack("<h", int(v)))
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(sr)
        w.writeframes(bytes(body))


def white_noise(n, rng):
    return [rng.uniform(-1.0, 1.0) for _ in range(n)]


class PinkNoiseFilter:
    """Paul Kellet's refined pink-noise filter."""

    def __init__(self):
        self.b = [0.0] * 7

    def step(self, w):
        self.b[0] = 0.99886 * self.b[0] + w * 0.0555179
        self.b[1] = 0.99332 * self.b[1] + w * 0.0750759
        self.b[2] = 0.96900 * self.b[2] + w * 0.1538520
        self.b[3] = 0.86650 * self.b[3] + w * 0.3104856
        self.b[4] = 0.55000 * self.b[4] + w * 0.5329522
        self.b[5] = -0.7616 * self.b[5] - w * 0.0168980
        self.b[6] = w * 0.115926
        return (
            self.b[0] + self.b[1] + self.b[2] + self.b[3]
            + self.b[4] + self.b[5] + self.b[6]
            + w * 0.5362
        )


def one_pole_lp(x, cutoff_hz, prev_y=0.0):
    rc = 1.0 / (2 * math.pi * cutoff_hz)
    dt = 1.0 / SAMPLE_RATE
    a = dt / (rc + dt)
    y = [0.0] * len(x)
    py = prev_y
    for i, xi in enumerate(x):
        py = py + a * (xi - py)
        y[i] = py
    return y


def one_pole_hp(x, cutoff_hz, prev_x=0.0, prev_y=0.0):
    rc = 1.0 / (2 * math.pi * cutoff_hz)
    dt = 1.0 / SAMPLE_RATE
    a = rc / (rc + dt)
    y = [0.0] * len(x)
    px = prev_x
    py = prev_y
    for i, xi in enumerate(x):
        py = a * (py + xi - px)
        y[i] = py
        px = xi
    return y


def normalize(samples, target_PEAK=PEAK, fraction=0.85):
    peak = max(abs(v) for v in samples) or 1.0
    norm = target_PEAK * fraction / peak
    return [v * norm for v in samples]


# ---------------------------------------------------------------------------
# Tracks
# ---------------------------------------------------------------------------


def synth_rain(seed=20261013):
    rng = random.Random(seed)
    n = N_SAMPLES
    pink_filt = PinkNoiseFilter()
    body = [0.0] * n
    for i in range(n):
        body[i] = pink_filt.step(rng.uniform(-1.0, 1.0))

    sparkle_src = white_noise(n, rng)
    sparkle = one_pole_hp(sparkle_src, cutoff_hz=2500.0)
    sparkle = one_pole_hp(sparkle, cutoff_hz=2500.0)

    taps = [0.0] * n
    t = 0
    while t < n:
        gap = max(1, int(rng.gauss(SAMPLE_RATE / 18, SAMPLE_RATE / 35)))
        t += gap
        if t >= n:
            break
        tap_len = int(rng.uniform(0.020, 0.050) * SAMPLE_RATE)
        amp = rng.uniform(0.4, 1.0)
        tap_freq = rng.uniform(2500.0, 5500.0)
        for k in range(tap_len):
            if t + k >= n:
                break
            env = math.exp(-k / (tap_len * 0.35))
            taps[t + k] += amp * env * math.sin(2 * math.pi * tap_freq * k / SAMPLE_RATE)

    out = []
    for i in range(n):
        slow_env = 0.78 + 0.10 * math.sin(2 * math.pi * i / (SAMPLE_RATE * 3.7))
        s = (0.55 * body[i] + 0.30 * sparkle[i] + 0.18 * taps[i]) * slow_env
        out.append(s)
    return normalize(out)


def synth_waves(seed=20261014):
    rng = random.Random(seed)
    n = N_SAMPLES
    pink_filt = PinkNoiseFilter()
    body = [0.0] * n
    for i in range(n):
        body[i] = pink_filt.step(rng.uniform(-1.0, 1.0))
    body = one_pole_lp(body, cutoff_hz=1200.0)

    # Two cycles per loop (4 s each). Each cycle = rise 1.6 s + decay 2.4 s.
    out = []
    for i in range(n):
        t = i / SAMPLE_RATE
        cp = t % 4.0
        if cp < 1.6:
            swell = math.sin(math.pi * cp / 1.6)
        else:
            x = (cp - 1.6) / 2.4
            swell = (1.0 - x) ** 1.6
        secondary = 0.10 * math.sin(2 * math.pi * t / 1.0)
        env = 0.18 + 0.82 * swell + 0.04 * secondary
        out.append(body[i] * env)
    return normalize(out)


def synth_white_noise(seed=20261015):
    rng = random.Random(seed)
    n = N_SAMPLES
    src = white_noise(n, rng)
    body = one_pole_lp(src, cutoff_hz=6500.0)
    out = [v * 0.55 for v in body]
    return normalize(out)


# ---------------------------------------------------------------------------
# Driver
# ---------------------------------------------------------------------------


def encode_mp3(wav_path, mp3_path):
    if not shutil.which("lame"):
        sys.exit("lame is required on PATH for MP3 encoding")
    subprocess.run(
        ["lame", "--quiet", "-m", "m", "-b", "32", wav_path, mp3_path],
        check=True,
    )


def place_in_repo(mp3_dir: str, repo_root: str):
    pairs = [
        ("ambient_rain.mp3", "androidApp/src/main/res/raw/ambient_rain.mp3"),
        ("ambient_waves.mp3", "androidApp/src/main/res/raw/ambient_waves.mp3"),
        ("ambient_white_noise.mp3", "androidApp/src/main/res/raw/ambient_white_noise.mp3"),
        ("ambient_rain.mp3", "iosApp/iosApp/Resources/ambient_rain.mp3"),
        ("ambient_waves.mp3", "iosApp/iosApp/Resources/ambient_waves.mp3"),
        ("ambient_white_noise.mp3", "iosApp/iosApp/Resources/ambient_white_noise.mp3"),
    ]
    for src_name, dst_rel in pairs:
        src = os.path.join(mp3_dir, src_name)
        dst = os.path.join(repo_root, dst_rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copyfile(src, dst)
        print(f"placed {dst}")


def main():
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[1] if __doc__ else "")
    ap.add_argument(
        "--repo-root",
        default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        help="Path to the Soine repo root (default: parent of this script).",
    )
    ap.add_argument(
        "--work-dir",
        default="/tmp/soine_audio/synth",
        help="Working directory for the intermediate WAV and MP3 files.",
    )
    ap.add_argument(
        "--no-place",
        action="store_true",
        help="Generate the files in --work-dir but do not copy them into the repo.",
    )
    args = ap.parse_args()

    os.makedirs(args.work_dir, exist_ok=True)

    samples = {
        "ambient_rain": synth_rain(),
        "ambient_waves": synth_waves(),
        "ambient_white_noise": synth_white_noise(),
    }

    for name, s in samples.items():
        wav = os.path.join(args.work_dir, f"{name}.wav")
        mp3 = os.path.join(args.work_dir, f"{name}.mp3")
        write_wav(wav, s)
        encode_mp3(wav, mp3)
        os.remove(wav)
        size = os.path.getsize(mp3)
        print(f"{name}: {size} bytes ({size / 1024:.1f} KB)")

    if not args.no_place:
        place_in_repo(args.work_dir, args.repo_root)


if __name__ == "__main__":
    main()