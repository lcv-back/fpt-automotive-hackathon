# PHÂN CÔNG VÒNG 2 — 4 NGƯỜI · CÓ DEADLINE
### Team VIVA · lập 28/07/2026 · còn 13 ngày tới hạn nộp 23:59 10/08

> Bản này **thay PHẦN C (phân vai) của `00-KE-HOACH-VONG-2.md`** và **chi tiết hoá PHẦN 5 của `04-KE-HOACH-CAP-NHAT-28-07.md`**.
> Chiến lược, phạm vi cắt T1/T2/T3 và các mốc C0–C3 **giữ nguyên** — file này chỉ trả lời một câu hỏi: *ai làm gì, xong lúc mấy giờ*.

---

## PHẦN 0 — NGÂN SÁCH GIỜ (cơ sở để nói "chia đều")

Năng lực đã thống nhất: ~4h/ngày thường + ~10h/ngày cuối tuần.

| Ngày | 28 | 29 | 30 | 31 | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 10 | **Tổng** |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Giờ/người | 4 | 4 | 4 | 4 | 10 | 10 | 4 | 4 | 5 | 4 | 4 | 10 | 10 | 3 | **80h** |

**Ngân sách đội: 320 person-hour.** Chia đều = 80h/người, gồm:

| Khoản | Giờ | Ghi chú |
|---|---|---|
| Task có tên (bảng PHẦN 2) | 46–49h | Chênh lệch tối đa 3h giữa 4 người |
| Việc chung (standup, kick-off, office hours, tổng duyệt, C2) | ~16h | Giống nhau cho cả 4 |
| **Đệm debug / tích hợp / phát sinh** | **15–18h** | Không lên lịch. Đây là thứ cứu deadline |

> ⚠️ Nếu ai đó cần dùng hết 18h đệm trước 02/08, đó là tín hiệu phải cắt phạm vi — không phải tín hiệu thức đêm.

### Tổng giờ task có tên — kiểm tra cân bằng

| Người | Giờ task | Đường găng | Nặng nhất ở giai đoạn |
|---|---|---|---|
| **Long** | 49h | ✅ | 30/07–02/08 (voice) · 05–07/08 (đo + write-up) |
| **Vĩ** | 47h | ✅ | 28–31/07 (nền tảng) · 04–06/08 (harness + benchmark) |
| **Tùng** | 46h | ✅ | 29–31/07 (Script Node) · 04–05/08 (safety + DTC) |
| **Dương** | 49h | ⬜ | 29–31/07 (HMI) · 04–05/08 (media) · 08–09/08 (video) |

**Ba thay đổi so với kế hoạch cũ, để cân tải:**
1. **DTC (T3) chuyển hẳn về Tùng** — trước đây "Tùng + Vĩ". Cả Script Node `nydus.uds.server` lẫn `DtcClient` ISO-TP đều là embedded, cùng một đầu người sẽ nhanh hơn hai người ghép.
2. **README không còn là việc của riêng Vĩ** — mỗi người viết mục của mình (hạn 06/08), Vĩ chỉ lắp ghép + viết mục build/deploy (07/08).
3. **Toàn bộ video chuyển về Dương** — quay 3' uncut, dựng 5–7', backup. Vĩ đổi lại giữ smoke test + kiểm chứng README trên máy sạch.

---

## PHẦN 1 — QUY TẮC ĐỌC DEADLINE

| Ký hiệu | Nghĩa | Vi phạm thì sao |
|---|---|---|
| 🔴 **CỨNG** | Nằm trên đường găng. Người khác đang chờ. | Báo nhóm **ngay khi biết sẽ trễ**, không đợi tới hạn. Cả đội họp 15' để tái phân bổ. |
| 🟡 **MỀM** | Trễ 1 ngày không chết ai. | Tự dời, ghi vào standup. |
| ⚪ **ĐIỀU KIỆN** | Chỉ làm khi tầng trước đã xanh. | Không được làm sớm để "cho vui". |

**Giờ mặc định của deadline là 23:00 cùng ngày** (trừ khi ghi rõ). Standup 21:30 mỗi tối là lúc báo cáo, 23:00 là lúc code phải nằm trên nhánh `main`.

**Định nghĩa "xong"**: có commit trên `main` + bằng chứng (log/screenshot/CSV) dán vào nhóm chat. Không có bằng chứng = chưa xong.

---

## PHẦN 2 — BẢNG TASK THEO TỪNG NGƯỜI

### 🔵 NGÔ VĂN LONG — Lead · Voice AI & Kiến trúc (49h)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| L1 | Trả lời chị Linh chốt giờ kick-off + gửi 5 câu hỏi văn bản + báo cáo 1 trang C1 làm pre-read | Đã gửi, có xác nhận đã đọc | 2h | 🔴 **28/07 20:00** |
| L2 | `LatencyTrace` — 6 mốc chuẩn + log format `VIVA_TRACE\|` đúng `03-contracts.md` | Vĩ parse được 1 dòng mẫu | 3h | 🔴 **29/07** (Vĩ chờ) |
| L3 | Push-to-talk `AudioRecord` + VAD Silero ONNX, ngưỡng chỉnh được | Nhấn giữ → cắt đúng đoạn nói, wav nghe rõ | 6h | 🔴 **30/07** |
| L4 | `AsrClient` gọi `POST /asr` + timeout/retry + **stub cục bộ** để không chờ Vĩ | Chạy được với stub trước, đổi endpoint sau | 3h | 🔴 **30/07** |
| L5 | Intent Router T0 grammar — 15 intent khoá cứng + unit test | 15/15 câu mẫu ra đúng intent | 6h | 🔴 **31/07** |
| L6 | `TtsSpeaker` + pre-render ~30 câu phản hồi vào `res/raw/` (dự phòng không có `vi-VN`) | Nói được 30 câu dù TTS hệ thống thiếu tiếng Việt | 4h | 🟡 01/08 |
| L7 | Delivery flow in-app — 3 intent (danh sách đơn · chặng kế · xác nhận giao) | 3 intent chạy end-to-end trên HMI | 5h | 🟡 02/08 18:00 |
| L8 | **Chốt kịch bản demo 3' uncut** — tối đa 6 lệnh, có lời thoại, có phương án khi 1 lệnh fail | Văn bản kịch bản + đã chạy thử 1 lần | 2h | 🔴 **02/08** |
| L9 | Tinh chỉnh VAD/ASR/router đạt **p95 < 1500ms** đường edge | Số của Vĩ xác nhận | 5h | 🔴 **05/08** (feature freeze) |
| L10 | Mục README: kiến trúc tổng thể + voice pipeline + 15 intent | Đã push, Vĩ lắp được | 2h | 🟡 06/08 |
| L11 | **Write-up câu chuyện AI** — prompt đã dùng, AI đúng ở đâu, sai ở đâu, MCP-driven testing | Bản hoàn chỉnh nộp được | 5h | 🔴 **07/08** |
| L12 | Dẫn chuyện + điều phối 2 buổi quay (3' uncut và 5–7') | Cả 2 video có tiếng dẫn chuẩn | 4h | 🔴 **08/08** & 09/08 |
| L13 | Checklist nộp bài, push repo cuối, soát `.env`/API key | Đã nộp | 2h | 🔴 **10/08 11:00** |

---

### 🟢 LÊ CÔNG VĨ — Backend · Nền tảng & Đo đạc (47h)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| V1 | Tải artifact DBC (`bcm`, `pwt`) + `vss` → **bảng tên message/signal thật** push repo | Tùng đối chiếu được với property cần dùng | 3h | 🔴 **28/07 22:00** (Tùng chờ) |
| V2 | `GET /api/v1/blueprints/:id/export` backup → **clone** → deploy bản clone lên Device | Bản clone Running, backup JSON trong repo | 2h | 🔴 **28/07 22:00** |
| V3 | `GET /api/v1/deployments/:roomId/nodes` → bảng node CCU + tên pin CAN + pin VHAL | Có bảng, khỏi hỏi mentor | 1h | 🔴 **28/07 22:00** |
| V4 | Repo Git + `.gitignore` chặn key + CI build APK + mời 3 người | Cả 4 push được | 2h | 🔴 **29/07** |
| V5 | `vm_tunnel_open` + `adb connect` + hướng dẫn dev loop 1 trang cho 3 người còn lại | 3 người tự cài APK lên Device được | 2h | 🔴 **29/07** |
| V6 | Dockerfile `viva-asr` (whisper-tiny INT8, **swap được model** phòng khi có GPU) + push Zot `registry.carsky.io` | Cluster pull được image | 6h | 🔴 **29/07** |
| V7 | Thêm Container Node `viva-asr` vào blueprint clone, guest gọi được `POST /asr` | Long đổi từ stub sang endpoint thật, trả text tiếng Việt | 4h | 🔴 **30/07** (Long chờ) |
| V8 | **Harness v1** — parse `VIVA_TRACE\|` qua `adb_shell` → CSV 6 chặng | Ra 1 file CSV có timestamp đủ 6 chặng | 5h | 🔴 **31/07** |
| V9 | Benchmark ≥20 utterance → **p50/p95 đầu tiên** cho C2 | Có bảng số + biểu đồ thô | 4h | 🔴 **02/08 20:00** |
| V10 | **Harness v2** — regression qua MCP: `send_signals` set trạng thái xe → `screenshot` → `find_text` → PASS/FAIL | 20+ câu chạy tự động, có ảnh làm bằng chứng | 6h | 🟡 04/08 |
| V11 | Benchmark đầy đủ: 20 câu × 3 mức nhiễu, p50/p95 + biểu đồ | Số cuối dùng cho slide và README | 5h | 🔴 **06/08** |
| V12 | Lắp ghép README + viết mục build/deploy lên CarSky + nguồn open-source | README đọc là làm theo được | 4h | 🔴 **07/08** |
| V13 | Smoke test Device sạch + **nhờ người ngoài đội làm theo README** | Người ngoài build & chạy được kịch bản chính | 3h | 🔴 **09/08 20:00** |

---

### 🟠 LÊ ĐỨC TÙNG — Embedded · VHAL, Safety & DTC (46h)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| T1 | Đọc DBC của Vĩ → **bảng ánh xạ property ↔ CAN signal** (thay giả định `HvacCommand.Driver_Temperature`) | Bảng có tên thật, không còn dòng nào "suy ra" | 3h | 🔴 **28/07 23:00** |
| T2 | **Script Node Luau VHAL ↔ CAN hai chiều** + deploy + xác minh bằng Signal Watch | Set property từ `adb shell` → Signal Watch đổi; đổi CAN → property đổi | 8h | 🔴 **30/07** ⭐ đường găng dài nhất |
| T3 | `VhalRepository` — wrapper `CarPropertyManager` + callback real-time (Dương chờ) | Dương subscribe được, HMI đổi theo | 5h | 🔴 **31/07** |
| T4 | `SafetyGuard` G1 — khoá theo tốc độ/gear (luật 1–3 của `03-contracts.md`) | "Mở cửa" khi `Vehicle.Speed=60` → DENY có lý do | 4h | 🔴 **31/07** |
| T5 | `SafetyGuard` G2 + đủ 7 luật G1–G3 | 7/7 luật có test | 4h | 🟡 02/08 18:00 |
| T6 | Climate skill đi **đủ 6 chặng**, đúng `areaId` từng ghế | Nói "hạ xuống 22 độ" → 6 chặng đổi cùng lúc | 4h | 🔴 **02/08 18:00** ⭐ |
| T7 | **Safety scenario pack** ≥6 kịch bản pass/fail chạy tự động | Chạy 1 lệnh ra bảng pass/fail | 5h | 🟡 04/08 |
| T8 | **T3 — DTC**: Script Node `nydus.uds.server` + `DtcClient` ISO-TP `19 02 FF` + phân nhóm P/C/B/U | Nhận PDU bắt đầu `0x59`, parse ra danh sách DTC thật | 8h | ⚪ **05/08** — *chỉ khi T1 & T2 đã xanh* |
| T9 | Mục README: **bảng Vehicle Property + CAN signal đã dùng** | Bảng đầy đủ, khớp code | 2h | 🟡 06/08 |
| T10 | Chạy safety pack + xuất báo cáo pass/fail cho slide | Có báo cáo | 3h | 🔴 **07/08** |

> T2 là task đơn lẻ dài nhất của cả đội (8h) và **cả 3 người còn lại đều chờ nó**. Tùng bắt đầu ngay 29/07 sáng, không để dồn sang 30/07.

---

### 🟣 VIỆT DƯƠNG — Android · HMI, Media & Video (49h)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| D1 | App shell AAOS + `car-ui-lib`, cài chạy trên Device | 3 người còn lại có chỗ cắm code vào | 4h | 🔴 **29/07** (cả đội chờ) |
| D2 | HMI 3 vùng Compose — trạng thái xe · hội thoại · skill hiện hành | Nhìn thấy đủ 3 vùng, không crash | 6h | 🔴 **31/07** |
| D3 | Phản chiếu HVAC **real-time** từ callback của T3 | Đổi giá trị ở GPIO Panel → HMI tự đổi, không cần bấm | 4h | 🔴 **31/07** ⭐ khung hình chốt của video |
| D4 | Build `usb.img` FAT32 (8 mp3 + album art) → Artifact category USB → mount `/sdcard/Music/usb_1` | `ls` thấy file trong VM | 4h | 🟡 01/08 20:00 |
| D5 | `MediaSourceProvider` interface + `UsbMediaProvider` + `LocalStorageProvider` (2 implementation) | Đổi provider không sửa UI | 6h | 🟡 04/08 |
| D6 | Album-art `LruCache` + disk cache, **không ANR** khi load | Cuộn 50 bài không giật, không ANR trong logcat | 4h | 🟡 05/08 |
| D7 | `MediaBrowserService`/`MediaSession` + HMI media | Phát/dừng/next chạy, điều khiển được bằng giọng nói | 5h | 🟡 05/08 |
| D8 | Đóng gói APK release + smoke test cài sạch | Cài từ file APK trên máy khác chạy được | 2h | 🔴 **05/08** (feature freeze) |
| D9 | Sơ đồ kiến trúc + extension points cho tài liệu | Ảnh dùng được cho cả README và slide | 3h | 🟡 07/08 |
| D10 | **Quay video 3' KHÔNG CẮT GHÉP** bằng Recorder Part — chạy sạch 3 lần liên tiếp mới lấy | File `.mp4` đã tải về | 3h | 🔴 **08/08** (C3) |
| D11 | Dựng **video 5–7'** + overlay 6 khung chứng minh không mock | Bản nộp chính thức | 6h | 🔴 **09/08 20:00** |
| D12 | **Video backup dự phòng** (guideline CarSky bắt buộc) | Có file riêng, độc lập với Room | 2h | 🔴 **09/08 20:00** |

---

## PHẦN 3 — VIỆC CHUNG (cả 4 người, ~16h mỗi người)

| Việc | Thời điểm | Giờ |
|---|---|---|
| Standup 15' | 21:30 mỗi tối, 28/07 → 09/08 | 3.5h |
| Kick-off mentor | 30/07 19:00 (đề xuất) | 1h |
| **Tổng duyệt demo 10' cho C2** | 02/08 tối | 3h |
| Nộp C2 | 03/08 | 2h |
| Office hours mentor | 04/08 & 06/08 19:00 | 2h |
| **Tổng duyệt cài sạch → chạy full kịch bản** ×2 lần | 08/08 & 09/08 tối | 5h |

**Nội dung standup — đúng 3 câu, không hơn:** hôm qua xong gì · hôm nay làm gì · **đang bị ai chặn**.

---

## PHẦN 4 — AI CHỜ AI (dùng khi bị tắc)

```
V1 DBC (28/07 22:00)
   └─► T1 bảng ánh xạ (28/07 23:00)
          └─► T2 Script Node Luau (30/07)  ◄── ĐƯỜNG GĂNG DÀI NHẤT
                 ├─► T3 VhalRepository (31/07)
                 │      └─► D3 HMI real-time (31/07)
                 └─► T6 Climate 6 chặng (02/08)

D1 App shell (29/07) ─► mọi code in-app của Long, Tùng, Dương

L2 LatencyTrace (29/07) ─► V8 Harness v1 (31/07) ─► V9 số cho C2 (02/08)

V6 image (29/07) ─► V7 node ASR chạy (30/07) ─► L4 đổi từ stub sang thật (30/07)
```

**Bốn điểm chặn duy nhất cần canh:**

| Chặn | Người giao | Người chờ | Hạn | Nếu trễ |
|---|---|---|---|---|
| Bảng signal DBC | Vĩ | Tùng | 28/07 22:00 | Tùng code theo giả định cũ, đánh dấu `// TODO-DBC`, sửa sau khi có |
| App shell | Dương | cả 3 | 29/07 | Mỗi người chạy module riêng bằng unit test, ghép sau |
| Script Node Luau | Tùng | Dương, cả demo | 30/07 | **Cả 4 dồn vào đây 31/07.** Không có nó thì không có xương sống |
| Node ASR chạy trong Room | Vĩ | Long | 30/07 | Long dùng stub cục bộ — đã thiết kế sẵn ở L4, không tắc |

> Quy tắc chung: **bị chặn > 4h thì đưa lên nhóm ngay**, không tự xoay qua đêm.

---

## PHẦN 5 — BA MỐC KIỂM TRA CÂN BẰNG (giữ cho "chia đều" không vỡ)

Chia đều lúc lập kế hoạch là dễ. Giữ đều khi chạy mới khó — nên đặt 3 điểm cân lại:

### ⚖️ Mốc cân 1 — **31/07 21:30**, sau xương sống

Câu hỏi: *nói "Hạ điều hòa xuống 22 độ" → 6 chặng đổi thật → HMI cập nhật → log đủ 6 mốc. ĐƯỢC hay CHƯA?*

| Kết quả | Hành động |
|---|---|
| ✅ ĐƯỢC | Giữ nguyên bảng PHẦN 2. Dương bắt đầu T2-media từ 01/08 |
| ❌ CHƯA | **Hoãn vô điều kiện D4–D7 và T8.** Dương chuyển sang hỗ trợ Tùng debug Script Node, Long tạm dừng L7 delivery. Cả 4 dồn tới khi xanh |

### ⚖️ Mốc cân 2 — **03/08 sau C2**

Ai còn dư > 6h so với lịch thì **nhận việc từ đường găng của người khác**, không tự mở phạm vi mới. Thứ tự nhận:
1. Giúp Tùng T7 safety pack
2. Giúp Vĩ V10 harness v2
3. Viết trước mục README của mình (hạn 06/08)
4. *(Chỉ khi cả 3 việc trên xong)* — mở T8 DTC

### ⚖️ Mốc cân 3 — **05/08 23:59 — FEATURE FREEZE**

Sau mốc này **không ai viết tính năng mới**, kể cả khi đang rảnh. Chỉ còn 4 loại việc:
sửa lỗi · đo đạc · tài liệu · quay video. Ai vi phạm là rủi ro cho cả đội, không phải nỗ lực thêm.

---

## PHẦN 6 — LỊCH DEADLINE TỔNG HỢP

| Ngày | Long | Vĩ | Tùng | Dương |
|---|---|---|---|---|
| **28/07** T3 | 🔴 L1 mentor 20:00 | 🔴 V1 DBC · V2 clone · V3 nodes 22:00 | 🔴 T1 bảng ánh xạ 23:00 | — |
| **29/07** T4 | 🔴 L2 LatencyTrace | 🔴 V4 repo · V5 dev loop · V6 image ASR | *(bắt đầu T2)* | 🔴 D1 App shell |
| **30/07** T5 | 🔴 L3 VAD · L4 AsrClient | 🔴 V7 node ASR chạy | 🔴 **T2 Script Node** | *(HMI)* |
| **31/07** T6 | 🔴 L5 Intent T0 | 🔴 V8 Harness v1 | 🔴 T3 VhalRepo · T4 Guard G1 | 🔴 D2 HMI · D3 real-time |
| | ⚖️ **MỐC CÂN 1 — 21:30 — xương sống được hay chưa** ||||
| **01/08** T7 | 🟡 L6 TTS | *(hỗ trợ tích hợp)* | *(hỗ trợ tích hợp)* | 🟡 D4 usb.img 20:00 |
| **02/08** CN | 🟡 L7 delivery 18:00 · 🔴 L8 kịch bản 3' | 🔴 V9 p50/p95 20:00 | 🟡 T5 Guard G2 · 🔴 T6 Climate 6 chặng 18:00 | *(media)* |
| | 🔴 **Tối 02/08 — tổng duyệt demo 10' cho C2** ||||
| **03/08** T2 | 🟢 **NỘP C2** ||| |
| | ⚖️ **MỐC CÂN 2 — sau C2** ||||
| **04/08** T3 | *(tối ưu latency)* | 🟡 V10 Harness v2 | 🟡 T7 safety pack | 🟡 D5 MediaSourceProvider |
| **05/08** T4 | 🔴 L9 p95<1.5s | *(hỗ trợ)* | ⚪ T8 DTC | 🟡 D6 cache · D7 MediaSession · 🔴 D8 APK |
| | 🚫 **23:59 — FEATURE FREEZE** ||||
| **06/08** T5 | 🟡 L10 README | 🔴 V11 benchmark đầy đủ | 🟡 T9 bảng property | *(video prep)* |
| | 🔴 **19:00 — office hours cuối cùng còn kịp sửa** ||||
| **07/08** T6 | 🔴 L11 write-up AI | 🔴 V12 README hoàn chỉnh | 🔴 T10 báo cáo safety | 🟡 D9 sơ đồ kiến trúc |
| **08/08** T7 | 🔴 L12 dẫn chuyện | *(hỗ trợ quay)* | *(hỗ trợ quay)* | 🔴 **D10 video 3' UNCUT** |
| | ⚫ **C3 — code freeze · slide pitch · test chạy được** ||||
| **09/08** CN | 🔴 L12 dẫn chuyện bản dựng | 🔴 V13 smoke + người ngoài thử README 20:00 | *(soát tài liệu)* | 🔴 D11 video 5–7' · D12 backup 20:00 |
| | 🔴 **Tối 09/08 — tổng duyệt lần cuối, cài APK sạch, logcat không crash** ||||
| **10/08** T2 | 🔴 **L13 nộp trước 12:00** ||| |

---

## PHẦN 7 — VIỆC CỦA HÔM NAY (28/07)

| Người | Việc | Hạn |
|---|---|---|
| **Long** | Gửi `05-tra-loi-kickoff.md` chốt giờ kick-off + 5 câu hỏi + báo cáo 1 trang C1 | 20:00 |
| **Long** | Đăng file này vào nhóm, **mỗi người xác nhận bằng chữ**: đã đọc phần của mình + đồng ý danh sách cắt ở PHẦN 4 của `04-KE-HOACH-CAP-NHAT-28-07.md` | 21:30 |
| **Vĩ** | V1 tải DBC + VSS → bảng signal · V2 backup + clone + deploy · V3 tra node/pin | 22:00 |
| **Tùng** | T1 đọc DBC → bảng ánh xạ property ↔ signal, thay hết giả định | 23:00 |
| **Dương** | Dựng sẵn project Android + `car-ui-lib` để 29/07 chỉ còn cắm vào Device | 23:00 |
| **Cả 4** | Standup đầu tiên | 21:30 |

> Việc quan trọng nhất hôm nay không phải code. Là **xoá dòng giả định `HvacCommand.Driver_Temperature`** — Tùng không được viết Script Node trước khi có tên signal thật từ DBC.
