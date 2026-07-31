"""Reproducible synthetic baseline for L3c; not a substitute for cabin audio."""

from __future__ import annotations

import argparse
import csv
import math
import wave
from dataclasses import dataclass
from pathlib import Path

import numpy as np
import onnxruntime as ort
from scipy.signal import resample_poly


SAMPLE_RATE = 16_000
FRAME_SAMPLES = 512
CONTEXT_SAMPLES = 64


class SileroScorer:
    def __init__(self, model: Path) -> None:
        self.session = ort.InferenceSession(str(model), providers=["CPUExecutionProvider"])

    def probabilities(self, audio: np.ndarray) -> list[float]:
        state = np.zeros((2, 1, 128), dtype=np.float32)
        context = np.zeros((1, CONTEXT_SAMPLES), dtype=np.float32)
        probabilities: list[float] = []
        for start in range(0, len(audio), FRAME_SAMPLES):
            frame = np.zeros((1, FRAME_SAMPLES), dtype=np.float32)
            source = audio[start : start + FRAME_SAMPLES]
            frame[0, : len(source)] = source
            model_input = np.concatenate((context, frame), axis=1)
            output, state = self.session.run(
                None,
                {"input": model_input, "state": state, "sr": np.array(SAMPLE_RATE, np.int64)},
            )
            probabilities.append(float(output[0, 0]))
            context = frame[:, -CONTEXT_SAMPLES:]
        return probabilities


@dataclass(frozen=True)
class Boundary:
    start: int
    end: int


def boundaries(
    probabilities: list[float],
    threshold: float,
    min_speech_ms: int,
    min_silence_ms: int,
    pad_ms: int,
    total_samples: int,
) -> list[Boundary]:
    negative_threshold = max(0.01, threshold - 0.15)
    min_speech = SAMPLE_RATE * min_speech_ms // 1_000
    min_silence = SAMPLE_RATE * min_silence_ms // 1_000
    pad = SAMPLE_RATE * pad_ms // 1_000
    candidate: int | None = None
    active: int | None = None
    silence: int | None = None
    found: list[Boundary] = []

    for index, probability in enumerate(probabilities):
        frame_start = index * FRAME_SAMPLES
        frame_end = frame_start + FRAME_SAMPLES
        if active is None:
            if probability >= threshold:
                candidate = frame_start if candidate is None else candidate
                if frame_end - candidate >= min_speech:
                    active = max(0, candidate - pad)
                    candidate = None
                    silence = None
            else:
                candidate = None
            continue

        if probability >= threshold:
            silence = None
        elif probability < negative_threshold and silence is None:
            silence = frame_start

        if silence is not None and frame_end - silence >= min_silence:
            found.append(Boundary(active, min(frame_end, silence + pad, total_samples)))
            candidate = active = silence = None

    if active is not None:
        found.append(Boundary(active, total_samples))
    return found


def read_wav(path: Path) -> np.ndarray:
    with wave.open(str(path), "rb") as wav:
        if wav.getnchannels() != 1 or wav.getsampwidth() != 2:
            raise ValueError(f"Expected mono PCM16: {path}")
        rate = wav.getframerate()
        pcm = np.frombuffer(wav.readframes(wav.getnframes()), dtype="<i2").astype(np.float32)
    audio = pcm / 32_768.0
    if rate != SAMPLE_RATE:
        divisor = math.gcd(rate, SAMPLE_RATE)
        audio = resample_poly(audio, SAMPLE_RATE // divisor, rate // divisor).astype(np.float32)
    return audio


def add_noise(audio: np.ndarray, snr_db: float, rng: np.random.Generator) -> np.ndarray:
    signal_rms = float(np.sqrt(np.mean(np.square(audio))))
    noise_rms = signal_rms / (10 ** (snr_db / 20.0))
    return np.clip(audio + rng.normal(0.0, noise_rms, audio.shape), -1.0, 1.0).astype(np.float32)


def energy_bounds(audio: np.ndarray) -> tuple[int, int]:
    """Approximate the spoken payload inside SAPI WAV leading/trailing silence."""
    window = SAMPLE_RATE // 100
    rms = np.array(
        [
            np.sqrt(np.mean(np.square(audio[start : start + window])))
            for start in range(0, len(audio), window)
        ],
    )
    active = np.flatnonzero(rms >= max(0.005, float(rms.max()) * 0.10))
    if not len(active):
        return 0, len(audio)
    return int(active[0] * window), min(len(audio), int((active[-1] + 1) * window))


def evaluate(args: argparse.Namespace) -> list[dict[str, object]]:
    scorer = SileroScorer(args.model)
    clips = [read_wav(path) for path in sorted(args.raw_dir.glob("tts_*.wav"))]
    if len(clips) < 30:
        raise ValueError(f"Expected at least 30 TTS clips, found {len(clips)}")
    rng = np.random.default_rng(args.seed)
    rows: list[dict[str, object]] = []

    for label, snr in [("clean", None), ("20dB", 20.0), ("10dB", 10.0), ("0dB", 0.0)]:
        probability_sets: list[tuple[list[float], int, int, int]] = []
        for clip in clips:
            clip_start, clip_end = energy_bounds(clip)
            speech = clip if snr is None else add_noise(clip, snr, rng)
            prefix = np.zeros(SAMPLE_RATE // 2, np.float32)
            suffix = np.zeros(SAMPLE_RATE // 2, np.float32)
            sample = np.concatenate((prefix, speech, suffix))
            probability_sets.append(
                (
                    scorer.probabilities(sample),
                    len(sample),
                    len(prefix) + clip_start,
                    len(prefix) + clip_end,
                ),
            )

        for threshold in args.thresholds:
            detected = 0
            coverages: list[float] = []
            start_errors: list[float] = []
            end_errors: list[float] = []
            for probabilities, total, expected_start, expected_end in probability_sets:
                segments = boundaries(
                    probabilities,
                    threshold,
                    args.min_speech_ms,
                    args.min_silence_ms,
                    args.pad_ms,
                    total,
                )
                if not segments:
                    continue
                detected += 1
                best = max(
                    segments,
                    key=lambda segment: max(
                        0,
                        min(segment.end, expected_end) - max(segment.start, expected_start),
                    ),
                )
                overlap = max(0, min(best.end, expected_end) - max(best.start, expected_start))
                coverages.append(overlap / max(1, expected_end - expected_start) * 100.0)
                start_errors.append((best.start - expected_start) * 1_000.0 / SAMPLE_RATE)
                end_errors.append((best.end - expected_end) * 1_000.0 / SAMPLE_RATE)
            rows.append(
                {
                    "threshold": threshold,
                    "condition": label,
                    "clips": len(clips),
                    "triggered_pct": round(detected / len(clips) * 100.0, 1),
                    "median_coverage_pct": round(float(np.median(coverages)), 1) if coverages else 0.0,
                    "median_start_error_ms": round(float(np.median(start_errors)), 1) if start_errors else 0.0,
                    "median_end_error_ms": round(float(np.median(end_errors)), 1) if end_errors else 0.0,
                },
            )
    noise_probability_sets: list[tuple[list[float], int]] = []
    for clip in clips:
        rms = max(0.005, float(np.sqrt(np.mean(np.square(clip)))))
        noise = rng.normal(0.0, rms, len(clip) + SAMPLE_RATE).astype(np.float32)
        noise_probability_sets.append((scorer.probabilities(noise), len(noise)))
    for threshold in args.thresholds:
        triggered = sum(
            bool(
                boundaries(
                    probabilities,
                    threshold,
                    args.min_speech_ms,
                    args.min_silence_ms,
                    args.pad_ms,
                    total,
                ),
            )
            for probabilities, total in noise_probability_sets
        )
        rows.append(
            {
                "threshold": threshold,
                "condition": "noise-only",
                "clips": len(clips),
                "triggered_pct": round(triggered / len(clips) * 100.0, 1),
                "median_coverage_pct": 0.0,
                "median_start_error_ms": 0.0,
                "median_end_error_ms": 0.0,
            },
        )
    return rows


def main() -> None:
    parser = argparse.ArgumentParser()
    module_root = Path(__file__).resolve().parents[1]
    parser.add_argument(
        "--model",
        type=Path,
        default=module_root / "src/main/assets/silero_vad_v6_2_1.onnx",
    )
    parser.add_argument("--raw-dir", type=Path, default=module_root / "src/main/res/raw")
    parser.add_argument("--thresholds", type=float, nargs="+", default=[0.35, 0.50, 0.65])
    parser.add_argument("--min-speech-ms", type=int, default=250)
    parser.add_argument("--min-silence-ms", type=int, default=100)
    parser.add_argument("--pad-ms", type=int, default=30)
    parser.add_argument("--seed", type=int, default=20260801)
    args = parser.parse_args()

    writer = csv.DictWriter(
        __import__("sys").stdout,
        fieldnames=[
            "threshold",
            "condition",
            "clips",
            "triggered_pct",
            "median_coverage_pct",
            "median_start_error_ms",
            "median_end_error_ms",
        ],
        lineterminator="\n",
    )
    writer.writeheader()
    writer.writerows(evaluate(args))


if __name__ == "__main__":
    main()
