# PHÂN CÔNG VÒNG 2 — 4 NGƯỜI · CÓ DEADLINE
### Team VIVA · bản v2 ngày 28/07/2026 · còn 13 ngày tới hạn nộp 23:59 10/08

> **v2 sửa gì so với v1:** đối chiếu lại với **proposal đã nộp (PPTX slide 3)**, `03-contracts.md`,
> guideline CDC và bảng tiêu chí từng challenge trong webinar 25/06. Phát hiện **3 lỗi phân vai**
> và **9 hạng mục thiếu**. Chi tiết ở PHẦN 8.
>
> Bản này thay PHẦN C của `00-KE-HOACH-VONG-2.md` và chi tiết hoá PHẦN 5 của `04-KE-HOACH-CAP-NHAT-28-07.md`.
> Chiến lược và phạm vi cắt T1/T2/T3 giữ nguyên.

---

## PHẦN 0 — VAI TRÒ ĐÃ CAM KẾT VỚI BTC (nguồn: proposal slide 3, không được đổi tuỳ tiện)

Barem Vòng 1 chấm **"Năng lực đội 15đ — sự hợp lý trong phân vai"**. BGK Vòng 2 chính là mentor
đã đọc slide này. Phân công Vòng 2 phải khớp, lệch thì phải giải thích được.

| Người | Vai trò đã khai | Phụ trách đã khai |
|---|---|---|
| **Ngô Văn Long** — đội trưởng | AI Engineer · Voice AI & Kiến trúc | voice pipeline · intent router · kiến trúc tổng |
| **Lê Công Vĩ** | Senior Backend · Agent & DevOps | intent router · LLM function-calling · **delivery simulator** · **benchmark harness** |
| **Lê Đức Tùng** | Embedded/System · VHAL & DTC | CarPropertyManager ↔ VHAL (**HVAC, DOOR**) · DTC/UDS simulator |
| **Việt Dương** | Fullstack/Android · HMI & Media | HMI cockpit AAOS · MediaBrowserService/MediaSession · đóng gói APK |

> Câu cuối slide 3, nguyên văn: *"Kịch bản demo, video & write-up: **cả đội cùng thực hiện**, đội trưởng
> điều phối — **mỗi thành viên chịu trách nhiệm demo phần mình phụ trách**."*
> → Video **không phải việc của một người**. Bản v1 dồn 11h video cho Dương là sai cam kết. Đã sửa.

---

## PHẦN 1 — NGÂN SÁCH GIỜ

| Ngày | 28 | 29 | 30 | 31 | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 10 | **Tổng** |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Giờ/người | 4 | 4 | 4 | 4 | 10 | 10 | 4 | 4 | 5 | 4 | 4 | 10 | 10 | 3 | **80h** |

**320 person-hour.** Phân bổ:

| Người | Task bắt buộc | Task điều kiện (T3) | Việc chung | Đệm |
|---|---|---|---|---|
| **Long** | 50h | — | 16h | 14h |
| **Vĩ** | 50h | — | 16h | 14h |
| **Tùng** | 47h | +9h (DTC) | 16h | 17h / 8h |
| **Dương** | 51h | — | 16h | 13h |

Chênh lệch tối đa 4h. Tùng nhận ít giờ bắt buộc hơn vì giữ **task đơn lẻ dài nhất (T2, 8h)** nằm trên
đường găng và **toàn bộ tầng T3 điều kiện (9h)** — nếu T3 được bật, Tùng là người kín nhất đội.

---

## PHẦN 2 — QUY TẮC ĐỌC DEADLINE

| Ký hiệu | Nghĩa | Vi phạm thì sao |
|---|---|---|
| 🔴 **CỨNG** | Trên đường găng, người khác đang chờ | Báo nhóm **ngay khi biết sẽ trễ**, không đợi tới hạn |
| 🟡 **MỀM** | Trễ 1 ngày không chết ai | Tự dời, ghi vào standup |
| ⚪ **ĐIỀU KIỆN** | Chỉ làm khi tầng trước xanh | Không được làm sớm "cho vui" |

Giờ mặc định **23:00** cùng ngày. Standup 21:30 là lúc báo cáo, 23:00 là lúc code nằm trên `main`.
**"Xong" = commit trên `main` + bằng chứng (log/screenshot/CSV) dán vào nhóm.** Không bằng chứng = chưa xong.

---

## PHẦN 3 — BẢNG TASK THEO TỪNG NGƯỜI

### 🔵 NGÔ VĂN LONG — Voice AI & Kiến trúc (50h)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| L1 | Gửi kick-off + 5 câu hỏi + báo cáo 1 trang C1. **Thêm lại câu xin `Hackathon_IVI_CDC_2026.md`** (xem PHẦN 8 mục ⑩) | Đã gửi, có xác nhận | 2h | 🔴 **28/07 20:00** |
| L2 | `LatencyTrace` 6 mốc + log format `VIVA_TRACE\|` đúng `03-contracts.md` | Vĩ parse được 1 dòng mẫu | 3h | 🔴 **29/07** (Vĩ chờ) |
| L3 | Push-to-talk `AudioRecord` + VAD Silero ONNX | Nhấn giữ → cắt đúng đoạn nói | 6h | 🔴 **30/07** |
| L4 | `AsrClient` + timeout/retry + **stub cục bộ** để không chờ Vĩ | Chạy với stub trước, đổi endpoint sau | 3h | 🔴 **30/07** |
| L5 | Intent Router T0 grammar — **đủ 15 intent** của `03-contracts.md` + unit test | 15/15 câu mẫu ra đúng intent | 6h | 🔴 **31/07** |
| L6 | `TtsSpeaker` + pre-render ~30 câu vào `res/raw/` | Nói được dù thiếu `vi-VN` | 4h | 🟡 01/08 |
| L7 | 🆕 **Audio focus** — `CarAudioManager`: duck nhạc khi TTS nói, trả focus sau | Nhạc đang phát + ra lệnh + TTS trả lời, không chồng tiếng | 3h | 🔴 **02/08** |
| L8 | **Chốt kịch bản demo 3' uncut** — ≤6 lệnh, có lời thoại, có đường thoát khi 1 lệnh fail | Văn bản + đã chạy thử 1 lần | 2h | 🔴 **02/08** |
| L9 | Tinh chỉnh đạt **p95 < 1500ms** đường edge | Số của Vĩ xác nhận | 5h | 🔴 **05/08** |
| L10 | 🆕 **Chốt trục so sánh thay "edge vs hybrid"** đã cam kết ở slide 9/11 (xem PHẦN 8 mục ⑫) | Có quyết định + 1 đoạn giải thích trong write-up | 2h | 🔴 **05/08** |
| L11 | README: kiến trúc + voice pipeline + **extension point thêm intent mới không sửa core** | Người đọc thêm được 1 intent theo hướng dẫn | 3h | 🟡 06/08 |
| L12 | **Write-up câu chuyện AI** + đoạn claim cross-vertical (PHẦN 8 mục ⑭) | Bản hoàn chỉnh nộp được | 5h | 🔴 **07/08** |
| L13 | Điều phối 2 video + **tự quay/dẫn phần voice của mình** | Cả 2 video có phần voice chuẩn | 4h | 🔴 **08/08** & 09/08 |
| L14 | Checklist nộp, push cuối, soát `.env`/API key | Đã nộp | 2h | 🔴 **10/08 11:00** |

---

### 🟢 LÊ CÔNG VĨ — Agent & DevOps (50h)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| V1 | Tải DBC (`bcm`,`pwt`) + `vss` → **bảng message/signal thật** push repo | Tùng đối chiếu được | 3h | 🔴 **28/07 22:00** (Tùng chờ) |
| V2 | Export blueprint backup → **clone** → deploy bản clone | Clone Running, backup JSON trong repo | 2h | 🔴 **28/07 22:00** |
| V3 | `GET /deployments/:roomId/nodes` → bảng node CCU + pin CAN + pin VHAL | Có bảng | 1h | 🔴 **28/07 22:00** |
| V4 | Repo Git + `.gitignore` chặn key + CI build APK | Cả 4 push được | 2h | 🔴 **29/07** |
| V5 | `vm_tunnel_open` + `adb connect` + hướng dẫn dev loop 1 trang | 3 người tự cài APK được | 2h | 🔴 **29/07** |
| V6 | Dockerfile `viva-asr` (whisper-tiny INT8, **swap được model**) + push Zot | Cluster pull được image | 5h | 🔴 **29/07** |
| V7 | Container Node `viva-asr` vào blueprint clone, guest gọi được `POST /asr` | Long đổi từ stub sang thật, ra text tiếng Việt | 4h | 🔴 **30/07** (Long chờ) |
| V8 | **Harness v1** — parse `VIVA_TRACE\|` qua `adb_shell` → CSV 6 chặng | 1 file CSV đủ 6 chặng | 5h | 🔴 **31/07** |
| V9 | ✅ **`DeliverySkill` — 3 intent** `delivery_next_stop` / `order_status` / `confirm`<br>*(trả về đúng owner theo proposal + `03-contracts.md` §5 — v1 giao nhầm cho Long)* | 3 intent end-to-end trên HMI | 5h | 🟡 02/08 18:00 |
| V10 | Benchmark ≥20 utterance → **p50/p95 đầu tiên** cho C2 | Bảng số + biểu đồ thô | 4h | 🔴 **02/08 20:00** |
| V11 | **Harness v2** — regression qua MCP: `send_signals` → `screenshot` → `find_text` → PASS/FAIL | 20+ câu tự động, có ảnh bằng chứng | 6h | 🟡 04/08 |
| V12 | Benchmark đầy đủ 20 câu × 3 mức nhiễu, p50/p95 + biểu đồ | Số cuối dùng cho slide + README | 5h | 🔴 **06/08** |
| V13 | Lắp ghép README + mục build/deploy CarSky + **ghi nguồn thư viện open-source** | README đọc là làm theo được | 3h | 🔴 **07/08** |
| V14 | Smoke test Device sạch + **nhờ người ngoài đội làm theo README** | Người ngoài build & chạy được | 2h | 🔴 **09/08 20:00** |
| V15 | Tự quay/dẫn **phần delivery + benchmark** của mình trong 2 video | Có trong cả 2 bản | 1h | 🔴 **08/08** & 09/08 |

---

### 🟠 LÊ ĐỨC TÙNG — VHAL, Safety & DTC (47h + 9h điều kiện)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| T1 | Đọc DBC của Vĩ → **bảng ánh xạ property ↔ CAN signal** | Không còn dòng nào "suy ra từ walkthrough" | 3h | 🔴 **28/07 23:00** |
| T2 | **Script Node Luau VHAL ↔ CAN hai chiều** + deploy + verify Signal Watch | Set property → Signal Watch đổi; đổi CAN → property đổi | 8h | 🔴 **30/07** ⭐ đường găng dài nhất |
| T3 | `VhalRepository` — wrapper `CarPropertyManager` + callback real-time | Dương subscribe được | 5h | 🔴 **31/07** |
| T4 | 🆕 **`BodySkill` — `door_lock` đúng `areaId`**<br>*(proposal giao Tùng "VHAL — HVAC, **DOOR**"; `03-contracts.md` §5 BodySkill = Tùng; slide 11 cam kết "≥5 lệnh car control: **cửa**, âm lượng, media, điều hòa". v1 thiếu hẳn)* | "Khóa cửa" / "mở cửa" đổi property thật | 3h | 🔴 **31/07** |
| T5 | `SafetyGuard` G1 + 🆕 **đọc `CarUxRestrictionsManager` vào `VehicleState`** (guideline CDC mục 5 & 8) | "Mở cửa" khi `Speed=60` → DENY có lý do; test bằng widget **Road Simulator** | 5h | 🔴 **31/07** |
| T6 | `SafetyGuard` G2 + đủ 7 luật G1–G3 | 7/7 luật có test case | 4h | 🟡 02/08 18:00 |
| T7 | Climate skill **đủ 6 chặng**, đúng `areaId` + 🆕 quyết định `hvac_mode` / ghế sưởi (PHẦN 8 mục ⑦) | "Hạ xuống 22 độ" → 6 chặng đổi cùng lúc | 5h | 🔴 **02/08 18:00** ⭐ |
| T8 | 🆕 **Bộ test VHAL chạy không cần ECU** — tiêu chí *Testability* của đề #2 | `./gradlew test` xanh, không cần Room | 3h | 🟡 04/08 |
| T9 | **Safety scenario pack** ≥6 kịch bản pass/fail tự động | 1 lệnh ra bảng pass/fail | 5h | 🟡 04/08 |
| T10 | ⚪ **T3 — DTC**: Script Node `nydus.uds.server` + `DtcClient` ISO-TP `19 02 FF` + phân nhóm P/C/B/U + active/pending/stored + **tần suất · xu hướng · TƯƠNG QUAN** (PHẦN 8 mục ⑨) | Nhận PDU `0x59`, parse ra DTC thật + `/dtc/analysis` đủ 3 trục | 9h | ⚪ **05/08** — *chỉ khi T1 & T2 xanh* |
| T11 | README: **bảng Vehicle Property + CAN signal đã dùng** (guideline CDC bắt buộc) | Bảng khớp code | 2h | 🟡 06/08 |
| T12 | Chạy safety pack + báo cáo pass/fail cho slide | Có báo cáo | 3h | 🔴 **07/08** |
| T13 | Tự quay/dẫn **phần climate + safety** của mình | Có trong cả 2 video | 1h | 🔴 **08/08** & 09/08 |

---

### 🟣 VIỆT DƯƠNG — HMI & Media (51h)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| D1 | App shell AAOS + `car-ui-lib` chạy trên Device | 3 người còn lại có chỗ cắm code | 4h | 🔴 **29/07** (cả đội chờ) |
| D2 | HMI 3 vùng Compose + 🆕 **Driver Distraction: nút lớn, tương phản rõ** (guideline CDC mục 5; tiêu chí *UX* của đề #1) | Đủ 3 vùng, không crash, chạm được khi xe chạy | 6h | 🔴 **31/07** |
| D3 | Phản chiếu HVAC **real-time** từ callback của T3 | Đổi ở GPIO Panel → HMI tự đổi | 4h | 🔴 **31/07** ⭐ khung hình chốt của video |
| D4 | Build `usb.img` FAT32 (8 mp3 + album art) → Artifact USB → mount verify | `ls` thấy file trong VM | 3h | 🟡 01/08 20:00 |
| D5 | `MediaSourceProvider` + `UsbMediaProvider` + `LocalMediaProvider` — tiêu chí *Extensibility* đề #1 | Đổi provider không sửa UI | 6h | 🟡 04/08 |
| D6 | Album-art `LruCache` + disk cache, **không ANR** — tiêu chí *Hiệu năng* đề #1 | Cuộn 50 bài không giật, logcat sạch | 4h | 🟡 05/08 |
| D7 | `MediaBrowserService`/`MediaSession` + 🆕 **shuffle · repeat · seek** (chức năng chính đề #1) | Phát/dừng/next/shuffle/repeat/tua chạy, điều khiển được bằng giọng | 6h | 🟡 05/08 |
| D8 | 🆕 **`volume_set` / `volume_adjust` + audio zone** qua `CarAudioManager`<br>*(`03-contracts.md` §5 giao MediaSkill; slide 11 cam kết "âm lượng". v1 thiếu hẳn)* | "Tăng âm lượng" đổi volume thật | 3h | 🔴 **02/08** |
| D9 | 🆕 HMI tự đơn giản hoá theo `CarUxRestrictions` (guideline CDC mục 5) | Xe chạy → UI giảm mật độ thông tin | 2h | 🟡 05/08 |
| D10 | Đóng gói APK release + smoke cài sạch | Cài từ APK trên máy khác chạy được | 2h | 🔴 **05/08** |
| D11 | Sơ đồ kiến trúc + extension points | Dùng được cho README và slide | 3h | 🟡 07/08 |
| D12 | Dựng máy quay **video 3' uncut** (Recorder Part) — chạy sạch 3 lần liên tiếp mới lấy | File `.mp4` đã tải về | 2h | 🔴 **08/08** (C3) |
| D13 | Dựng **video 5–7'** + overlay 6 khung chứng minh không mock | Bản nộp chính thức | 5h | 🔴 **09/08 20:00** |
| D14 | **Video backup dự phòng**, lưu ở mục Videos (guideline CDC mục 7) | File riêng, độc lập với Room | 1h | 🔴 **09/08 20:00** |

---

## PHẦN 4 — VIỆC CHUNG (~16h mỗi người)

| Việc | Thời điểm | Giờ |
|---|---|---|
| Standup 15' — 3 câu: hôm qua xong gì · hôm nay làm gì · **đang bị ai chặn** | 21:30 mỗi tối 28/07→09/08 | 3.5h |
| Kick-off mentor | 30/07 19:00 (đề xuất) | 1h |
| Tổng duyệt demo 10' cho C2 | 02/08 tối | 3h |
| Nộp C2 | 03/08 | 2h |
| Office hours mentor | 04/08 & 06/08 19:00 | 2h |
| Tổng duyệt cài sạch → chạy full kịch bản ×2 | 08/08 & 09/08 tối | 5h |

---

## PHẦN 5 — AI CHỜ AI

```
V1 DBC (28/07 22:00)
   └─► T1 bảng ánh xạ (28/07 23:00)
          └─► T2 Script Node Luau (30/07)  ◄── ĐƯỜNG GĂNG DÀI NHẤT
                 ├─► T3 VhalRepository (31/07) ─► D3 HMI real-time (31/07)
                 ├─► T4 BodySkill door_lock (31/07) ─► T5 SafetyGuard G1 demo được
                 └─► T7 Climate 6 chặng (02/08)

D1 App shell (29/07) ─► mọi code in-app của Long, Tùng, Dương
L2 LatencyTrace (29/07) ─► V8 Harness v1 (31/07) ─► V10 số cho C2 (02/08)
V6 image (29/07) ─► V7 node ASR (30/07) ─► L4 đổi stub sang thật (30/07)
D7 MediaSession + D8 volume ─► L7 audio focus (02/08)
```

| Chặn | Giao | Chờ | Hạn | Nếu trễ |
|---|---|---|---|---|
| Bảng signal DBC | Vĩ | Tùng | 28/07 22:00 | Tùng code theo giả định cũ, đánh dấu `// TODO-DBC` |
| App shell | Dương | cả 3 | 29/07 | Mỗi người chạy module bằng unit test, ghép sau |
| Script Node Luau | Tùng | Dương + cả demo | 30/07 | **Cả 4 dồn vào đây 31/07** |
| Node ASR trong Room | Vĩ | Long | 30/07 | Long dùng stub — đã thiết kế sẵn ở L4 |

---

## PHẦN 6 — BA MỐC CÂN BẰNG

**⚖️ 31/07 21:30 — xương sống được hay chưa?**
*"Hạ điều hòa xuống 22 độ" → 6 chặng đổi thật → HMI cập nhật → log đủ 6 mốc.*
✅ → giữ nguyên bảng PHẦN 3. ❌ → **hoãn vô điều kiện D5–D9 và T10**; Dương sang hỗ trợ Tùng, Long dừng L7.

**⚖️ 03/08 sau C2** — ai dư > 6h nhận việc từ đường găng, **không tự mở phạm vi mới**. Thứ tự nhận:
① T9 safety pack → ② V11 harness v2 → ③ viết trước mục README của mình → ④ *chỉ khi ① ② ③ xong* mở T10 DTC.

**⚖️ 05/08 23:59 — FEATURE FREEZE.** Sau mốc này chỉ còn 4 loại việc: sửa lỗi · đo đạc · tài liệu · quay video.

---

## PHẦN 7 — LỊCH DEADLINE TỔNG HỢP

| Ngày | Long | Vĩ | Tùng | Dương |
|---|---|---|---|---|
| **28/07** | 🔴 L1 mentor 20:00 | 🔴 V1 DBC · V2 clone · V3 nodes 22:00 | 🔴 T1 bảng ánh xạ 23:00 | chuẩn bị project Android |
| **29/07** | 🔴 L2 LatencyTrace | 🔴 V4 repo · V5 dev loop · V6 image | *(bắt đầu T2)* | 🔴 D1 App shell |
| **30/07** | 🔴 L3 VAD · L4 AsrClient | 🔴 V7 node ASR chạy | 🔴 **T2 Script Node** | *(HMI)* |
| **31/07** | 🔴 L5 Intent 15 intent | 🔴 V8 Harness v1 | 🔴 T3 VhalRepo · T4 door_lock · T5 Guard G1 | 🔴 D2 HMI · D3 real-time |
| | ⚖️ **MỐC CÂN 1 — 21:30** ||||
| **01/08** | 🟡 L6 TTS | *(hỗ trợ tích hợp)* | *(hỗ trợ tích hợp)* | 🟡 D4 usb.img 20:00 |
| **02/08** | 🔴 L7 audio focus · L8 kịch bản 3' | 🟡 V9 Delivery 18:00 · 🔴 V10 p50/p95 20:00 | 🟡 T6 Guard G2 · 🔴 T7 Climate 6 chặng 18:00 | 🔴 D8 volume |
| | 🔴 **Tối 02/08 — tổng duyệt demo 10' cho C2** ||||
| **03/08** | 🟢 **NỘP C2** — ⚖️ MỐC CÂN 2 ||||
| **04/08** | *(tối ưu latency)* | 🟡 V11 Harness v2 | 🟡 T8 test VHAL · T9 safety pack | 🟡 D5 MediaSourceProvider |
| **05/08** | 🔴 L9 p95<1.5s · L10 trục benchmark | *(hỗ trợ)* | ⚪ T10 DTC | 🟡 D6 cache · D7 MediaSession · D9 UxRestrictions · 🔴 D10 APK |
| | 🚫 **23:59 FEATURE FREEZE** ||||
| **06/08** | 🟡 L11 README | 🔴 V12 benchmark đầy đủ | 🟡 T11 bảng property | *(chuẩn bị quay)* |
| | 🔴 **19:00 office hours cuối cùng còn kịp sửa** ||||
| **07/08** | 🔴 L12 write-up AI | 🔴 V13 README hoàn chỉnh | 🔴 T12 báo cáo safety | 🟡 D11 sơ đồ kiến trúc |
| **08/08** | 🔴 L13 phần voice | 🔴 V15 phần delivery | 🔴 T13 phần climate+safety | 🔴 **D12 quay 3' UNCUT** |
| | ⚫ **C3 — code freeze · slide pitch · test chạy được** ||||
| **09/08** | 🔴 L13 bản dựng | 🔴 V14 smoke + người ngoài thử README 20:00 | *(soát tài liệu)* | 🔴 D13 video 5–7' · D14 backup 20:00 |
| | 🔴 **Tối 09/08 — tổng duyệt cuối, cài APK sạch, logcat không crash** ||||
| **10/08** | 🔴 **L14 nộp trước 12:00** ||| |

---

## PHẦN 8 — KẾT QUẢ RÀ SOÁT (vì sao có bản v2)

### A. Ba lỗi phân vai — đã sửa

| # | Lỗi ở v1 | Nguồn đối chiếu | Sửa thành |
|---|---|---|---|
| ① | Delivery flow giao **Long** | Proposal slide 3: Vĩ phụ trách *"delivery simulator"* · `03-contracts.md` §5: `DeliverySkill` = **Vĩ** | → **V9 (Vĩ)** |
| ② | Toàn bộ video (11h) giao **Dương** | Proposal slide 3: *"video & write-up: cả đội cùng thực hiện… mỗi thành viên chịu trách nhiệm demo phần mình"* | → Dương giữ kỹ thuật quay + dựng (8h); **mỗi người tự quay/dẫn phần mình** (L13/V15/T13) |
| ③ | DTC gộp **"Tùng + Vĩ"** ở kế hoạch gốc | Proposal slide 3: Tùng phụ trách *"DTC/UDS simulator"* | → **T10 (Tùng)** trọn gói. Giữ nguyên như v1, nay có căn cứ |

### B. Chín hạng mục thiếu — đã bổ sung

| # | Thiếu gì | Nguồn yêu cầu | Đã thêm |
|---|---|---|---|
| ④ | **`door_lock` không có task nào** | Slide 11 cam kết *"≥5 lệnh car control (**cửa**, âm lượng, media, điều hòa)"* · `03-contracts.md` §5 `BodySkill` · Safety Guard G1 vô nghĩa nếu không có lệnh mở cửa | **T4** |
| ⑤ | **`volume_*` không có task nào** | Slide 11 · `03-contracts.md` §5 giao MediaSkill · webinar đề #3 *"điều chỉnh âm lượng"* | **D8** |
| ⑥ | **`CarAudioManager` / audio focus** | Guideline CDC mục 4 liệt kê là API cốt lõi cho *"media/voice app"*. Demo có nhạc đang phát + ra lệnh + TTS trả lời → bắt buộc phải duck | **L7** + **D8** |
| ⑦ | **`CarUxRestrictionsManager`** | Guideline CDC mục 5 (*Driver Distraction*) và mục 8 (test bằng widget **Road Simulator**) · tiêu chí *UX* của đề #1 | **T5** (vào `VehicleState`) + **D2**, **D9** (HMI) |
| ⑧ | **HVAC thiếu chế độ gió & ghế sưởi** | Webinar đề #2, chức năng chính: *"nhiệt độ, tốc độ quạt, **chế độ gió** và **điều hòa ghế**"* | **T7** — thêm nếu DBC có signal; nếu không, **ghi rõ lý do trong README** |
| ⑨ | **Media thiếu shuffle/repeat/seek** | Webinar đề #1, chức năng chính: *"phát, tạm dừng, chuyển bài, **tua nhanh**, **phát ngẫu nhiên** và **lặp lại**"* | **D7** |
| ⑩ | **Tương quan DTC bị cắt nhưng là tiêu chí chấm** | Webinar đề #4, tiêu chí *Analysis depth*: *"tần suất, xu hướng và **tương quan** lỗi"* · `03-contracts.md` §7 có `correlation` — mâu thuẫn với danh sách cắt ở `04-KE-HOACH` PHẦN 4 | **T10** khôi phục correlation. **Quy tắc: làm T3 thì làm đủ 3 trục, không làm thì bỏ hẳn** — làm nửa vời ăn điểm thấp hơn không làm |
| ⑪ | **Testability của đề #2 không ai lo** | Webinar đề #2, 1 trong 4 tiêu chí: *"có thể kiểm chứng mà không cần phần cứng ECU thực tế"* | **T8** |
| ⑫ | **Extensibility của đề #3 không ai lo** | Webinar đề #3, 1 trong 4 tiêu chí: *"quản lý intent rõ ràng, không cần tái cấu trúc core"*. v1 chỉ có extension point cho Media | **L11** |

### C. Ba việc phải quyết, không phải việc phải code

| # | Vấn đề | Vì sao quan trọng | Ai · khi nào |
|---|---|---|---|
| ⑬ | **Đã bỏ mất câu xin `Hackathon_IVI_CDC_2026.md`.** Nó là câu #1 trong 8 câu ở `01-tin-nhan-gui-mentor.md`, nhưng không còn trong 5 câu ở `05-tra-loi-kickoff.md` | Guideline CDC nói thẳng: file này chứa *"bảng Tiêu chí chấm điểm"* và **"chính xác API/property bắt buộc"** của đề đã chọn. Đội đang đoán bằng webinar | **Long — thêm lại vào L1, 28/07** |
| ⑭ | **Cam kết "edge-only vs hybrid" không còn thực hiện được** — slide 9 và slide 11 đều hứa, nhưng tầng T2 cloud LLM đã cắt | BGK Vòng 2 chính là mentor đã đọc proposal. Hứa mà im lặng bỏ thì tệ hơn nhiều so với đổi có giải thích | **Long — L10, 05/08.** Hai lựa chọn: giữ 1 đường T2 tối giản chỉ để đo, hoặc đổi trục sang *ASR on-device vs ASR container* và nói rõ lý do trong write-up |
| ⑮ | **Cross-vertical đang bị cắt, nhưng có điểm cộng riêng** | Thể lệ 6.7: *"phát triển cross-vertical được khuyến khích và **có tiêu chí cộng điểm riêng**"*; bảng chấm có dòng *"(+05) kết hợp nguyên liệu từ **2 domain trở lên**"*. DTC/UDS của đội **chính là nguyên liệu vertical Vehicle Middleware** — gần như đã có sẵn, chỉ thiếu một đoạn write-up nói rõ điều đó | **Long — trong L12.** Không mở thêm scope, chỉ viết cho đúng |

### D. Một điểm mơ hồ nên hỏi mentor

Thể lệ có **ba bảng 100 điểm** và vị trí bảng bị trôi so với tiêu đề mục (trang 6, 7, 8):

| Bảng | Nội dung | Đội đang giả định |
|---|---|---|
| A | Ý tưởng 35 · Khả thi 30 · Hiểu đề & starter pack 20 · Năng lực đội 15 | = Vòng 1 |
| B | Ý tưởng & insight 25 · Kết dính 20 · Chất lượng thực thi 20 · Tận dụng nền tảng 15 · README 10 · Hiểu khách hàng 10 | = **Vòng 2** |
| C | Tài liệu-slide 5 · Thuyết trình 10 · Trả lời BGK 10 · Sáng tạo 35 · **Demo 40** | = Vòng 3 chung kết |

Giả định này **đúng về mặt ngữ nghĩa** — bảng A hỏi *"khả thi ở các vòng tiếp theo"* (chỉ hợp với proposal),
bảng C có *"thuyết trình"* và *"trả lời BGK"* (chỉ hợp với sân khấu). Nhưng nếu bảng C cũng áp cho Vòng 2 thì
**Demo 40đ** và **"tích hợp ≥2 domain +5đ"** đổi hẳn thứ tự ưu tiên. Đây là biến thể của câu hỏi #1 đội đã gửi
(*chấm 1 sản phẩm hay 4 barem riêng*) — nên hỏi luôn trong cùng câu, không tốn thêm lượt.

---

## PHẦN 9 — VIỆC HÔM NAY (28/07)

| Người | Việc | Hạn |
|---|---|---|
| **Long** | Gửi kick-off + 5 câu hỏi, **thêm lại câu xin `Hackathon_IVI_CDC_2026.md`** và câu hỏi barem ở mục D | 20:00 |
| **Long** | Đăng file này vào nhóm; mỗi người xác nhận bằng chữ đã đọc phần của mình | 21:30 |
| **Vĩ** | V1 DBC + VSS → bảng signal · V2 backup + clone + deploy · V3 tra node/pin | 22:00 |
| **Tùng** | T1 đọc DBC → bảng ánh xạ property ↔ signal, xoá hết giả định | 23:00 |
| **Dương** | Dựng sẵn project Android + `car-ui-lib` để 29/07 chỉ còn cắm vào Device | 23:00 |
| **Cả 4** | 🆕 **Báo cáo kết quả spike S2 (VHAL) · S3 (USB media) · S4 (platform) vào nhóm** — `02-runbook-4-spike-M0.md` giao từ 26/07 mà chưa ai chốt. S3 quyết định D4 có khả thi không | 21:30 |
| **Cả 4** | Standup đầu tiên | 21:30 |

> Việc quan trọng nhất hôm nay không phải code. Là **xoá dòng giả định `HvacCommand.Driver_Temperature`** —
> Tùng không được viết Script Node trước khi có tên signal thật từ DBC.
