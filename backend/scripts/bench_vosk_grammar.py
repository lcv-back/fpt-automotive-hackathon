# -*- coding: utf-8 -*-
"""Đo tác dụng của ràng buộc vốn từ trên corpus giọng người thật.

Câu hỏi script này trả lời: thu vốn từ của Vosk từ 19.529 xuống ~100 từ làm
transcript tốt lên hay tệ đi, trên **giọng nói thật** chứ không phải trên biến
thể sinh tay.

Cách đo: giải mã đúng một bộ WAV hai lần — một lần grammar bật, một lần tắt —
rồi so với nhãn chuẩn. Cùng model, cùng file, cùng decoder; khác đúng một biến.

Chạy:
    python backend/scripts/bench_vosk_grammar.py

Dùng `vosk` bản desktop chứ không phải APK: cùng libvosk, cùng file model, nhưng
không phải chờ 7 phút build và không phụ thuộc micro ảo của emulator.
"""
import io
import json
import os
import re
import sys
import unicodedata
import wave

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODEL_DIR = os.path.join(
    ROOT, "automotive", "feature", "voice", "src", "main", "assets", "model-vi"
)
PROMPTS = os.path.join(ROOT, "asr", "scripts", "corpus_prompts.tsv")
RAW_DIR = os.path.join(ROOT, "evidence", "asr", "corpus-human", "raw")

# Cùng danh sách với CommandVocabulary.kt. Cố ý chép ra chứ không sinh tự động:
# script này là phép đo độc lập, và nếu nó đọc thẳng file Kotlin thì một lỗi
# trong file đó sẽ tự xác nhận chính nó.
VOCAB = (
    "viva ơi "
    "điều hòa hoà máy lạnh nóng ấm nhiệt độ tăng giảm hạ lên xuống đặt để về "
    "quạt gió mức số mạnh nhẹ to nhỏ "
    "mở khóa khoá đóng cửa xe "
    "âm lượng nhạc bài hát chuyển tiếp theo phát dừng tạm "
    "chặng điểm giao hàng đơn kế xác nhận đã thành công "
    "còn bao nhiêu xăng nhiên liệu pin điện ắc quy "
    "tốc hiện tại đang chạy là gì cho biết mấy thế nào "
    "không một hai ba bốn tư năm lăm sáu bảy tám chín mười mươi linh rưỡi phần trăm "
    "a b c d e g h"
).split()


def fold(text):
    """Bỏ dấu và `đ`→`d`, để so khớp không bị thanh điệu chi phối."""
    text = unicodedata.normalize("NFD", text.lower())
    text = "".join(c for c in text if not unicodedata.combining(c))
    return text.replace("đ", "d")


def words_of(text):
    return re.findall(r"[^\W_]+", fold(text), re.UNICODE)


def wer(reference, hypothesis):
    """Word error rate theo khoảng cách Levenshtein trên chuỗi từ đã bỏ dấu."""
    ref, hyp = words_of(reference), words_of(hypothesis)
    if not ref:
        return 0.0 if not hyp else 1.0
    prev = list(range(len(hyp) + 1))
    for i, r in enumerate(ref, 1):
        cur = [i]
        for j, h in enumerate(hyp, 1):
            cur.append(min(prev[j] + 1, cur[j - 1] + 1, prev[j - 1] + (r != h)))
        prev = cur
    return prev[-1] / len(ref)


def decode(model, path, grammar):
    """Giải mã một file, trả về (text, min_conf). `grammar=None` là không ràng buộc."""
    from vosk import KaldiRecognizer

    with wave.open(path, "rb") as w:
        assert w.getnchannels() == 1 and w.getsampwidth() == 2, path
        rate = w.getframerate()
        pcm = w.readframes(w.getnframes())

    rec = (
        KaldiRecognizer(model, rate, json.dumps(grammar, ensure_ascii=False))
        if grammar
        else KaldiRecognizer(model, rate)
    )
    rec.SetWords(True)

    # Đẩy theo khối 4000 mẫu cho giống đường chạy streaming trên máy, thay vì
    # nạp cả file một lần: endpointer của Vosk phụ thuộc vào cách audio tới.
    step = 8000
    for off in range(0, len(pcm), step):
        rec.AcceptWaveform(pcm[off:off + step])
    final = json.loads(rec.FinalResult())

    confs = [w["conf"] for w in final.get("result", []) if "conf" in w]
    return final.get("text", "").strip(), (min(confs) if confs else None)


def main():
    from vosk import Model, SetLogLevel

    SetLogLevel(-1)
    if not os.path.isdir(MODEL_DIR):
        raise SystemExit("Chua co model: %s" % MODEL_DIR)

    prompts = []
    with io.open(PROMPTS, encoding="utf-8") as f:
        next(f)
        for line in f:
            if line.strip():
                name, text = line.rstrip("\n").split("\t")
                prompts.append((name, text))

    model = Model(MODEL_DIR)
    out = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

    grammar = VOCAB + ["[unk]"]
    rows, sum_off, sum_on = [], 0.0, 0.0
    for name, want in prompts:
        path = os.path.join(RAW_DIR, name + ".wav")
        if not os.path.exists(path):
            out.write("BO QUA (khong co file): %s\n" % name)
            continue
        off_text, _ = decode(model, path, None)
        on_text, on_conf = decode(model, path, grammar)
        w_off, w_on = wer(want, off_text), wer(want, on_text)
        sum_off += w_off
        sum_on += w_on
        rows.append((name, want, off_text, w_off, on_text, w_on, on_conf))

    out.write("\n%-22s %s\n" % ("", "WER thap hon la tot hon"))
    out.write("=" * 100 + "\n")
    for name, want, off_text, w_off, on_text, w_on, conf in rows:
        mark = "  " if abs(w_on - w_off) < 1e-9 else ("TOT HON" if w_on < w_off else "TE HON")
        out.write("\n[%s] %s\n" % (name, want))
        out.write("   grammar TAT  wer=%.2f  \"%s\"\n" % (w_off, off_text))
        out.write("   grammar BAT  wer=%.2f  \"%s\"  (min conf=%s)  %s\n"
                  % (w_on, on_text, "-" if conf is None else "%.2f" % conf, mark))

    n = len(rows) or 1
    out.write("\n" + "=" * 100 + "\n")
    out.write("So cau        : %d\n" % len(rows))
    out.write("WER grammar TAT: %.3f\n" % (sum_off / n))
    out.write("WER grammar BAT: %.3f\n" % (sum_on / n))
    delta = (sum_off - sum_on) / n
    out.write("Chenh lech     : %+.3f (%s)\n"
              % (delta, "grammar giup" if delta > 0 else "grammar lam te di"))
    out.flush()


if __name__ == "__main__":
    main()
