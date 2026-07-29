# KẾ HOẠCH CẬP NHẬT — 28/07/2026

> Bản này **thay thế PHẦN D (lịch mốc) và PHẦN F (rủi ro) của `00-KE-HOACH-VONG-2.md`**.
> Các phần A/B/C/E/G của file gốc vẫn còn hiệu lực.
> Lý do cập nhật: ① mentor đã trả lời 3 câu chặn · ② BTC công bố timeline checkpoint C0–C3.

---

## PHẦN 1 — MENTOR ĐÃ TRẢ LỜI GÌ (28/07)

| # | Câu hỏi cũ | Trả lời | Kế hoạch phải đổi gì |
|---|---|---|---|
| 2 | Xin danh sách Vehicle Property đã wire | **Đội tự đọc file DBC trong Artifacts** | Bỏ việc chờ hạ tầng. Tải artifact `bcm`, `pwt` (category DBC) + `vss` → đó là nguồn sự thật về tên tín hiệu |
| 3 | Có quyền thêm Container Node không | **Đội được sửa design và thêm node theo nhu cầu** | Bỏ toàn bộ phương án dự phòng `adb reverse`. Bỏ rủi ro "property trả null → chờ hạ tầng" |
| 4 | Mic có vào được VM không | **App trong Skycraft đọc được audio từ mic trên widget** | **Chốt PHƯƠNG ÁN A.** Xoá hoàn toàn nhánh PA-B (bắt audio ở host) |
| — | Property nào bắt buộc | Mentor gợi ý **luồng chuẩn** (xem PHẦN 2) | Đây chính là xương sống kỹ thuật của đề #2 |

**Còn treo — gửi bằng văn bản TRƯỚC kick-off, xác nhận lại tại buổi kick-off:**

| # | Câu hỏi | Chặn việc gì | Tự xoay được không |
|---|---|---|---|
| 1 | ~~Chấm **1 sản phẩm tích hợp hay 4 barem riêng?**~~ | Quyết định T3 (DTC) đáng làm không | ✅ **29/07 đã rõ: MỘT barem 100đ duy nhất cho Vòng 2**, không chia theo đề. Thể lệ ghi thẳng *"không cộng điểm theo số lượng chức năng, màn hình, module"* → **T3 (DTC) không đáng làm**, xem `08` |
| 2 | Đội có được cấp **GPU/LLM quota** không? (C0 có nhắc) | Chọn PhoWhisper-small hay whisper-tiny INT8 | ⚠️ thiết kế container cho swap được model → không chặn |
| 3 | VM có outbound internet không? | Không còn ảnh hưởng gì (cloud LLM đã cắt) | ✅ non-blocking |
| 4 | Tên node CCU + tên pin CAN trong blueprint đội | Đường `/run/nydus/uds-<pin>.sock` cho T3 | ✅ **tự tra được** — `GET /api/v1/deployments/:roomId/nodes` hoặc mở canvas |
| 5 | Xin bộ source tutorial Test Bench AEB | Học pattern Container Node | ✅ nice-to-have |

> ⚠️ **Không câu nào trong 5 câu này chặn tầng T1.** Đội chạy hết tốc lực xương sống, không ngồi chờ kick-off.
> Chỉ câu #1 thực sự cần mentor, và nó chỉ ảnh hưởng T3 — thứ nằm cuối hàng đợi.

---

## PHẦN 2 — LUỒNG CHUẨN MENTOR CHO (xương sống đề #2)

```
App (CarPropertyManager.setProperty)
  → Car Service
  → VHAL pin ─────────── Script Node Luau (VehicleServer gRPC)   ← ĐỘI TỰ VIẾT
  → CAN signal ───────── CAN Bus Node (decode bằng DBC trong Artifacts)
  → KUKSA Databroker ── VSS, cổng 55555
  → CCU (Climate Control Unit)
```

Script Node hai chiều — đây là ~30 dòng Luau, nhưng là 30 dòng ăn điểm nhất của cả dự án:

```lua
-- Chiều xuống: app ghi property → phát lên CAN
pins.vhal0:on_change(function(ev)
  if ev.id == vhal.prop.HVAC_TEMPERATURE_SET then
    pins.can0.db.HvacCommand.Driver_Temperature:publish(ev.value)
  elseif ev.id == vhal.prop.HVAC_FAN_SPEED then
    pins.can0.db.HvacCommand.Fan_Speed:publish(ev.value)
  end
end)

-- Chiều lên: CCU phản hồi → đẩy ngược vào guest (real-time sync)
pins.can0.db.HvacStatus.Driver_Temperature:on_change(function(v)
  pins.vhal0:push(vhal.prop.HVAC_TEMPERATURE_SET, vhal.area.seat.ROW_1_LEFT, v)
end)

-- Guest đọc property → trả giá trị hiện hành
pins.vhal0:on_get(function(prop_id, area_id)
  if prop_id == vhal.prop.PERF_VEHICLE_SPEED then
    return pins.kuksa:get("Vehicle.Speed")
  end
  return nil
end)
```

> ⚠️ Tên `HvacCommand.Driver_Temperature` / `HvacStatus.*` là **giả định theo walkthrough tài liệu**.
> Phải đối chiếu với DBC thật tải từ Artifacts trước khi code.

**Đây là 4/4 tiêu chí chấm đề #2:** Full-stack end-to-end · VHAL correctness · Real-time sync · Testability.

**Cảnh quay video mạnh nhất của cả bài:** mở song song 6 khung — logcat app · log Script Node · Signal Watch (CAN) · Signal Watch (KUKSA) · log CCU · màn hình HMI. Nói *"hạ điều hòa xuống 22 độ"*, cả 6 đổi cùng lúc.

### An toàn khi sửa blueprint (làm đúng thứ tự này)

1. `GET /api/v1/blueprints/:id/export` → lưu JSON làm backup
2. **Clone** blueprint rồi sửa trên bản clone — không sửa "Started pack" tại chỗ
3. Deploy bản clone lên Device của đội
4. Sửa topology **bắt buộc redeploy** → VM restart vài phút. **Không làm việc này sau 05/08.**

---

## PHẦN 3 — TIMELINE CHECKPOINT CỦA BTC (C0–C3)

| Mốc | Ngày | BTC muốn thấy | Trạng thái đội |
|---|---|---|---|
| **C0 — Kickoff** | trước 21/07 | Cam kết scope 1 trang: must-have · nice-to-have · stack · tài nguyên cần (GPU/LLM quota) · rủi ro lớn nhất | Đã qua — nội dung nằm trong proposal V1 |
| **C1 — Skeleton** | 27/07 (T2) | Demo 5' luồng chính chạy xuyên suốt ở mức khung + báo cáo 1 trang | ⚠️ **ĐỘI ĐANG TRỄ** — kế hoạch cũ đặt xương sống ở 31/07 |
| **C2 — Giữa kỳ** | 03/08 (T2) | Demo 10' end-to-end tính năng lõi trên nền tảng + **số đo đầu tiên** (KPI tự đặt) | Còn **6 ngày** |
| **C3 — Code freeze** | 08/08 (T7) | Test chạy được + **video demo 3' KHÔNG CẮT GHÉP** + slide pitch | Còn 11 ngày |
| **Nộp bài** | 10/08 23:59 (T2) | Source · Documentation · Video 5–7' · Write-up | Nộp trưa 10/08 |

> Timeline này BTC nói là **để tham khảo về output**, không phải mốc chấm điểm.
> Nhưng nó là nhịp mentor quan sát — bám sát để lấy feedback khi còn kịp sửa.

### Ba điều C0–C3 buộc phải đổi trong kế hoạch cũ

**① Benchmark phải chạy sớm 4 ngày.** C2 (03/08) đòi *"số đo đầu tiên"*. Kế hoạch cũ để benchmark ở M5 (06–07/08). → **Harness v1 của Vĩ chuyển lên 31/07**, ra được p50/p95 thô trước 02/08.

**② Video 3' không cắt ghép là ràng buộc thiết kế demo, không phải việc quay phim.** Một lần chạy 3 phút không dừng nghĩa là: tối đa 5–6 lệnh thoại, không có thời gian phục hồi khi lỗi. → **Chốt kịch bản demo trước 02/08** để có 6 ngày tổng duyệt, không phải nghĩ kịch bản vào 08/08.

**③ Đội cần HAI video khác nhau.** 3' uncut cho C3 (08/08) + 5–7' bản nộp chính thức (09/08). Bản uncut chính là bằng chứng mạnh nhất cho tiêu chí ~~*"Chất lượng thực thi 20đ"*~~ → 🆕 *"**Demo live online và độ ổn định** — 6đ"* của barem mới, ô L3 đòi *"lần lặp hoặc bằng chứng repeatability"*.

> 🆕 **Nhưng video không thay được demo trực tiếp.** Barem mới có mục "Demo và Q&A" riêng và ô L0 ghi
> *"Không chạy trực tiếp do nguyên nhân thuộc phía đội"*. Đội phải tập **chạy live**, không chỉ tập quay —
> xem task **N7** trong `06` PHẦN 4.

---

## PHẦN 4 — CẮT PHẠM VI THEO 3 TẦNG

> ⚠️ **CẬP NHẬT 29/07 — barem trích dưới đây đã bị BTC thay.** Bảng mới:
> *Demo 25 · Kỹ thuật 20 · **Team-owned 25** · Platform 15 · Khách hàng 10 · Trình bày 5.*
> Xem `08-BAREM-VONG-2-CHINH-THUC.md`. **Kết luận "cắt phạm vi" dưới đây vẫn đúng — và mạnh hơn trước:**
> barem mới ghi thẳng *"không cộng điểm theo số lượng chức năng, màn hình, module"*.

Barem Vòng 2 (100đ) **không có dòng nào cho "làm được mấy đề"**: ~~ý tưởng 25 · kết dính 20 · chất lượng thực thi 20 · tận dụng nền tảng 15 · tài liệu 10 · hiểu khách hàng 10~~. Một sản phẩm chạy mượt xuyên tầng ăn điểm cao hơn bốn app rời rạc. Barem đứng về phía cắt — đúng như mentor khuyên.

| Tầng | Nội dung | Xong trước | Chạm tiêu chí |
|---|---|---|---|
| **T1 — BẮT BUỘC 100%** | Voice push-to-talk → VAD → ASR → T0 grammar · **Climate đủ 6 chặng** · Safety Guard G1/G2 · HMI real-time | **02/08** | Kết dính 20 · Tận dụng nền tảng 15 · Đề #2 + #3 |
| **T2 — sau khi T1 xanh** | Media: `MediaSourceProvider` + `UsbMediaProvider` + cache album art · Delivery flow in-app (3 intent) | 05/08 | Đề #1 *Extensibility* · Đề #3 *Coverage* |
| ~~**T3**~~ 🚫 **ĐÃ BỎ 29/07** | ~~DTC: Script Node `nydus.uds.server` + tester ISO-TP `19 02 FF` + phân nhóm P/C/B/U~~ | — | ~~Đề #4 *Simulation fidelity*~~ → **Barem mới không chấm theo tiêu chí từng đề.** 9h chuyển sang N3b + N4b của Tùng, xem `08` |

**Cắt dứt khoát — ghi ra để không ai bị cám dỗ:**

- ❌ Cloud LLM tầng T2
- ❌ Wake word "Vivi ơi" (dùng push-to-talk)
- ❌ Intent classifier T1 (giữ T0 grammar)
- ❌ Container `viva-svc` riêng cho delivery (làm in-app)
- ❌ Phân tích tương quan DTC
- ❌ Theme ngày/đêm · barge-in · noise augmentation

---

## PHẦN 5 — LỊCH NGÀY-THEO-NGÀY TỪ 28/07

### 🔴 28/07 (T3, hôm nay) — bù C1 + chốt lịch kick-off

| Người | Việc | Xong khi |
|---|---|---|
| Long | **Trả lời chị Linh, chốt giờ kick-off** — đề xuất T5 30/07 19:00 (xem PHẦN 5b) | Trước trưa |
| Long | Gửi **5 câu hỏi bằng văn bản** + báo cáo 1 trang C1 làm pre-read | Trong hôm nay |
| Vĩ | Tải artifact DBC (`bcm`, `pwt`) + VSS về, mở ra đọc tên message/signal thật | Có file DBC trong repo + 1 bảng tên signal |
| Vĩ | Export blueprint làm backup → clone → deploy bản clone lên Device đội | Bản clone Running |
| Vĩ | `GET /api/v1/deployments/:roomId/nodes` → tự tra tên node CCU + tên pin CAN | Có bảng node/pin (khỏi phải hỏi mentor) |
| Tùng | Đọc DBC, đối chiếu với property cần dùng, phác Script Node Luau | Bản nháp `.lua` |

### 🤝 PHẦN 5b — KICK-OFF VỚI MENTOR

Chị Linh đề xuất một buổi kick-off (làm quen + hỏi đáp) vào **một tối từ nay tới cuối tuần**.

**Đề xuất của đội — theo thứ tự ưu tiên:**

| Ưu tiên | Thời điểm | Lý do |
|---|---|---|
| **1** | **T5 30/07, 19:00–20:00** | Trùng khung office hours mentor đã block sẵn → khả năng đủ người cao nhất. Đội có 2 ngày chuẩn bị, và đã có DBC + blueprint clone để nói chuyện có nội dung |
| 2 | T4 29/07, 19:30 | Nếu mentor muốn sớm hơn. Đội vẫn kịp mang kết quả spike + bảng signal từ DBC |
| 3 | T6 31/07, 19:30 | Chấp nhận được, nhưng câu #1 (cách chấm) về muộn 1 ngày |
| ❌ | T7 01/08 hoặc CN 02/08 | **Tránh** — đây là cửa sổ full-capacity duy nhất trước C2, không nên cắt bằng họp |

**Ba nguyên tắc cho buổi này:**

1. **Gửi câu hỏi bằng văn bản TRƯỚC.** Buổi kick-off là "hỏi đáp zui zẻ" — mentor không cầm sẵn thông tin về cách chấm hay GPU quota. Gửi trước để các anh kịp tra, thay vì nghe "để anh check lại rồi báo".
2. **Pre-read = báo cáo 1 trang C1.** Vừa bù mốc C1 đang trễ, vừa cho mentor thấy đội đã đọc tài liệu và chạy spike thật — ấn tượng đầu quan trọng.
3. **Không biến kick-off thành review kỹ thuật.** Mục đích là làm quen. Demo (nếu có) giữ dưới 5 phút; phần review kỹ thuật để dành office hours 04/08 và 06/08.

> ⚠️ **Kick-off không nằm trên đường găng.** Xem bảng ở PHẦN 1: không câu hỏi nào chặn T1.
> Đội vẫn chạy xương sống đúng lịch 29–31/07 dù buổi này rơi vào ngày nào.

### 🟠 29–31/07 (T4–T6) — XƯƠNG SỐNG

> **Đích 31/07:** nói *"Hạ điều hòa xuống 22 độ"* → nhiệt độ đổi thật qua đủ 6 chặng → HMI cập nhật → log ra timestamp đủ chặng. **Một câu, một luồng, chạy được.**

| Người | Việc | Deadline |
|---|---|---|
| Tùng | **Script Node Luau VHAL ↔ CAN hai chiều** + deploy + xác minh bằng Signal Watch | 30/07 |
| Tùng | Lớp `CarPropertyManager` wrapper + Safety Guard G1 | 31/07 |
| Long | VAD (Silero ONNX) + `AsrClient` + T0 grammar 5 lệnh + `LatencyTrace` đủ 6 mốc | 30/07 |
| Vĩ | Container `viva-asr` push Zot (`registry.carsky.io`) → thêm node vào blueprint → chạy trong Room | 30/07 |
| Vĩ | **Harness v1** (đẩy sớm từ M5) — parse `VIVA_TRACE\|` qua `adb_shell`, xuất CSV | 31/07 |
| Dương | HMI 3 vùng, phản chiếu HVAC real-time + TTS 3 câu mẫu | 31/07 |
| Cả đội | **30/07 (T5) 19:00** — kick-off mentor (nếu chốt được ngày này). Demo ngắn ≤5' phần đã chạy, còn lại là làm quen + hỏi đáp | 30/07 |

> ⚠️ **Nếu 31/07 chưa đạt xương sống → dừng mọi thứ khác, cả 4 người dồn vào đây.** Không có xương sống thì không có C2.

### 🟡 01–02/08 (T7–CN, full) — cửa sổ quyết định

| Người | Việc |
|---|---|
| Cả đội | Khoá T1: Voice + Climate + Safety Guard G1/G2 + HMI chạy ổn định |
| Dương | Bắt đầu T2 — `MediaSourceProvider` + `UsbMediaProvider` + cache album art |
| Long | Delivery flow in-app (3 intent) + **chốt kịch bản demo 3'** |
| Vĩ | Benchmark ≥20 utterance → **p50/p95 đầu tiên** cho C2 |
| Cả đội | **02/08 tối: chạy thử trọn vẹn demo 10' của C2** |

### 🟢 03/08 (T2) — **C2**

Nộp demo 10' end-to-end + số đo đầu tiên. Đây là lần cuối mentor còn kịp nắn hướng.

### 🔵 04–05/08 — tích hợp & FEATURE FREEZE

| Người | Việc |
|---|---|
| Dương | Hoàn thiện Media (T2) |
| ~~Tùng + Vĩ~~ | ~~T3 (DTC)~~ 🚫 **đã bỏ 29/07** → thay bằng **N3b + N4b** (Baseline Manifest phần VHAL + ablation A1), hạn 06/08 |
| Vĩ | Harness v2: 20+ câu tự động, `send_signals` set trạng thái xe, `screenshot` làm bằng chứng |
| Tùng | Safety scenario pack ≥6 kịch bản pass/fail |
| Cả đội | **Office hours 04/08 (T3)** |
| — | 🚫 **FEATURE FREEZE 05/08 23:59** — sau mốc này chỉ sửa lỗi |

### 🟣 06–07/08 — đo đạc & tài liệu

| Người | Việc |
|---|---|
| Vĩ | Benchmark đầy đủ: ≥20 câu × 3 mức nhiễu, p50/p95 + biểu đồ |
| Vĩ | README hoàn chỉnh (có **bảng tín hiệu/property đã dùng** lấy từ DBC) |
| Tùng | Chạy safety pack, xuất báo cáo pass/fail |
| Long | Write-up câu chuyện AI: prompt đã dùng, AI đúng ở đâu, sai ở đâu, MCP-driven testing |
| Dương | Sơ đồ kiến trúc + extension point |
| Cả đội | **Office hours 06/08 (T5)** — checkpoint cuối |

### ⚫ 08/08 (T7) — **C3**

- Slide pitch
- **Video 3' KHÔNG CẮT GHÉP** — quay bằng Recorder Part của widget Screen, một lần chạy
- Test chạy được
- Tổng duyệt: cài APK sạch → chạy full kịch bản → logcat không crash

### ⚫ 09/08 (CN) — bản nộp chính thức

- Video 5–7' (được dựng, chèn overlay 6 khung chứng minh không mock)
- **Video backup dự phòng** (guideline CarSky yêu cầu rõ)
- Smoke test trên Device sạch
- Nhờ người ngoài đội làm theo README trên máy sạch — kiểm chứng "code chạy lại được"

### 🏁 10/08 (T2) — nộp trước trưa

- 10:00 push repo cuối, kiểm tra `.env` / API key **không** bị commit
- 11:00 đối chiếu checklist nộp bài
- 12:00 **đã nộp xong**. 12 tiếng còn lại là bảo hiểm, không phải thời gian làm việc

---

## PHẦN 6 — RỦI RO CÒN LẠI (thay PHẦN F file gốc)

| Rủi ro | Dấu hiệu sớm | Phương án |
|---|---|---|
| **Trễ xương sống so với C1** | 31/07 chưa chạy được 1 câu đủ 6 chặng | Cả 4 người dồn vào xương sống, hoãn T2 và T3 vô điều kiện |
| **Redeploy blueprint làm hỏng Room** | Node không lên Running sau khi sửa | Đã có backup JSON export; deploy lại bản gốc. **Không sửa blueprint sau 05/08** |
| **Tên signal trong DBC khác giả định** | Script Node publish nhưng Signal Watch không đổi | Đọc DBC trước khi code — việc của 28/07, không để trôi |
| **ASR quá chậm trên pod CPU** | RTF > 0.5 | Hạ xuống whisper-tiny INT8; hỏi mentor về GPU quota (câu #3 PHẦN 1) |
| **TTS không có tiếng Việt** | `isLanguageAvailable()` trả false | Pre-render ~30 câu phản hồi cố định vào `res/raw/` |
| **Demo 3' uncut fail giữa chừng** | Tổng duyệt lỗi ở lần chạy thứ 2, 3 | Kịch bản chốt từ 02/08 → có 6 ngày tập. Cắt bớt lệnh cho đến khi chạy sạch 3 lần liên tiếp |
| **Room sự cố đúng lúc demo** | — | Video backup quay từ 08/08 |

---

## PHẦN 7 — VIỆC PHẢI LÀM TRONG HÔM NAY

1. **Long** — trả lời chị Linh chốt giờ kick-off (đề xuất T5 30/07 19:00), kèm 5 câu hỏi văn bản + báo cáo 1 trang C1 làm pre-read
2. **Vĩ** — tải DBC + VSS từ Artifacts, đọc tên signal thật, đẩy lên repo
3. **Vĩ** — export blueprint backup → clone → deploy bản clone
4. **Vĩ** — tra tên node CCU + tên pin CAN qua `GET /api/v1/deployments/:roomId/nodes`
5. **Tùng** — đọc DBC, phác Script Node Luau VHAL↔CAN
6. **Cả đội** — xác nhận đã đọc PHẦN 4 và đồng ý với danh sách cắt
