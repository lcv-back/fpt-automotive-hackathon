#!/usr/bin/env python3
"""Build the three-noise-level benchmark corpus required by V12.

`vong2/24-N5-TRANG-THAI-INTEGRATION.md` lists the quiet/cabin/highway levels as
"chua tao" and states the condition for ever quoting a number from them:

    "Khi tao phai ghi: nguon nhieu, muc SNR, cach tron"

So that is exactly what this script emits — a WAV per (clip x level) plus a
manifest naming the noise source, the SNR and the mixing rule for each one.

    python scripts/noise_mix.py --source ../android/voice/src/main/res/raw \
                                --out-dir ../evidence/asr/corpus

⚠️ The noise here is **synthesised, not recorded in a cabin.** Nobody on the
team has a cabin recording, and inventing a provenance for one would be worse
than saying so. What the numbers downstream can support is a *relative* claim —
"recognition degrades this much between SNR 20 dB and SNR 5 dB" — not an
absolute claim about how VIVA behaves on a real highway. The manifest repeats
this so the limitation travels with the audio instead of living only here.

Determinism: every clip/level pair seeds its own PRNG from the pair's name, so
a rebuild produces byte-identical audio and a reviewer can regenerate the exact
files the CSV was measured on. No wall-clock, no global random state.

Stdlib only, matching bench_tts_samples.py — this runs next to the container
without a venv.
"""

from __future__ import annotations

import argparse
import array
import csv
import math
import random
import sys
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bench_tts_samples import TARGET_RATE, read_wav_mono16, resample_linear  # noqa: E402


# --------------------------------------------------------------- noise shapes

def _pink(n: int, rng: random.Random) -> list[float]:
    """Paul Kellet's economy pink filter: white noise shaped to roughly -3 dB/octave.

    Pink rather than white because cabin noise has far more energy low down than
    up high; white noise would under-weight exactly the band that masks Vietnamese
    vowels and over-weight the band an ASR front-end mostly ignores.
    """
    b0 = b1 = b2 = 0.0
    out = []
    for _ in range(n):
        w = rng.uniform(-1.0, 1.0)
        b0 = 0.99765 * b0 + w * 0.0990460
        b1 = 0.96300 * b1 + w * 0.2965164
        b2 = 0.57000 * b2 + w * 1.0526913
        out.append(b0 + b1 + b2 + w * 0.1848)
    return out


def _lowpass(signal: list[float], rate: int, cutoff_hz: float) -> list[float]:
    """One-pole lowpass. Crude on purpose — a steeper filter would imply a
    precision about the noise spectrum that a synthetic source does not have."""
    a = math.exp(-2.0 * math.pi * cutoff_hz / rate)
    y = 0.0
    out = []
    for x in signal:
        y = (1.0 - a) * x + a * y
        out.append(y)
    return out


def _rms(signal) -> float:
    if not len(signal):
        return 0.0
    return math.sqrt(sum(float(s) * float(s) for s in signal) / len(signal))


def _scale(signal: list[float], factor: float) -> list[float]:
    return [s * factor for s in signal]


def _mix_shapes(*parts: list[float]) -> list[float]:
    return [sum(values) for values in zip(*parts)]


def profile_hvac_idle(n: int, rate: int, rng: random.Random) -> list[float]:
    """Stationary car, blower running: broadband hiss, mild low-frequency tilt."""
    return _lowpass(_pink(n, rng), rate, 3000.0)


def profile_city_road(n: int, rate: int, rng: random.Random) -> list[float]:
    """Moderate speed on city streets: tyre/road rumble plus some cabin hiss."""
    pink = _pink(n, rng)
    rumble = _scale(_lowpass(pink, rate, 700.0), 1.0)
    hiss = _scale(_lowpass(_pink(n, rng), rate, 4000.0), 0.35)
    return _mix_shapes(rumble, hiss)


def profile_highway_rumble(n: int, rate: int, rng: random.Random) -> list[float]:
    """Sustained high speed: dominant low-frequency rumble plus wind noise."""
    pink = _pink(n, rng)
    rumble = _scale(_lowpass(pink, rate, 250.0), 1.6)
    wind = _scale(_lowpass(_pink(n, rng), rate, 1800.0), 0.5)
    return _mix_shapes(rumble, wind)


# Team-chosen SNR values. These are engineering estimates of idle / city /
# highway cabins, NOT measurements — no one on the team has metered a cabin.
# They are stated here so a reader can disagree with the number instead of
# having to reverse-engineer it out of the audio.
LEVELS = {
    "quiet": {
        "snr_db": 20.0,
        "profile": profile_hvac_idle,
        "source": "pink noise, one-pole lowpass 3000 Hz (HVAC blower hiss)",
        "meaning": "xe dung yen, dieu hoa dang chay",
    },
    "cabin": {
        "snr_db": 10.0,
        "profile": profile_city_road,
        "source": "pink noise, lowpass 700 Hz (rumble) + 0.35x lowpass 4000 Hz (hiss)",
        "meaning": "chay trong pho, toc do vua",
    },
    "highway": {
        "snr_db": 5.0,
        "profile": profile_highway_rumble,
        "source": "pink noise, 1.6x lowpass 250 Hz (rumble) + 0.5x lowpass 1800 Hz (gio)",
        "meaning": "chay duong truong, toc do cao",
    },
}


def mix_at_snr(
    speech: array.array,
    level: str,
    rate: int,
    rng: random.Random,
) -> tuple[array.array, dict]:
    """Add `level`'s noise to `speech` at that level's SNR.

    SNR is defined over the **whole clip's** RMS, speech and noise alike. The
    alternative — measuring only voice-active frames — would report a higher
    SNR for the same audio, and picking the flattering definition without
    saying so is how benchmark numbers stop meaning anything. TTS clips carry
    very little leading silence, so the two definitions are close here anyway.
    """
    spec = LEVELS[level]
    n = len(speech)
    speech_rms = _rms(speech)
    noise = spec["profile"](n, rate, rng)
    noise_rms = _rms(noise)
    if noise_rms == 0.0 or speech_rms == 0.0:
        raise ValueError(f"silent signal, cannot set SNR (speech_rms={speech_rms})")

    target_noise_rms = speech_rms / (10.0 ** (spec["snr_db"] / 20.0))
    gain = target_noise_rms / noise_rms

    mixed = array.array("h")
    clipped = 0
    for sample, noise_sample in zip(speech, noise):
        value = sample + noise_sample * gain
        if value > 32767 or value < -32768:
            clipped += 1
        mixed.append(int(max(-32768, min(32767, round(value)))))

    achieved = 20.0 * math.log10(speech_rms / (noise_rms * gain))
    return mixed, {
        "speech_rms": round(speech_rms, 2),
        "noise_rms": round(noise_rms * gain, 2),
        "snr_db_target": spec["snr_db"],
        "snr_db_achieved": round(achieved, 3),
        "clipped_samples": clipped,
        "samples": n,
    }


def write_wav(path: Path, samples: array.array, rate: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    with wave.open(str(path), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(rate)
        if sys.byteorder == "big":
            samples = array.array("h", samples)
            samples.byteswap()
        wav.writeframes(samples.tobytes())


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source", default="../android/voice/src/main/res/raw",
                        help="thu muc chua WAV goc (mono 16-bit)")
    parser.add_argument("--pattern", default="*.wav")
    parser.add_argument("--out-dir", default="../evidence/asr/corpus")
    parser.add_argument("--levels", default="quiet,cabin,highway")
    args = parser.parse_args()

    source = Path(args.source)
    out_dir = Path(args.out_dir)
    levels = [lv.strip() for lv in args.levels.split(",") if lv.strip()]
    for level in levels:
        if level not in LEVELS:
            print(f"Muc nhieu khong biet: {level}. Co: {', '.join(LEVELS)}", file=sys.stderr)
            return 1

    clips = sorted(source.glob(args.pattern))
    if not clips:
        print(f"Khong tim thay WAV nao trong {source}", file=sys.stderr)
        return 1

    rows = []
    for path in clips:
        samples, rate = read_wav_mono16(path)
        resampled = resample_linear(samples, rate, TARGET_RATE)

        # The clean 16 kHz copy is part of the corpus: every noisy variant is
        # this exact signal plus noise, so a reviewer can diff them.
        clean_path = out_dir / "clean" / path.name
        write_wav(clean_path, resampled, TARGET_RATE)
        rows.append({
            "clip": path.stem, "level": "clean", "path": str(clean_path.as_posix()),
            "snr_db_target": "", "snr_db_achieved": "", "clipped_samples": 0,
            "samples": len(resampled), "source_rate": rate,
        })

        for level in levels:
            rng = random.Random(f"viva-v12|{path.stem}|{level}")
            mixed, stats = mix_at_snr(resampled, level, TARGET_RATE, rng)
            out_path = out_dir / level / path.name
            write_wav(out_path, mixed, TARGET_RATE)
            rows.append({
                "clip": path.stem, "level": level, "path": str(out_path.as_posix()),
                "snr_db_target": stats["snr_db_target"],
                "snr_db_achieved": stats["snr_db_achieved"],
                "clipped_samples": stats["clipped_samples"],
                "samples": stats["samples"], "source_rate": rate,
            })
        print(f"  {path.stem:<34} -> clean + {' + '.join(levels)}")

    index = out_dir / "corpus-index.csv"
    with index.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    total_clipped = sum(r["clipped_samples"] for r in rows)
    print()
    print(f"clip goc      : {len(clips)}")
    print(f"file da ghi   : {len(rows)}  ({len(levels)} muc nhieu + ban clean)")
    print(f"mau bi clip   : {total_clipped}  (0 la tot; >0 nghia la nhieu day tin hieu vuot tran)")
    print(f"index         -> {index}")
    print()
    print("NGUON NHIEU LA TONG HOP, KHONG PHAI THU TRONG CABIN — moi so do tren")
    print("corpus nay chi noi len muc SUY GIAM giua cac muc SNR, khong noi len")
    print("do chinh xac cua VIVA tren duong that.")
    for level in levels:
        spec = LEVELS[level]
        print(f"  {level:<8} SNR {spec['snr_db']:>4.1f} dB  ({spec['meaning']}) — {spec['source']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
