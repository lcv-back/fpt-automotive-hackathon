# BÀN GIAO L2 + NỘI DUNG STANDUP 29/07
### Long · theo `07-PLAN-CA-NHAN-LONG.md` PHẦN 2 (T4 29/07)

---

## PHẦN 1 — XONG GÌ HÔM NAY

| Task | Trạng thái | "Xong khi" theo `06` | Bằng chứng |
|---|---|---|---|
| **L2a** `LatencyTrace` khung + tên chặng | ✅ code | *Class biên dịch được* | ⚠️ **chưa biên dịch được** — xem PHẦN 4 |
| **L2b** log format + `_SUMMARY` | ✅ code + log mẫu | *Vĩ parse được 1 dòng mẫu* | `android/voice/fixtures/` — đã kiểm bằng đúng luật parse của Vĩ |
| **L3a** push-to-talk `AudioRecord` | ✅ code + test | *Giữ nút → ra file wav nghe rõ* | ⚠️ **chưa nghe được** — cần Device |

File mới:

```text
android/voice/
├── build.gradle.kts
├── README.md                     ← Dương đọc mục "Ghép vào app shell"
├── fixtures/                     ← Vĩ đọc cái này
│   ├── README.md
│   ├── golden_trace.log          4 lượt: Allow · Deny · Confirm · Error
│   └── golden_trace_edge.log     ca biên + 4 dòng cố tình hỏng
└── src/
    ├── main/kotlin/com/viva/voice/
    │   ├── trace/  Stage · NanoClock · TraceSink · TraceVerdict · LatencyTrace · AndroidTrace
    │   └── audio/  AudioConfig · PcmSource · PushToTalkRecorder · WavWriter · AndroidPcmSource
    └── test/kotlin/com/viva/voice/   3 file test, 30 case
```

`vong2/03-contracts.md` §1 đã viết lại: thêm §1.1 luật format · §1.2 grammar verdict ·
§1.3 ba chỗ khác bản phác 28/07 · §1.4 log mẫu.

---

## PHẦN 2 — GỬI VĨ (bạn đang chờ cái này)

**1. Câu hỏi treo trong `backend/CLAUDE.md` của bạn đã có trả lời.** Bạn ghi:

> *"Format chuỗi `Verdict` trong dòng `VIVA_TRACE_SUMMARY` … chưa rõ serialize thành gì
> (`"Allow"`? `"Deny:G1_SPEED_LOCK"`?). Hỏi Long trước khi dùng field này để lọc pass/fail."*

Chốt: **`Deny:G1_SPEED_LOCK`** — có mã luật. Grammar đầy đủ:

```text
verdict := "Allow" | "Deny:"<RULE_ID> | "Confirm:"<RULE_ID> | "Error:"<STAGE_ID>
```

Tách bằng dấu `:` **đầu tiên**. Lọc pass/fail được ngay, và group-by theo `RULE_ID` chính
là bảng N4b của Tùng cần (ablation A1).

**2. `Error:<stage>` là dạng verdict mới** — lượt chết giữa chừng (ASR timeout, VHAL không
trả về) **vẫn in dòng summary**, khai luôn chết ở chặng nào. Trước đây lượt đó không có
summary và **biến mất khỏi benchmark**. Vẫn nằm trong grammar cũ nên **parser của bạn không
phải sửa** — chỉ cần biết là sẽ gặp.

**3. `e2e_ms` = `speech_end` → `tts_start`**, số nguyên. Không phải `speech_start`, vì tính
từ đó là cộng cả thời gian tài xế nói → câu dài thành "hệ thống chậm". Đây là con số cam kết
p95 < 1500ms nằm trên, nên nó cần một định nghĩa viết ra.
Muốn đo màn hình thì tự tính `speech_end → render_done` từ mốc thô.

**4. `golden_trace_edge.log` là bài kiểm cho harness**, không phải log đẹp:
4 dòng cố tình hỏng phải ra **đúng 4 warning**, không crash, và **không được vứt mốc hợp lệ
cùng `traceId`**. Chạy:

```bash
cd backend
go run ./cmd/viva-tools harness --input ../android/voice/fixtures/golden_trace_edge.log --out /tmp/edge.csv
```

**5. Gợi ý cho V8** *(không bắt buộc)*: `ParseEventLine` hiện nhận **mọi** chuỗi làm tên chặng.
Gõ sai `asr_dnoe` sẽ thành một mark hợp lệ và chỉ lộ ra khi CSV thủng cột. Phía Kotlin đã
chặn bằng enum; nếu bạn đối chiếu thêm với `CanonicalStageOrder` và cảnh báo khi lệch thì
hai đầu khớp nhau.

---

## PHẦN 3 — GỬI TÙNG & DƯƠNG

**Tùng** — đánh mốc chỉ 1 dòng, và `SafetyGuard` cần map `Verdict` → chuỗi log:

```kotlin
trace.mark(Stage.GUARD_DONE)
// rồi khi đóng lượt:
TraceVerdict.Deny("G1_SPEED_LOCK")        // mã luật đúng như bảng §4
TraceVerdict.Confirm("G2_CONFIRM_DOOR")
```

Mã luật vào log **không phải để cho đẹp** — N4b của bạn (ablation A1, 06/08) là một câu
group-by trên CSV nếu có nó, và là chạy tay lại demo nếu không có.

**Dương** — mục *"Ghép vào app shell"* trong `android/voice/README.md`: 3 dòng vào
`settings.gradle.kts` + 1 dòng dependency. Module là **library**, không đụng gì vào app.

---

## PHẦN 4 — ⚠️ HAI THỨ CHẶN, PHẢI NÊU Ở STANDUP

### ① Repo chưa có project Android nào — và **không ai đang chạy được test**

`06` PHẦN 5 dự phòng cho D1 trễ bằng *"chạy module bằng unit test, ghép sau"*. Nhưng chạy
unit test **vẫn cần một project Gradle**: `settings.gradle.kts` + wrapper. Repo hiện chưa có,
và cũng chưa có `local.properties`/Android SDK trên máy đang làm.

→ Hệ quả thật: **code L2 và L3a chưa từng được biên dịch.** Nó viết theo contract và có 30
case test, nhưng "biên dịch được" trong ô *xong khi* của L2 thì **chưa tick được**.

**Việc số 1 tối nay, trước mọi tính năng:** Dương dựng xong D1 (hoặc tối thiểu
`settings.gradle.kts` + gradle wrapper) để cả 3 người còn lại chạy được `./gradlew test`.
Nếu D1 trượt qua 30/07 thì bản tối thiểu chỉ mất ~5 phút và phải làm ngay trong tối nay —
đây là thứ chặn cả Long, Tùng lẫn chính Dương.

### ② `07` và `08` chưa có trên `main` — brief mentor đang trỏ vào bản cũ

`09-brief-kickoff-mentor.md` mục "KIỂM TRA TRƯỚC KHI GỬI" đã cảnh báo, và **vẫn đúng tới
giờ này**: `origin/main` chỉ có tới `06` bản 28/07. Brief nói *"PHẦN 8: bỏ hẳn DTC"*, mentor
mở link ra thấy DTC còn nguyên, và bấm vào `08` thì 404.

→ **Push `06` + `07` + `08` lên `main` TRƯỚC, rồi mới gửi brief.** Đảo thứ tự là mentor đọc
bản sai ngay lần tiếp xúc đầu tiên của Vòng 2.

---

## PHẦN 5 — SCRIPT STANDUP 21:30

**Long — 3 câu:**

1. *Hôm qua:* L1 kick-off, dựng khung `LatencyTrace`.
2. *Hôm nay:* xong **L2** (log format + grammar verdict + log mẫu bàn giao Vĩ) và **L3a**
   (push-to-talk). Trả lời xong câu hỏi treo của Vĩ về ô `verdict`.
3. *Đang bị chặn:* **chưa có project Gradle nên chưa biên dịch được dòng nào.** Chờ D1.

**Bốn thứ phải chốt bằng lời, không để "mai xem sao":**

- [ ] **Dương:** D1 tối nay xong tới đâu? Nếu chưa xong, dựng `settings.gradle.kts` + wrapper
      tối thiểu **ngay tối nay** — 3 người đang chờ.
- [ ] **Tùng:** xác nhận nhận **N3b + N4b** thay T10. Từ giờ **không viết dòng code DTC nào**
      (`08` PHẦN 9).
- [ ] **Vĩ:** V4 (repo + CI build APK) hôm nay — CI hiện chỉ build Go. Có thêm bước build
      Android không, và repo Android là repo này hay repo khác?
- [ ] **Cả đội:** đã ai chạy được gì **trên CarSky** chưa? Mốc cân 1 (31/07) có câu hỏi bắt
      buộc *"lần chạy vừa rồi có phải trên CarSky không"* — chạy emulator local = trần cứng
      L1 = mất phần lớn **15đ**. Còn 2 ngày.

**Nhắc lại quyết định đã chốt** (không mở lại thảo luận): T10 DTC bỏ hẳn · L5 xuống 10 intent ·
D5 bỏ `LocalMediaProvider` · D6 bỏ disk cache.

---

## PHẦN 6 — BA QUYẾT ĐỊNH ĐÃ RA HÔM NAY, GHI LẠI ĐỂ KHÔNG PHẢI CÃI LẠI

| # | Quyết định | Bỏ lựa chọn nào | Vì sao |
|---|---|---|---|
| 1 | `verdict` mang theo mã luật: `Deny:G1_SPEED_LOCK` | `Deny` trơn | Cùng chi phí in ra; đổi lấy bảng ablation N4b làm bằng group-by thay vì chạy tay lại demo. Khối ③ **6đ + 7đ** |
| 2 | Thêm `Error:<stage>` cho lượt chết giữa chừng | Im lặng, không có summary | Lượt hỏng trước đây biến mất khỏi benchmark. Ăn ô *"Xử lý lỗi và khả năng quan sát"* **4đ** mà `08` đang chấm 🟡 yếu. Không phải sửa parser |
| 3 | `e2e_ms` = `speech_end` → `tts_start`, số nguyên | `speech_start` → `tts_start`; số thập phân | Tính từ `speech_start` là đo tài xế nói nhanh hay chậm, không đo hệ thống. Số thập phân trên máy locale `vi-VN` ra `690,0` → Go từ chối → **hỏng đúng lúc demo, chỉ hỏng trên máy thật** |

Chi tiết lý do nằm ngay trong code (`TraceVerdict.kt`, `LatencyTrace.e2eMs`) và
`03-contracts.md` §1.2–§1.3 — **N1 Claim–Evidence Map (07/08) lấy nguyên chỗ này làm nguồn**,
đừng viết lại từ đầu.
