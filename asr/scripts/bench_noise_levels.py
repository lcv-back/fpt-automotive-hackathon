#!/usr/bin/env python3
"""V12 — run the benchmark corpus through viva-asr at every noise level.

Task V12 in `vong2/06-PHAN-CONG-4-NGUOI.md` asks for "20 cau x 3 muc nhieu,
p50/p95 + bieu do". The measurement rules are not free-form; `vong2/15-QUYET-DINH-BENCHMARK-ASR.md`
fixes them, and this script implements that matrix rather than a convenient
subset of it:

  * same utterances across levels          -> one corpus, built by noise_mix.py
  * cold run separated from steady state   -> --warmup requests, excluded and counted
  * failures stay in the sample            -> HTTP/timeout errors become Error:<stage>
                                              rows, never dropped
  * identity recorded                      -> image/model/commit written into the summary

Run order matters more than it looks. The first version of this script measured
each level as one contiguous block, and mean latency fell monotonically with the
block order — 678, 648, 628, 593 ms — which reads as "noise makes VIVA faster".
It does not: transcript length was flat across levels (35.2 vs 35.3 chars), so
the fall was machine drift that block order had silently renamed into a noise
effect. Levels are therefore **interleaved** by default: run order is decorrelated
from level, and drift spreads across all four instead of pooling in the last one.
`--order blocked` reproduces the old behaviour for comparison, nothing else.

    python scripts/bench_noise_levels.py --url http://127.0.0.1:8080 \
        --corpus ../evidence/asr/corpus --out-dir ../evidence/asr/v12

Dropping a failed utterance would raise the mean and shrink p95 at the same
time, which is precisely the direction that flatters us — so a request that
errors is scored WER 1.0 and kept. The summary prints the error count next to
every aggregate so the two are never read apart.

Stdlib only.
"""

from __future__ import annotations

import argparse
import csv
import json
import random
import statistics
import subprocess
import sys
import urllib.error
import urllib.request
import uuid
import wave
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from bench_tts_samples import TARGET_RATE, load_ground_truth, word_error_rate  # noqa: E402
from noise_mix import LEVELS  # noqa: E402

LEVEL_ORDER = ["clean", "quiet", "cabin", "highway"]


def read_pcm16(path: Path) -> tuple[bytes, float]:
    """Corpus WAVs are already mono/16-bit/16 kHz — assert rather than resample.

    noise_mix.py did the one resample this pipeline is allowed to do. A second
    silent resample here would change the signal the CSV claims to describe.
    """
    with wave.open(str(path), "rb") as wav:
        if (wav.getnchannels(), wav.getsampwidth(), wav.getframerate()) != (1, 2, TARGET_RATE):
            raise ValueError(
                f"{path.name}: can mono/16-bit/{TARGET_RATE} Hz, dang co "
                f"{wav.getnchannels()}ch/{wav.getsampwidth() * 8}-bit/{wav.getframerate()} Hz"
            )
        frames = wav.readframes(wav.getnframes())
    return frames, len(frames) / 2 / TARGET_RATE * 1000.0


def transcribe(url: str, pcm: bytes, timeout: float) -> dict:
    request = urllib.request.Request(
        url.rstrip("/") + "/asr",
        data=pcm,
        method="POST",
        headers={
            "Content-Type": "application/octet-stream",
            "X-Sample-Rate": str(TARGET_RATE),
            "X-Trace-Id": str(uuid.uuid4()),
        },
    )
    with urllib.request.urlopen(request, timeout=timeout) as resp:
        return json.loads(resp.read())


def percentile(values: list[float], fraction: float) -> float:
    """Nearest-rank percentile on the sorted sample.

    Same rule as bench_tts_samples.py and as the Go harness, so p95 means one
    thing across every VIVA report. Interpolating here would make the ASR
    numbers quietly incomparable with the on-device ones.
    """
    if not values:
        return 0.0
    ordered = sorted(values)
    index = max(0, min(len(ordered) - 1, round(fraction * len(ordered)) - 1))
    return ordered[index]


def git_commit(repo: Path) -> str:
    try:
        out = subprocess.run(
            ["git", "-C", str(repo), "rev-parse", "HEAD"],
            capture_output=True, text=True, timeout=10,
        )
        return out.stdout.strip() or "unknown"
    except (OSError, subprocess.SubprocessError):
        return "unknown"


def service_identity(url: str, timeout: float) -> dict:
    try:
        with urllib.request.urlopen(url.rstrip("/") + "/health", timeout=timeout) as resp:
            return json.loads(resp.read())
    except (urllib.error.URLError, json.JSONDecodeError, OSError) as exc:
        return {"error": str(exc)}


def summarise(rows: list[dict]) -> dict:
    """Aggregate one level. Errors are inside `rows` and stay inside the stats."""
    wers = [r["wer"] for r in rows]
    latencies = [r["server_ms"] for r in rows if not r["error"]]
    rtfs = [r["rtf"] for r in rows if not r["error"] and r["rtf"]]
    return {
        "n": len(rows),
        "errors": sum(1 for r in rows if r["error"]),
        "empty_text": sum(1 for r in rows if not r["error"] and not r["hypothesis"].strip()),
        "wer_mean": round(statistics.mean(wers), 4) if wers else 0.0,
        "wer_median": round(statistics.median(wers), 4) if wers else 0.0,
        "wer_zero": sum(1 for w in wers if w == 0),
        "rtf_median": round(statistics.median(rtfs), 4) if rtfs else 0.0,
        "server_p50": round(percentile(latencies, 0.50), 1),
        "server_p95": round(percentile(latencies, 0.95), 1),
        "server_max": round(max(latencies), 1) if latencies else 0.0,
    }


def render_svg(summaries: dict[str, dict], levels: list[str], path: Path) -> None:
    """Hand-rolled SVG so the chart has no dependency and diffs as text.

    Two panels sharing an x axis: latency (p50/p95 bars) and WER (a line), because
    the whole question V12 answers is whether noise costs accuracy, latency, or both.
    """
    width, height = 720, 340
    pad_l, pad_r, pad_t, pad_b = 62, 62, 46, 54
    plot_w = width - pad_l - pad_r
    plot_h = height - pad_t - pad_b
    n = len(levels)
    slot = plot_w / max(1, n)

    lat_max = max([summaries[lv]["server_p95"] for lv in levels] + [1.0]) * 1.25
    wer_max = max([summaries[lv]["wer_mean"] for lv in levels] + [0.1]) * 1.3
    wer_max = min(1.0, max(wer_max, 0.2))

    def x_center(i: int) -> float:
        return pad_l + slot * (i + 0.5)

    def y_lat(value: float) -> float:
        return pad_t + plot_h * (1.0 - value / lat_max)

    def y_wer(value: float) -> float:
        return pad_t + plot_h * (1.0 - value / wer_max)

    parts = [
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" '
        f'viewBox="0 0 {width} {height}" font-family="Segoe UI, Arial, sans-serif">',
        f'<rect width="{width}" height="{height}" fill="#ffffff"/>',
        f'<text x="{width/2}" y="22" text-anchor="middle" font-size="15" font-weight="600">'
        f'viva-asr — do tre va WER theo muc nhieu</text>',
        f'<text x="{width/2}" y="38" text-anchor="middle" font-size="10" fill="#8a6d1f">'
        f'Nhieu TONG HOP tren giong TTS, do tren CPU may dev — khong phai so cua CarSky/Device</text>',
    ]

    for tick in range(5):
        value = lat_max * tick / 4
        y = y_lat(value)
        parts.append(f'<line x1="{pad_l}" y1="{y:.1f}" x2="{width-pad_r}" y2="{y:.1f}" '
                     f'stroke="#e8e8e8" stroke-width="1"/>')
        parts.append(f'<text x="{pad_l-8}" y="{y+3:.1f}" text-anchor="end" font-size="10" '
                     f'fill="#555">{value:.0f}</text>')
        wer_value = wer_max * tick / 4
        parts.append(f'<text x="{width-pad_r+8}" y="{y_wer(wer_value)+3:.1f}" font-size="10" '
                     f'fill="#c2410c">{wer_value:.2f}</text>')

    parts.append(f'<text x="{pad_l-8}" y="{pad_t-12}" text-anchor="end" font-size="10" '
                 f'fill="#555">ms</text>')
    parts.append(f'<text x="{width-pad_r+8}" y="{pad_t-12}" font-size="10" fill="#c2410c">WER</text>')

    bar_w = slot * 0.26
    for i, level in enumerate(levels):
        summary = summaries[level]
        cx = x_center(i)
        for offset, key, colour in ((-0.55, "server_p50", "#2563eb"), (0.55, "server_p95", "#93c5fd")):
            value = summary[key]
            x = cx + offset * bar_w - bar_w / 2
            y = y_lat(value)
            parts.append(f'<rect x="{x:.1f}" y="{y:.1f}" width="{bar_w:.1f}" '
                         f'height="{pad_t+plot_h-y:.1f}" fill="{colour}"/>')
            parts.append(f'<text x="{x+bar_w/2:.1f}" y="{y-4:.1f}" text-anchor="middle" '
                         f'font-size="9" fill="#1e3a8a">{value:.0f}</text>')

        label = level if level == "clean" else f'{level} · {LEVELS[level]["snr_db"]:.0f} dB'
        parts.append(f'<text x="{cx:.1f}" y="{height-pad_b+18}" text-anchor="middle" '
                     f'font-size="11">{label}</text>')
        parts.append(f'<text x="{cx:.1f}" y="{height-pad_b+32}" text-anchor="middle" '
                     f'font-size="9" fill="#777">n={summary["n"]} · loi={summary["errors"]}</text>')

    points = " ".join(f"{x_center(i):.1f},{y_wer(summaries[lv]['wer_mean']):.1f}"
                      for i, lv in enumerate(levels))
    parts.append(f'<polyline points="{points}" fill="none" stroke="#c2410c" stroke-width="2"/>')
    for i, level in enumerate(levels):
        value = summaries[level]["wer_mean"]
        parts.append(f'<circle cx="{x_center(i):.1f}" cy="{y_wer(value):.1f}" r="3.5" fill="#c2410c"/>')
        parts.append(f'<text x="{x_center(i):.1f}" y="{y_wer(value)-9:.1f}" text-anchor="middle" '
                     f'font-size="9" fill="#c2410c">{value:.3f}</text>')

    legend = [("#2563eb", "server p50"), ("#93c5fd", "server p95"), ("#c2410c", "WER trung binh")]
    lx = pad_l
    for colour, text in legend:
        parts.append(f'<rect x="{lx}" y="{height-16}" width="10" height="10" fill="{colour}"/>')
        parts.append(f'<text x="{lx+14}" y="{height-7}" font-size="10" fill="#333">{text}</text>')
        lx += 22 + len(text) * 6

    parts.append("</svg>")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("\n".join(parts), encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--url", default="http://127.0.0.1:8080")
    parser.add_argument("--corpus", default="../evidence/asr/corpus")
    parser.add_argument("--prompts", default="../android/voice/scripts/tts_prompts.tsv")
    parser.add_argument("--out-dir", default="../evidence/asr/v12")
    parser.add_argument("--levels", default="clean,quiet,cabin,highway")
    parser.add_argument("--warmup", type=int, default=5,
                        help="so request bo di truoc khi do (tach cold run)")
    parser.add_argument("--order", choices=("interleave", "blocked"), default="interleave",
                        help="interleave: tron thu tu de muc nhieu khong trung voi thu tu chay")
    parser.add_argument("--timeout", type=float, default=120.0)
    parser.add_argument("--label", default="LOCAL DOCKER — khong phai CarSky, khong phai Device")
    args = parser.parse_args()

    corpus = Path(args.corpus)
    index_path = corpus / "corpus-index.csv"
    if not index_path.exists():
        print(f"Chua co {index_path} — chay noise_mix.py truoc", file=sys.stderr)
        return 1

    levels = [lv.strip() for lv in args.levels.split(",") if lv.strip()]
    truth = load_ground_truth(Path(args.prompts))

    by_level: dict[str, list[Path]] = {lv: [] for lv in levels}
    with index_path.open(encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            if row["level"] in by_level and row["clip"] in truth:
                by_level[row["level"]].append(Path(row["path"]))
    missing = [lv for lv in levels if not by_level[lv]]
    if missing:
        print(f"Khong co clip nao co ground truth o muc: {', '.join(missing)}", file=sys.stderr)
        return 1

    identity = service_identity(args.url, args.timeout)
    if "error" in identity:
        print(f"Khong goi duoc /health tai {args.url}: {identity['error']}", file=sys.stderr)
        return 1

    # Cold run: the first requests after model load are slower and would sit in
    # the sample as a fake tail. Run them, throw them away, say how many.
    warm_source = by_level[levels[0]][0]
    warm_pcm, _ = read_pcm16(warm_source)
    warmed = 0
    for _ in range(max(0, args.warmup)):
        try:
            transcribe(args.url, warm_pcm, args.timeout)
            warmed += 1
        except (urllib.error.URLError, OSError) as exc:
            print(f"Warm-up that bai: {exc}", file=sys.stderr)
            return 1

    worklist = [(level, path) for level in levels for path in sorted(by_level[level])]
    if args.order == "interleave":
        # Fixed seed: the order is scrambled but reproducible, so a rerun measures
        # the same sequence and two runs stay comparable.
        random.Random("viva-v12-order").shuffle(worklist)

    rows: list[dict] = []
    for position, (level, path) in enumerate(worklist, start=1):
        clip = path.stem
        reference = truth[clip]
        pcm, audio_ms = read_pcm16(path)
        error = ""
        hypothesis = ""
        server_ms = 0
        confidence = 0.0
        try:
            body = transcribe(args.url, pcm, args.timeout)
            hypothesis = body.get("text", "")
            server_ms = int(body.get("server_ms", 0))
            confidence = float(body.get("confidence", 0.0) or 0.0)
        except urllib.error.HTTPError as exc:
            error = f"Error:asr_http_{exc.code}"
        except urllib.error.URLError as exc:
            error = f"Error:asr_unreachable({exc.reason})"
        except (OSError, json.JSONDecodeError) as exc:
            error = f"Error:asr_decode({exc})"

        wer = 1.0 if error else round(word_error_rate(reference, hypothesis), 4)
        rows.append({
            "run_position": position,   # kept so drift can be re-checked from the CSV
            "level": level,
            "snr_db": "" if level == "clean" else LEVELS[level]["snr_db"],
            "clip": clip,
            "reference": reference,
            "hypothesis": hypothesis,
            "wer": wer,
            "confidence": round(confidence, 4),
            "audio_ms": round(audio_ms, 1),
            "server_ms": server_ms,
            "rtf": round(server_ms / audio_ms, 4) if audio_ms and not error else 0.0,
            "error": error,
        })
        flag = f"  {error}" if error else ""
        print(f"  {position:>3}/{len(worklist)} {level:<8} {clip:<30} "
              f"WER={wer:.2f} server={server_ms}ms{flag}")

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)
    csv_path = out_dir / "v12-noise-levels.csv"
    with csv_path.open("w", encoding="utf-8", newline="") as handle:
        writer = csv.DictWriter(handle, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)

    summaries = {lv: summarise([r for r in rows if r["level"] == lv]) for lv in levels}
    svg_path = out_dir / "v12-noise-levels.svg"
    render_svg(summaries, levels, svg_path)

    lines = [
        "Evidence: V12 — benchmark theo ba muc nhieu",
        f"Label: {args.label}",
        f"Repository commit: {git_commit(Path(__file__).resolve().parents[2])}",
        f"Service /health: {json.dumps(identity, ensure_ascii=False)}",
        f"Corpus: {corpus.as_posix()} (xem corpus-index.csv de biet SNR tung file)",
        f"Ground truth: {Path(args.prompts).as_posix()}",
        f"Warm-up da bo: {warmed} request (tach cold run khoi steady-state)",
        "",
        f"{'muc':<9}{'n':>4}{'loi':>5}{'rong':>6}{'WER tb':>9}{'WER med':>9}"
        f"{'WER=0':>7}{'RTF med':>9}{'p50':>8}{'p95':>8}{'max':>8}",
    ]
    for level in levels:
        s = summaries[level]
        lines.append(
            f"{level:<9}{s['n']:>4}{s['errors']:>5}{s['empty_text']:>6}{s['wer_mean']:>9.3f}"
            f"{s['wer_median']:>9.3f}{s['wer_zero']:>7}{s['rtf_median']:>9.3f}"
            f"{s['server_p50']:>8.0f}{s['server_p95']:>8.0f}{s['server_max']:>8.0f}"
        )

    # Drift check. Interleaving stops run order from *masquerading* as a noise
    # effect; it does not stop drift existing. Measure it and print it, so the
    # reader can judge whether a 47 ms gap between levels means anything at all.
    ordered = sorted((r for r in rows if not r["error"]), key=lambda r: r["run_position"])
    half = len(ordered) // 2
    first_half = statistics.mean(r["server_ms"] for r in ordered[:half]) if half else 0.0
    second_half = statistics.mean(r["server_ms"] for r in ordered[half:]) if half else 0.0
    lines += [
        "",
        f"Thu tu chay: {args.order}"
        + ("  (muc nhieu da duoc tron, khong trung voi thu tu chay)"
           if args.order == "interleave" else "  (theo khoi — thu tu chay TRUNG voi muc nhieu)"),
        f"Drift trong luot do: nua dau {first_half:.0f} ms · nua sau {second_half:.0f} ms"
        f"  ({second_half - first_half:+.0f} ms)",
        "  -> Neu drift nay lon ngang chenh lech giua cac muc nhieu thi chenh lech do",
        "     KHONG doc duoc la do nhieu.",
    ]

    base = summaries[levels[0]]
    worst = summaries[levels[-1]]
    lines += [
        "",
        f"Suy giam {levels[0]} -> {levels[-1]}: WER {base['wer_mean']:.3f} -> {worst['wer_mean']:.3f}"
        f"  ({worst['wer_mean'] - base['wer_mean']:+.3f})",
        f"                          p95 {base['server_p95']:.0f} ms -> {worst['server_p95']:.0f} ms"
        f"  ({worst['server_p95'] - base['server_p95']:+.0f} ms)",
        "",
        "GIOI HAN — phai khai kem khi trich bat ky so nao o tren:",
        "  1. Nhieu la TONG HOP (pink noise co dinh dang pho), khong phai thu trong cabin.",
        "     Cac muc SNR 20/10/5 dB do doi chon, khong phai do do.",
        "  2. Giong noi la TTS tong hop, khong phai nguoi noi.",
        "  3. Latency do tren CPU may dev, KHONG phai node CarSky va KHONG phai Device.",
        "  4. Day la mot nua cua truc so sanh o 15-QUYET-DINH-BENCHMARK-ASR.md.",
        "     Nua con lai — Vosk on-device tren cung corpus — chua chay.",
        "  => Ket luan duoc phep rut ra: MUC SUY GIAM tuong doi giua cac muc nhieu.",
        "     Khong duoc phat bieu do chinh xac tuyet doi cua VIVA tren duong that.",
    ]
    summary_path = out_dir / "v12-manifest.txt"
    summary_path.write_text("\n".join(lines) + "\n", encoding="utf-8")

    print()
    print("\n".join(lines[8:]))
    print()
    print(f"CSV   -> {csv_path}")
    print(f"Chart -> {svg_path}")
    print(f"Manifest -> {summary_path}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
