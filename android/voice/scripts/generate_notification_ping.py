"""Generate the final non-speech TTS fallback cue as mono PCM16 WAV."""

from __future__ import annotations

import math
import struct
import wave
from pathlib import Path


def main() -> None:
    module_root = Path(__file__).resolve().parents[1]
    output = (module_root / "src/main/res/raw/viva_notification_ping.wav").resolve()
    if module_root not in output.parents:
        raise RuntimeError("Output escaped the voice module")
    output.parent.mkdir(parents=True, exist_ok=True)

    sample_rate = 22_050
    duration_seconds = 0.18
    sample_count = int(sample_rate * duration_seconds)
    frames = bytearray()
    for index in range(sample_count):
        t = index / sample_rate
        envelope = math.sin(math.pi * index / sample_count) ** 2
        signal = 0.16 * envelope * (
            math.sin(2 * math.pi * 880 * t) + 0.35 * math.sin(2 * math.pi * 1_320 * t)
        )
        frames.extend(struct.pack("<h", int(max(-1.0, min(1.0, signal)) * 32_767)))

    with wave.open(str(output), "wb") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(sample_rate)
        wav.writeframes(frames)
    print(f"Generated {output} ({sample_count} samples)")


if __name__ == "__main__":
    main()
