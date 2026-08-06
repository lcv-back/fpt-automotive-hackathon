# -*- coding: utf-8 -*-
"""Kiểm mọi từ trong `CommandVocabulary.kt` đều có trong vốn từ của model Vosk.

Vì sao cần: Vosk **lặng lẽ bỏ qua** từ nào không có trong bảng ký hiệu của model.
Một lỗi chính tả trong danh sách vốn từ vì thế thành một lệnh không bao giờ nhận
ra được, mà không sinh lấy một dòng log nào. Đây là thứ duy nhất bắt được lỗi đó.

Model `vosk-model-small-vn-0.4` **không** kèm `graph/words.txt` rời — bảng ký hiệu
được gắn thẳng vào `graph/Gr.fst` theo định dạng nhị phân của OpenFst. Script này
đọc phần header đó ra thay vì đòi một file không tồn tại.

Chạy:
    python backend/scripts/check_command_vocab.py

Thoát 0 nếu mọi từ đều hợp lệ, 1 nếu có từ lạ.
"""
import io
import os
import re
import struct
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
GR_FST = os.path.join(
    ROOT, "automotive", "feature", "voice", "src", "main", "assets",
    "model-vi", "graph", "Gr.fst",
)
VOCAB_KT = os.path.join(
    ROOT, "automotive", "feature", "voice", "src", "main", "java", "com", "sopa",
    "viva_automotive", "feature", "voice", "data", "vosk", "CommandVocabulary.kt",
)

FST_MAGIC = 2125659606
SYMTAB_MAGIC = 2125658996


def model_vocabulary(path):
    """Rút bảng ký hiệu đầu ra khỏi header nhị phân của một file OpenFst.

    Bố cục theo `FstHeader::Read` của OpenFst: magic, fsttype, arctype, version,
    flags, properties, start, numstates, numarcs, rồi tới các bảng ký hiệu nếu
    bit tương ứng trong `flags` được bật (0x1 = input, 0x2 = output). Chuỗi ghi
    kèm độ dài `int32` phía trước.
    """
    with open(path, "rb") as f:
        def i32():
            return struct.unpack("<i", f.read(4))[0]

        def i64():
            return struct.unpack("<q", f.read(8))[0]

        def text():
            return f.read(i32()).decode("utf-8", "replace")

        magic = i32()
        if magic != FST_MAGIC:
            raise SystemExit("%s khong phai file OpenFst (magic=%d)" % (path, magic))
        text()          # fsttype
        text()          # arctype
        i32()           # version
        flags = i32()
        for _ in range(4):
            i64()       # properties, start, numstates, numarcs

        def symbol_table():
            if i32() != SYMTAB_MAGIC:
                raise SystemExit("bang ky hieu hong trong %s" % path)
            text()      # ten bang
            i64()       # available_key
            count = i64()
            out = []
            for _ in range(count):
                sym = text()
                i64()   # key
                out.append(sym)
            return out

        isyms = symbol_table() if flags & 0x1 else []
        osyms = symbol_table() if flags & 0x2 else []
        return set(osyms or isyms)


def declared_words(path):
    """Lấy các chuỗi trong khối `WORDS` của CommandVocabulary.kt.

    Cắt đúng khối đó thay vì quét cả file, rồi bỏ tiếp phần sau `//` trên mỗi
    dòng: chú thích ngay trong khối có trích dẫn câu nói ("24 độ", "hai mươi bốn
    độ"), và đếm chúng thành vốn từ sẽ báo thiếu giả.
    """
    src = io.open(path, encoding="utf-8").read()
    start = src.index("private val WORDS")
    end = src.index("fun asGrammarJson", start)
    code = "\n".join(line.split("//")[0] for line in src[start:end].splitlines())
    return re.findall(r'"([^"]+)"', code)


def main():
    if not os.path.exists(GR_FST):
        raise SystemExit(
            "Chua co %s — chay `./gradlew :feature:voice:assembleMockDebug` de tai model." % GR_FST
        )
    vocab = model_vocabulary(GR_FST)
    words = declared_words(VOCAB_KT)
    missing = [w for w in words if w not in vocab]

    out = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    out.write("vocab model : %d tu\n" % len(vocab))
    out.write("CommandVocabulary: %d tu\n" % len(words))
    if missing:
        out.write("\nTHIEU %d tu — Vosk se lang le bo qua chung:\n" % len(missing))
        for w in missing:
            out.write("  %s\n" % w)
        out.flush()
        return 1
    out.write("OK — moi tu deu co trong vocab cua model.\n")
    out.flush()
    return 0


if __name__ == "__main__":
    sys.exit(main())
