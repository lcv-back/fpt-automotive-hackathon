# PHÂN CÔNG VÒNG 2 — 4 NGƯỜI · CÓ DEADLINE
### Team VIVA · bản v2 ngày 28/07/2026 · còn 13 ngày tới hạn nộp 23:59 10/08

> **v2 sửa gì so với v1:** đối chiếu lại với **proposal đã nộp (PPTX slide 3)**, `03-contracts.md`,
> guideline CDC và bảng tiêu chí từng challenge trong webinar 25/06. Phát hiện **3 lỗi phân vai**
> và **9 hạng mục thiếu**. Chi tiết ở PHẦN 8.
>
> Bản này thay PHẦN C của `00-KE-HOACH-VONG-2.md` và chi tiết hoá PHẦN 5 của `04-KE-HOACH-CAP-NHAT-28-07.md`.
> Chiến lược và phạm vi cắt T1/T2/T3 giữ nguyên.
>
> **🚨 29/07 — BTC ĐÃ ĐĂNG BAREM VÒNG 2 MỚI, BẢNG CŨ BỊ XOÁ**
>
> Barem đội đang dùng (*Ý tưởng 25 · Kết dính 20 · Thực thi 20 · Nền tảng 15 · README 10 · Khách hàng 10*)
> **không còn trong thể lệ**. Bảng mới: **Demo 25 · Kỹ thuật 20 · Team-owned 25 · Platform 15 · Khách hàng 10 · Trình bày 5.**
> Giải mã đầy đủ ở **`08-BAREM-VONG-2-CHINH-THUC.md`** — đọc trước khi làm theo file này.
>
> **Ba thay đổi chạm vào bảng task dưới đây:** ⑴ thêm **7 task N1–N7** phục vụ khối team-owned 25đ + khách hàng 10đ
> + demo live 6đ đang bị bỏ trống; ⑵ 🚫 **ĐÃ CHỐT: bỏ hẳn T10 DTC**, 9h của Tùng chuyển sang N3 + N4 —
> cross-vertical không còn là dòng điểm ở Vòng 2 (xem PHẦN 8 mục ⑮ đã sửa); ⑶ mục D "ba bảng 100 điểm"
> **đã tự trả lời**, bỏ khỏi danh sách hỏi mentor.
>
> ---
>
> **⭐ 31/07 — MENTOR ĐÃ SỬA KIẾN TRÚC SAU KICK-OFF TỐI 30/07**
>
> Nguyên văn: *"Luồng chạy này của các bạn chưa đủ — **không có phần vhal nào nhận intent cả**.
> Chính xác thì luồng các bạn cần xử lý như thế này: (Agent → STT → command) APP → **service fw** →
> **PropertyID** ← vhal → CAN signal → CCU."*
>
> Đội **chưa có thành phần nào đóng vai *service fw***: `VhalRepository` (T3) là library trong app.
> Và **chưa có bảng dịch `intent → PropertyID`** — mắt xích mentor nói đang thiếu.
> Thêm **8 task M1–M8**, giải mã đầy đủ ở **`11-PHAN-HOI-MENTOR-KICKOFF-30-07.md`** — đọc file đó trước.
>
> **Bốn thứ chạm vào bảng dưới đây:** ⑴ **M1 `VivaCarService`** (Tùng + Vĩ) — vendor car service của
> riêng đội, T3 được đóng gói lại vào trong nó chứ không bị bỏ; ⑵ **M2 bảng intent → PropertyID**
> (Long + Tùng, **hôm nay**); ⑶ **M3 tách app HVAC và app DOOR** (Dương) — kick-off chốt "app phải được
> kích hoạt lên, hiển thị lên app"; ⑷ **M4** — starter kit đã có Script Node Skycraft ↔ KUKSA và BTC đã
> dựng sẵn candb, **T2 có thể ngắn hơn nhiều so với 8h đang ghi**. Mốc cân 1 tối nay có **4** câu hỏi,
> không phải 2 — xem PHẦN 6.

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

⭐ **Kick-off 30/07 chốt lại phân vai — khớp với bảng trên, không đổi cam kết BTC:**

| Ghi được ở kick-off | Ánh xạ vào bảng task |
|---|---|
| *"Vĩ và Tùng hợp tác với nhau xử lý phần ứng dụng bắt tay với AI Agent, HVAC"* | **M1 `VivaCarService`** — chỗ "bắt tay" chính là service fw. Tùng chủ, Vĩ đóng gói/cài đặt |
| *"Anh Dương làm phần Media"* | D5–D8 giữ nguyên · thêm **M3** (app HVAC + app DOOR) vì Dương là người dư giờ nhất |
| *"Cần xây dựng lên app HVAC và DOOR… tích hợp AI Agent vào service thì app phải được kích hoạt lên"* | **M3** |
| *"Xây dựng vendor car service của riêng mình"* | **M1** — và đây là chữ ăn thẳng vào ô *Tách phần team-owned* **5đ** |

---

## PHẦN 1 — NGÂN SÁCH GIỜ

| Ngày | 28 | 29 | 30 | 31 | 01 | 02 | 03 | 04 | 05 | 06 | 07 | 08 | 09 | 10 | **Tổng** |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| Giờ/người | 4 | 4 | 4 | 4 | 10 | 10 | 4 | 4 | 5 | 4 | 4 | 10 | 10 | 3 | **80h** |

**320 person-hour.** Phân bổ:

| Người | Task bắt buộc | Task điều kiện | Việc chung | Đệm |
|---|---|---|---|---|
| **Long** | 53h → **55h** *(−L5 2.5h −L12 2h +N1 3.5h +N2 2h +N6 1h)* | — | 16h → **19.5h** | 11h → **5.5h** |
| **Vĩ** | 50h → **54.5h** *(+N3a 1.5h +N4a 2h +N5 1h)* | — | 16h → **19.5h** | 14h → **6h** |
| **Tùng** | 47h → **49.5h** *(+N3b 1.5h +N4b 1h)* | 🚫 ~~+9h DTC~~ **T10 đã bỏ** | 16h → **19.5h** | 17h → **11h** |
| **Dương** | 51h → **48h** *(−D5 2h −D6 2h +N6 1h)* | — | 16h → **19.5h** | 13h → **12.5h** |

🆕 **Cân bằng sau khi bỏ T10.** 9h giải phóng của Tùng **không trả về quỹ đệm** mà chuyển thẳng sang
N3 + N4 — đúng như lý do bỏ T10. Tùng nhận **N3b** (phần VHAL/CAN/Luau của Baseline Manifest — anh ấy
là người duy nhất biết CarSky cấp sẵn gì và đội tự wire gì) và **N4b** (ablation A1 tắt `SafetyGuard`,
vì `SafetyGuard` là của anh ấy). Phần còn lại Vĩ giữ vì sở hữu harness.

Chênh lệch tối đa **7h** (Long 55h vs Dương 48h) — đều hơn bản trước. Tùng còn **11h đệm**, nhiều
nhất đội, cố ý: T2 (Script Node Luau 8h) vẫn là task rủi ro nhất, và anh ấy là người nhận việc đầu tiên
ở mốc cân 2 nếu ai đó trễ.

### ⭐ 31/07 — CÂN LẠI SAU KHI THÊM M1–M8

| Người | Task cá nhân | Nhận thêm | Đệm | Ghi chú |
|---|---|---|---|---|
| **Long** | 55h → **56.5h** | M2 1h · M7 3h · *cắt lại L5b −1h, L9 −0.5h, L10 −1h* | 5.5h → **4h** | Chi tiết cắt ở đâu: `07` PHẦN 3 |
| **Vĩ** | 54.5h → **57.5h** | M1 2h · M4 1h | 6h → **3h** | |
| **Tùng** | 49.5h → **60.5h** | M1a 1.5 · M1 5 · M2 1 · M3 1 · M4 1 · M5 1.5 | 11h → **~0h** | 🚨 **vỡ** — xem dưới |
| **Dương** | 48h → **53h** | M3 4h · M6 1h | 12.5h → **7.5h** | Người dư giờ nhất, nhận M3 là đúng chỗ |

> 🚨 **Đệm 11h của Tùng đã tiêu hết, và đó là quỹ dự phòng cho T2 — task rủi ro nhất cả đội.**
> Không giả vờ rằng con số này ổn. Ba đường thoát, theo thứ tự:
>
> 1. **M4 phải làm hôm nay.** Nếu Script Node starter kit + candb BTC dùng được thì T2 trả lại **3–4h** —
>    đủ để lấp phần lớn lỗ hổng. Đây là lý do M4 xếp trên mọi việc khác của Tùng sáng nay.
> 2. **Hoãn M5 (1.5h) rồi M3-phần-Tùng (1h)** nếu M4 không giúp được. Không được hoãn M1a · M1 · M2.
> 3. **Mốc cân 2 (03/08): Dương là người chuyển việc sang Tùng**, không phải ngược lại — Dương còn 7.5h đệm.
>
> Chênh lệch giờ giữa Tùng (60.5h) và Dương (53h) giờ là **7.5h** — vẫn trong ngưỡng, nhưng **rủi ro thì
> không cân**: Tùng giữ cả đường găng cũ lẫn phần lớn việc mới.

### ⚠️ Cân bằng theo tổng ĐÚNG, nhưng 4 ngày đầu KHÔNG VỪA

Tổng 13 ngày thì đều. Nhưng dồn vào 28–31/07 (chỉ 4h/ngày) thì vỡ:

| | 28–31/07 cần | Có (4h × 4 − kick-off − standup) | Thiếu |
|---|---|---|---|
| Long | 20h | 14h | −6h |
| **Tùng** | **24h** | 14h | **−10h** |
| **Vĩ** | **24h** | 14h | **−10h** |
| Dương | 14h | 14h | 0 |

**Cách xử đã chốt: chẻ nhỏ task, hạ đích 31/07 xuống mức tối thiểu** — xương sống chỉ cần *"một câu,
một luồng"*, không cần đủ tính năng. Long đã chẻ L3 → a/b/c và L5 → 5 lệnh (31/07) + đủ 10 intent (01/08);
xem `07-PLAN-CA-NHAN-LONG.md` PHẦN 1–2 làm mẫu.
**Tùng và Vĩ phải tự chẻ task của mình y hệt, chốt trong standup 28/07.**

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

### 🔵 NGÔ VĂN LONG — Voice AI & Kiến trúc (55h → ⭐ **56.5h**)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| L1 | Gửi kick-off + 5 câu hỏi + báo cáo 1 trang C1. **Thêm lại câu xin `Hackathon_IVI_CDC_2026.md`** (xem PHẦN 8 mục ⑩) | Đã gửi, có xác nhận | 2h | 🔴 **28/07 20:00** |
| L2 | `LatencyTrace` 6 mốc + log format `VIVA_TRACE\|` đúng `03-contracts.md` | Vĩ parse được 1 dòng mẫu | 3h | 🔴 **29/07** (Vĩ chờ) |
| L3 | Push-to-talk `AudioRecord` + VAD Silero ONNX | Nhấn giữ → cắt đúng đoạn nói | 6h | 🔴 **30/07** |
| L4 | `AsrClient` + timeout/retry + **stub cục bộ** để không chờ Vĩ | Chạy với stub trước, đổi endpoint sau | 3h | 🔴 **30/07** |
| L5 | Intent Router T0 grammar — ~~đủ 15 intent~~ → **10 intent lõi** của `03-contracts.md` §3 v2 + unit test<br>*(barem mới: **không cộng điểm theo số lượng chức năng**. Bộ 10 giữ nguyên cam kết slide 11 "≥5 lệnh car control" + 3 intent delivery của Vĩ)* | 10/10 câu mẫu ra đúng intent · 5 câu đã cắt bị **từ chối lịch sự**, không rơi vào `unknown` im lặng | ~~6h~~ **3.5h** | 🔴 **31/07** |
| L6 | `TtsSpeaker` + pre-render ~30 câu vào `res/raw/` | Nói được dù thiếu `vi-VN` | 4h | 🟡 01/08 |
| L7 | 🆕 **Audio focus** — `CarAudioManager`: duck nhạc khi TTS nói, trả focus sau | Nhạc đang phát + ra lệnh + TTS trả lời, không chồng tiếng | 3h | 🔴 **02/08** |
| L8 | **Chốt kịch bản demo 3' uncut** — ≤6 lệnh, có lời thoại, có đường thoát khi 1 lệnh fail | Văn bản + đã chạy thử 1 lần | 2h | 🔴 **02/08** |
| L9 | Tinh chỉnh đạt **p95 < 1500ms** đường edge | Số của Vĩ xác nhận | 5h | 🔴 **05/08** |
| L10 | 🆕 **Chốt trục so sánh thay "edge vs hybrid"** đã cam kết ở slide 9/11 (xem PHẦN 8 mục ⑫) | Có quyết định + 1 đoạn giải thích trong write-up | 2h | 🔴 **05/08** |
| L11 | README: kiến trúc + voice pipeline + **extension point thêm intent mới không sửa core** | Người đọc thêm được 1 intent theo hướng dẫn | 3h | 🟡 06/08 |
| L12 | **Write-up câu chuyện AI** + đoạn claim cross-vertical (PHẦN 8 mục ⑭) | Bản hoàn chỉnh nộp được | 5h | 🔴 **07/08** |
| L13 | Điều phối 2 video + **tự quay/dẫn phần voice của mình** | Cả 2 video có phần voice chuẩn | 4h | 🔴 **08/08** & 09/08 |
| L15 | 🆕 **Slide pitch cho C3** — C3 yêu cầu *"test chạy được + video 3' uncut + **slide pitch**"* nhưng v2 chưa giao ai. Long giữ vì đã sở hữu write-up + kịch bản demo | Slide dùng được ở C3 | 3h | 🔴 **08/08** |
| L14 | Checklist nộp, push cuối, soát `.env`/API key | Đã nộp | 2h | 🔴 **10/08 11:00** |
| **N1** | 🆕 **Claim–Evidence Map** — mỗi claim cốt lõi ↔ baseline ↔ phần team-owned ↔ expected result ↔ **evidence ID**. Thay chỗ L12a vì chính là dàn ý write-up | Mỗi claim trỏ được tới 1 file log/CSV/ảnh **có tên** | 3.5h | 🔴 **07/08** |
| **N2** | 🆕 **Product & Integration Card** — user vs buyer/process owner · offering & quan hệ tiếp nhận · outcome + giả định · dependency có nhãn · bước kiểm chứng tiếp theo | Đủ 5 ô, không ô nào bỏ trống | 2h | 🟡 01/08 |
| **M2** | ⭐ **Bảng `intent → PropertyID + areaId + kiểu + value`** *(phần intent; Tùng làm phần property)* — **đây là mắt xích mentor nói đang thiếu**. Đi kèm: `03-contracts.md` §5 phải ghi rõ mỗi Skill dịch intent thành property nào | Mọi intent nhóm `hvac_*` + `door_lock` có đủ 4 cột, không dòng trống | 1h | 🔴 **31/07** |
| **M7** | ⭐ **Bộ 5 tình huống phức tạp** — mơ hồ · ghép lệnh · không an toàn · thiếu slot · ngoài phạm vi. Mentor: *"chìa khoá ăn điểm: con AI xử lý độ phức tạp ntn"* | 5/5 có **hành vi mong đợi viết trước**, chạy đúng, có log. ≥2 cái nằm trong kịch bản L8 | 3h | 🔴 **02/08** |

> **N2 là 2h lấy 10đ** — tỉ lệ tốt nhất trong cả barem. Thể lệ nói rõ *không cần* TAM, pricing hay LOI.
> Làm sớm 01/08 vì nó là khung cho cả write-up L12 lẫn slide L15.
>
> **M2 + M7 = 4h, lấy 2.5h bằng cách cắt lại L5b · L9 · L10, 1.5h còn lại từ đệm** (đệm 5.5h → 4h).
> Lý do cắt L5b: 5 intent còn lại đều là biến thể của intent đã có — mentor nói thẳng điểm nằm ở
> **chiều sâu tình huống**, và barem mới ghi *"không cộng điểm theo số lượng chức năng"*.
> Chi tiết ba chỗ cắt ở `07` PHẦN 3; bảng 5 tình huống ở `11-PHAN-HOI-MENTOR-KICKOFF-30-07.md` PHẦN 6.

---

### 🟢 LÊ CÔNG VĨ — Agent & DevOps (54.5h → ⭐ **57.5h**)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| V1 | Tải DBC (`bcm`,`pwt`) + `vss` → **bảng message/signal thật** push repo | Tùng đối chiếu được | 3h | 🔴 **28/07 22:00** (Tùng chờ) |
| V2 | Export blueprint backup → **clone** → deploy bản clone | Clone Running, backup JSON trong repo | 2h | 🔴 **28/07 22:00** |
| V3 | `GET /deployments/:roomId/nodes` → bảng node CCU + pin CAN + pin VHAL | Có bảng | 1h | 🔴 **28/07 22:00** |
| V4 | Repo Git + `.gitignore` chặn key + CI build APK | Cả 4 push được | 2h | 🔴 **29/07** |
| V5 | `vm_tunnel_open` + `adb connect` + hướng dẫn dev loop 1 trang | 3 người tự cài APK được | 2h | 🔴 **29/07** |
| V6 | Dockerfile `viva-asr` (whisper-tiny INT8, **swap được model**) + push Zot | Cluster pull được image | 5h | 🔴 **29/07** |
| V7 | Container Node `viva-asr` vào blueprint clone, guest gọi được `POST /asr` | Long đổi từ stub sang thật, ra text tiếng Việt | 4h | 🔴 **30/07** (Long chờ) |
| V8 | **Harness v1** — parse `VIVA_TRACE\|` qua `adb_shell` → CSV 6 chặng.<br>🆕 **Thiết kế sao cho bật/tắt được từng thành phần** (SafetyGuard, đường ASR, callback VHAL) — N4 sẽ cần đúng khả năng này | 1 file CSV đủ 6 chặng | 5h | 🔴 **31/07** |
| V9 | ✅ **`DeliverySkill` — 3 intent** `delivery_next_stop` / `order_status` / `confirm`<br>*(trả về đúng owner theo proposal + `03-contracts.md` §5 — v1 giao nhầm cho Long)* | 3 intent end-to-end trên HMI | 5h | 🟡 02/08 18:00 |
| V10 | Benchmark ≥20 utterance → **p50/p95 đầu tiên** cho C2 | Bảng số + biểu đồ thô | 4h | 🔴 **02/08 20:00** |
| V11 | **Harness v2** — regression qua MCP: `send_signals` → `screenshot` → `find_text` → PASS/FAIL | 20+ câu tự động, có ảnh bằng chứng | 6h | 🟡 04/08 |
| V12 | Benchmark đầy đủ 20 câu × 3 mức nhiễu, p50/p95 + biểu đồ | Số cuối dùng cho slide + README | 5h | 🔴 **06/08** |
| V13 | Lắp ghép README + mục build/deploy CarSky + **ghi nguồn thư viện open-source** | README đọc là làm theo được | 3h | 🔴 **07/08** |
| V14 | Smoke test Device sạch + **nhờ người ngoài đội làm theo README** | Người ngoài build & chạy được | 2h | 🔴 **09/08 20:00** |
| V15 | Tự quay/dẫn **phần delivery + benchmark** của mình trong 2 video | Có trong cả 2 bản | 1h | 🔴 **08/08** & 09/08 |
| **N3a** | 🆕 **Baseline Manifest — phần platform** + **compile bảng tổng**: CarSky/starter pack cấp sẵn gì, đội xây gì; ranh giới `provided / configured / modified / new`. *Ghép với N3b của Tùng* | Không thành phần nào trong core flow thiếu nhãn | 1.5h | 🟡 **06/08** |
| **N4a** | 🆕 **Ablation A2 + A3** — **A2** thay ASR container bằng đường cloud → p95 vượt 1500ms · **A3** bỏ callback `VhalRepository` → HMI mất real-time | 2 kịch bản có số before/after | 2h | 🟡 **06/08** |
| **N5** | 🆕 Bảng **3 trạng thái integration** trong README (*thật / mô phỏng / kế hoạch*) + đoạn mô tả **cách tạo synthetic data** *(trong V13)*.<br>⭐ **31/07: thêm CCU vào bảng này** — mentor cho phép *"giả lập nhận gửi CAN signal"*, nhưng giả lập thì phải khai nhãn **mô phỏng**, không được khai *"đã tích hợp"* (ô *Ranh giới và tính tương xứng* 2đ) | Không mục nào khai sai nhãn | 1h | 🔴 **07/08** |
| **M1**<br>*(phần Vĩ)* | ⭐ **`VivaCarService`** cùng Tùng — phần Vĩ: đóng gói/cài đặt service lên Device (privileged nếu quyền đòi) + đưa vào CI build. *Kick-off giao: "Vĩ và Tùng hợp tác xử lý phần ứng dụng bắt tay với AI Agent, HVAC"* | App HVAC và app DOOR cùng bind được một service | 2h | 🔴 **02/08** |
| **M4**<br>*(phần Vĩ)* | ⭐ **Xác nhận candb BTC dùng được** thay vì tự parse DBC thô — mentor: *"bạn có thể sử dụng candb BTC đã dựng và support sẵn, tiết kiệm thời gian"* | Tùng tra được signal từ candb, khỏi qua bảng tự dựng ở V1 | 1h | 🔴 **31/07** |

> **N3 + N4 là 9đ ăn chắc nhất còn lại trong barem** (baseline 3 + mức quyết định của phần team-owned 6).
> Không phải viết tính năng mới — chỉ chạy lại harness có/không rồi lập bảng.
> **Chia đôi với Tùng** (N3b, N4b) vì 9h bỏ T10 chuyển sang đây, và vì Tùng là người biết rõ phần VHAL/CAN.

---

### 🟠 LÊ ĐỨC TÙNG — VHAL & Safety (49.5h → 🚨 **60.5h**) · ~~DTC~~ *T10 đã bỏ 29/07*

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
| ~~T10~~ | 🚫 **ĐÃ BỎ 29/07 — DTC**: ~~Script Node `nydus.uds.server` + `DtcClient` ISO-TP `19 02 FF` + phân nhóm P/C/B/U + tần suất · xu hướng · tương quan~~.<br>**Lý do:** cross-vertical **không còn là dòng điểm ở Vòng 2** (bảng "+05 kết hợp 2 domain" thuộc Vòng 3). T10 chỉ ăn được ô *"Khác biệt có ý nghĩa"* **tối đa 4đ**, làm nửa vời ăn 0. **9h chuyển sang N3b + N4b + đệm.**<br>⚠️ **Không được tự bật lại sau freeze** — nếu dư giờ thì theo thứ tự ở PHẦN 6 | — | ~~9h~~ → **2.5h vào N3b/N4b, 6.5h vào đệm** | 🚫 **đã chốt** |
| **N3b** | 🆕 **Baseline Manifest — phần VHAL/CAN/Luau**: CarSky cấp sẵn property/signal nào, đội tự wire cái nào trong Script Node. Nộp cho Vĩ ghép vào bảng tổng | Mỗi property trong core flow có nhãn `provided`/`configured`/`modified`/`new` | 1.5h | 🟡 **06/08** |
| **N4b** | 🆕 **Ablation A1** — tắt `SafetyGuard`, chạy lại: "mở cửa" lúc `Speed=60` vẫn thực thi → chứng minh bỏ phần đội làm thì claim an toàn sụp | Bảng before/after có log 2 lần chạy | 1h | 🟡 **06/08** |
| **M1a** | 🚨 **SPIKE QUYỀN VHAL — việc số 1 hôm nay.** APK thường có `setProperty(HVAC_TEMPERATURE_SET)` được không, hay bị từ chối quyền `CONTROL_CAR_CLIMATE`? Nếu bị từ chối → cài privileged (`/system/priv-app` + allowlist XML, cần `adb root`/`remount`).<br>**Đây là spike S2 giao từ 26/07 mà chưa ai chốt.** Không set được property = không có 6 chặng = **đổ cả xương sống**, không riêng M1 | Một dòng logcat cho thấy property đổi thật + ghi lại cách cài đã dùng | 1.5h | 🔴 **31/07** |
| **M1** | ⭐ **`VivaCarService` — vendor car service của riêng đội** *(cùng Vĩ)*: `Service` + AIDL, giữ **một** kết nối `Car`/`CarPropertyManager`, sở hữu bảng M2, gọi `SafetyGuard`, fan-out callback cho app HVAC / DOOR / HMI.<br>**T3 không bị bỏ** — `VhalRepository` chuyển vào trong service, app nhận client AIDL mỏng (~3h trong 5h là đóng gói lại T3) | App HVAC và app DOOR **cùng bind một service**, cùng nhận callback real-time | 5h | 🔴 **02/08** |
| **M2**<br>*(phần Tùng)* | ⭐ Cột property của bảng **`intent → PropertyID + areaId + kiểu`** *(Long làm cột intent)* — mở rộng thẳng bảng T1 lên trên một tầng, thành **ba cột: PropertyID ↔ đường VSS ↔ CAN signal** | Mỗi intent `hvac_*`/`door_lock` truy được xuống tới tên signal thật | 1h | 🔴 **31/07** |
| **M3**<br>*(phần Tùng)* | Mở API service cho app HVAC + app DOOR của Dương *(xem M3 của Dương)* | Dương gọi được, không phải đọc `CarPropertyManager` trực tiếp | 1h | 🟡 **03/08** |
| **M4** | ⭐ **Đọc Script Node đã có sẵn TRƯỚC KHI viết Luau.** Bấm **Edit Script** trên **IVI Gateway** (pin `kuksa` + `vhal`) và **PWT Gateway** (pin `can` + `kuksa`) của blueprint đã clone. Mentor: *"ở phần starter kit bạn có thể check script node để truyền data giữa skycraft và kuksa data broker"* | Trả lời được: node nào giữ chặng nào, **sửa mapping hay viết mới**. Nếu đúng như đọc từ ảnh thì **T2 ngắn lại đáng kể** | 1h | 🔴 **31/07** |
| **M5** | 🆕 **CCU mô phỏng** — Script Node echo `HvacCommand` → `HvacStatus` để đóng vòng phản hồi. Mentor: *"phần CCU nếu các bạn không biết có thể giả lập nhận gửi CAN signal"*. Khai nhãn **mô phỏng** ở N5 | Đổi property → Signal Watch đổi → giá trị quay ngược lên HMI | 1.5h | 🟡 **02/08** |

> 🚨 **Tùng nhận thêm 11h và đệm về gần 0.** Đây là người phải theo dõi sát nhất từ giờ tới 03/08:
> vừa giữ đường găng cũ (T2) vừa nhận phần lớn việc mới. **M4 có thể trả lại 3–4h** nếu Script Node
> starter kit dùng được — đó là lý do M4 phải làm **hôm nay**, trước khi viết dòng Luau nào.
> Nếu M4 không giúp được: **hoãn M5 rồi tới M3 trước tiên.** Không được hoãn M1a · M1 · M2.

> **Tùng nhận N3b + N4b vì đó là phần chỉ anh ấy biết** — không ai khác nói được CarSky wire sẵn signal nào
> và `SafetyGuard` chặn theo luật gì. Đây là 9h T10 được tái đầu tư, không phải việc phát sinh thêm.
| T11 | README: **bảng Vehicle Property + CAN signal đã dùng** (guideline CDC bắt buộc) | Bảng khớp code | 2h | 🟡 06/08 |
| T12 | Chạy safety pack + báo cáo pass/fail cho slide | Có báo cáo | 3h | 🔴 **07/08** |
| T13 | Tự quay/dẫn **phần climate + safety** của mình | Có trong cả 2 video | 1h | 🔴 **08/08** & 09/08 |

---

### 🟣 VIỆT DƯƠNG — HMI & Media (48h → ⭐ **53h**)

| # | Task | Xong khi | Giờ | Deadline |
|---|---|---|---|---|
| D1 | App shell AAOS + `car-ui-lib` chạy trên Device | 3 người còn lại có chỗ cắm code | 4h | 🔴 **29/07** (cả đội chờ) |
| D2 | HMI 3 vùng Compose + 🆕 **Driver Distraction: nút lớn, tương phản rõ** (guideline CDC mục 5; tiêu chí *UX* của đề #1) | Đủ 3 vùng, không crash, chạm được khi xe chạy | 6h | 🔴 **31/07** |
| D3 | Phản chiếu HVAC **real-time** từ callback của T3 | Đổi ở GPIO Panel → HMI tự đổi | 4h | 🔴 **31/07** ⭐ khung hình chốt của video |
| D4 | Build `usb.img` FAT32 (8 mp3 + album art) → Artifact USB → mount verify | `ls` thấy file trong VM | 3h | 🟡 01/08 20:00 |
| D5 | `MediaSourceProvider` + `UsbMediaProvider` — tiêu chí *Extensibility* đề #1.<br>🆕 **Bỏ `LocalMediaProvider`** — provider thứ 3 không thêm điểm, 2 provider đã đủ chứng minh đổi được mà không sửa UI | Đổi provider không sửa UI | ~~6h~~ **4h** | 🟡 04/08 |
| D6 | Album-art `LruCache` **(bỏ disk cache)**, không ANR — tiêu chí *Hiệu năng* đề #1 | Cuộn 50 bài không giật, logcat sạch | ~~4h~~ **2h** | 🟡 05/08 |
| D7 | `MediaBrowserService`/`MediaSession` + 🆕 **shuffle · repeat · seek** (chức năng chính đề #1) | Phát/dừng/next/shuffle/repeat/tua chạy, điều khiển được bằng giọng | 6h | 🟡 05/08 |
| D8 | 🆕 **`volume_set` / `volume_adjust` + audio zone** qua `CarAudioManager`<br>*(`03-contracts.md` §5 giao MediaSkill; slide 11 cam kết "âm lượng". v1 thiếu hẳn)* | "Tăng âm lượng" đổi volume thật | 3h | 🔴 **02/08** |
| D9 | 🆕 HMI tự đơn giản hoá theo `CarUxRestrictions` (guideline CDC mục 5) | Xe chạy → UI giảm mật độ thông tin | 2h | 🟡 05/08 |
| D10 | Đóng gói APK release + smoke cài sạch | Cài từ APK trên máy khác chạy được | 2h | 🔴 **05/08** |
| D11 | Sơ đồ kiến trúc + extension points | Dùng được cho README và slide | 3h | 🟡 07/08 |
| D12 | Dựng máy quay **video 3' uncut** (Recorder Part) — chạy sạch 3 lần liên tiếp mới lấy | File `.mp4` đã tải về | 2h | 🔴 **08/08** (C3) |
| D13 | Dựng **video 5–7'** + overlay 6 khung chứng minh không mock | Bản nộp chính thức | 5h | 🔴 **09/08 20:00** |
| D14 | **Video backup dự phòng**, lưu ở mục Videos (guideline CDC mục 7) | File riêng, độc lập với Room | 1h | 🔴 **09/08 20:00** |
| **N6** | 🆕 **Artifact identity** *(với Vĩ + Long)* — version APK · commit · config · **video backup phải cùng MỘT identity** với bài nộp | 1 trang ghi rõ, 4 thứ khớp nhau | 1h | 🔴 **09/08** |
| **M6** | 🚨 **Đưa được MỘT APK lên Device AAOS hôm nay**, dù còn trống rỗng. Mentor: *"phần trên android các bạn cũng nên thử cài đặt phần AI để kiểm tra vận hành ứng dụng — ví dụ có app → triển khai trên AAOS thử xem hoạt động ntn"*.<br>**Hôm nay code voice của Long chưa từng biên dịch** (`10` PHẦN 4) — nếu D1 chưa xong thì đây chính là D1 | Ảnh app chạy trên Device + logcat sạch, dán vào nhóm | 1h | 🔴 **31/07 tối nay** |
| **M3** | ⭐ **Tách app HVAC và app DOOR thành 2 launcher entry riêng**, cùng bind `VivaCarService` (M1). Ra lệnh bằng giọng → **app tương ứng tự bật lên màn hình** và phản chiếu giá trị mới.<br>*Kick-off chốt: "cần xây dựng lên app HVAC và DOOR, sau đó tích hợp AI Agent vào service thì app phải được kích hoạt lên, hiển thị lên app"* | Nói *"hạ điều hoà 22 độ"* → app HVAC tự hiện, số đổi theo. Nói *"khoá cửa"* → app DOOR tự hiện | 4h | 🟡 **03/08** |

> **M3 không thay D2.** HMI 3 vùng vẫn là màn hình chính; M3 tách HVAC và DOOR thành **hai activity có
> launcher icon riêng** để chứng minh được cảnh *"agent kích hoạt app"* mà không phải làm lại UI.
> Đây là **khung hình demo mạnh thứ hai** sau cảnh 6 khung của D3 — và là chỗ duy nhất trong cả bài
> nhìn thấy được bằng mắt rằng có một **service** đứng giữa agent và app.

> **N6 ăn ô "Nhận diện artifact được chấm" 2đ.** L3 nguyên văn: *"Artifact, config, evidence và **video dự phòng
> cùng một identity** đã nộp."* → D14 không được là bản quay tuần trước, phải khớp đúng APK cuối.

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
| 🆕 **N7 Tổng duyệt LIVE + Q&A ×2** — chạy core flow **trực tiếp, không quay**, mỗi người bị hỏi ngược về phần mình | 06/08 · 09/08 | 3.5h |

> **N7 khác hẳn "tổng duyệt" đã có ở trên.** Tổng duyệt cũ là tập *để quay video*. N7 là tập **vận hành trực tiếp
> và trả lời câu hỏi**, kể cả khi một lệnh fail giữa chừng. Barem mới có ô *"Demo live online và độ ổn định"* **6đ**
> — L0 của ô đó nguyên văn: *"Không chạy trực tiếp do nguyên nhân thuộc phía đội"* — cộng khối *"Trình bày và
> trả lời làm rõ"* **5đ**, trong đó 4/5 ô chỉ đạt được khi có người vận hành thật. **Tổng 11đ.**
>
> Thể lệ có mục **"Demo và Q&A"** riêng, ghi *"lịch, thời lượng và hình thức được BTC thông báo riêng"* —
> đội chưa nhận thông báo này. **Long phải hỏi mentor ngay** (câu a ở `07` PHẦN 5 mục ⑬).

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

### ⭐ 31/07 — NHÁNH MỚI SAU KHI MENTOR SỬA KIẾN TRÚC

```
M1a spike quyền VHAL (31/07)  ◄── 🚨 CHẶN NẶNG NHẤT, CHƯA AI BIẾT CÂU TRẢ LỜI
   └─► M1 VivaCarService (02/08) ─┬─► M3 app HVAC + app DOOR (03/08)
          ▲                        ├─► T3 VhalRepository → đóng gói vào trong M1
          │                        └─► T4 door_lock · T7 Climate đi qua service
   M2 bảng intent→PropertyID (31/07)
          ▲
   Long (cột intent) + Tùng (cột property, nối tiếp T1)

M4 đọc Script Node có sẵn (31/07) ─► quyết định T2 "sửa mapping" hay "viết mới 8h"
                                        └─► M5 CCU mô phỏng (02/08) đóng vòng phản hồi
M6 APK lên Device (31/07 tối) ─► mọi thứ khác đều là phỏng đoán nếu chưa có mốc này
```

| Chặn | Giao | Chờ | Hạn | Nếu trễ |
|---|---|---|---|---|
| 🚨 **M1a spike quyền VHAL** | Tùng | **M1 · T3 · T4 · T7 · cả xương sống** | 🔴 31/07 | **Không có đường vòng.** Không set được property = không có 6 chặng. Nếu 21:30 chưa có câu trả lời thì đây là việc duy nhất của Tùng đêm nay, và Long hỏi mentor câu **d** ngay (`11` PHẦN 8) |
| **M2 bảng intent→PropertyID** | Long + Tùng | M1 | 🔴 31/07 | M1 code theo bảng tạm, đánh dấu `// TODO-M2` — nhưng bảng này chỉ 2h, đừng để trễ |
| **M4 đọc Script Node có sẵn** | Tùng + Vĩ | quyết định phạm vi T2 | 🔴 31/07 | Tùng viết Luau cho một topology chưa xác nhận. **Mất 30' để kiểm, rẻ hơn nhiều so với 8h viết nhầm** |
| **M1 `VivaCarService`** | Tùng + Vĩ | Dương (M3) | 🔴 02/08 | Dương làm UI 2 app trước, cắm vào service sau — không tắc hẳn |
| **M6 APK trên Device** | Dương | cả đội | 🔴 31/07 tối | Không có đường vòng. Code chưa từng biên dịch thì mọi ước lượng giờ đều là phỏng đoán |

---

## PHẦN 6 — BA MỐC CÂN BẰNG

**⚖️ 31/07 21:30 — xương sống được hay chưa?**
*"Hạ điều hòa xuống 22 độ" → 6 chặng đổi thật → HMI cập nhật → log đủ 6 mốc.*
✅ → giữ nguyên bảng PHẦN 3. ❌ → **hoãn vô điều kiện D5–D9**; Dương sang hỗ trợ Tùng, Long dừng L7.

🆕 **Câu hỏi thứ hai, bắt buộc:** *"Lần chạy vừa rồi có phải **trên CarSky** không?"*
Thể lệ mới: Digital Cockpit **phải chứng minh core flow chạy trên CarSky mới đạt từ L2** ở khối Platform
utilization. Chạy trên emulator local = **trần cứng L1**, mất phần lớn **15đ**. Nếu 31/07 chưa chạy trên
CarSky thì đó là việc số 1 của cả đội, đứng trên mọi tính năng còn lại.

⭐ **Và hai câu thứ ba, thứ tư — thêm 31/07 sau kick-off:**

| # | Câu hỏi | Nếu ❌ |
|---|---|---|
| ③ | *APK của đội đã **cài lên Device AAOS** và chạy được chưa?* (M6) | Cả đội **dừng viết tính năng** cho tới khi có một APK chạy trên Device. Code chưa từng biên dịch thì mọi ước lượng giờ còn lại đều là phỏng đoán |
| ④ | *`setProperty` HVAC từ APK của đội có **bị từ chối quyền** không?* (M1a) | 🚨 **Nặng nhất trong bốn câu.** Không set được property = không có 6 chặng = mất cả khối Demo lẫn khối Platform. Đây là việc duy nhất của Tùng đêm nay, và Long nhắn mentor câu **d** (`11` PHẦN 8) ngay trong đêm |

**⚖️ 03/08 sau C2** — ai dư > 6h nhận việc từ đường găng, **không tự mở phạm vi mới**.
🆕 **Thứ tự nhận đã đổi — bằng chứng đứng trước tính năng:**
① **N3 Baseline Manifest** → ② **N4 Ablation** → ③ T9 safety pack → ④ V11 harness v2 →
⑤ viết trước mục README của mình → ⑥ tăng độ phủ kịch bản biên cho ô *"Kịch bản đại diện và tình huống biên"* (4đ).

🚫 **T10 DTC đã bị bỏ hẳn — không nằm trong danh sách này và không được tự bật lại.** Ai dư giờ mà mở lại
DTC là đang đổi 9h lấy tối đa 4đ, trong khi ⑥ ở trên rẻ hơn và ăn đúng bằng đó.

**⚖️ 05/08 23:59 — FEATURE FREEZE.** Sau mốc này chỉ còn **5** loại việc: sửa lỗi · đo đạc · tài liệu ·
quay video · 🆕 **dựng bằng chứng** (Claim–Evidence Map, ablation, integration card, artifact identity).

---

## PHẦN 7 — LỊCH DEADLINE TỔNG HỢP

| Ngày | Long | Vĩ | Tùng | Dương |
|---|---|---|---|---|
| **28/07** | 🔴 L1 mentor 20:00 | 🔴 V1 DBC · V2 clone · V3 nodes 22:00 | 🔴 T1 bảng ánh xạ 23:00 | chuẩn bị project Android |
| **29/07** | 🔴 L2 LatencyTrace | 🔴 V4 repo · V5 dev loop · V6 image | *(bắt đầu T2)* | 🔴 D1 App shell |
| **30/07** | 🔴 L3 VAD · L4 AsrClient | 🔴 V7 node ASR chạy | 🔴 **T2 Script Node** | *(HMI)* |
| **31/07** | 🔴 L5a Intent — **5 lệnh xương sống** · ⭐ **M2** cột intent | 🔴 V8 Harness v1 · ⭐ **M4** candb | 🚨 ⭐ **M1a spike quyền · M4 đọc Script Node · M2** *(ba việc này trước T3/T4/T5)* | 🔴 D2 HMI · D3 real-time · 🚨 ⭐ **M6 APK lên Device** |
| | ⚖️ **MỐC CÂN 1 — 21:30 · ⭐ 4 CÂU HỎI, KHÔNG PHẢI 2** ||||
| **01/08** | 🟡 L6 TTS · 🆕 **N2 Product Card** | *(hỗ trợ tích hợp)* · ⭐ **M1** | ⭐ **M1 `VivaCarService`** *(việc chính cả ngày)* | 🟡 D4 usb.img 20:00 |
| **02/08** | 🔴 L7 audio focus · L8 kịch bản 3' · ⭐ **M7 5 tình huống phức tạp** | 🟡 V9 Delivery 18:00 · 🔴 V10 p50/p95 20:00 | 🟡 T6 Guard G2 · 🔴 T7 Climate 6 chặng 18:00 · ⭐ **M1 xong · M5 CCU mô phỏng** | 🔴 D8 volume · *(bắt đầu M3)* |
| | 🔴 **Tối 02/08 — tổng duyệt demo 10' cho C2** ||||
| **03/08** | 🟢 **NỘP C2** — ⚖️ MỐC CÂN 2 · ⭐ **Dương chuyển giờ sang Tùng, không phải ngược lại** | | | ⭐ **M3 app HVAC + app DOOR** |
| **04/08** | *(tối ưu latency)* | 🟡 V11 Harness v2 | 🟡 T8 test VHAL · T9 safety pack | 🟡 D5 MediaSourceProvider |
| **05/08** | 🔴 L9 p95<1.5s · L10 trục benchmark | *(hỗ trợ)* | ~~T10 DTC~~ **đã bỏ** → đệm / gỡ tắc đội | 🟡 D6 cache · D7 MediaSession · D9 UxRestrictions · 🔴 D10 APK |
| | 🚫 **23:59 FEATURE FREEZE** — *sau mốc: sửa lỗi · đo đạc · tài liệu · quay video · 🆕 dựng bằng chứng* ||||
| **06/08** | 🟡 L11 README | 🔴 V12 benchmark · 🆕 **N3a Baseline · N4a Ablation A2+A3** | 🟡 T11 bảng property · 🆕 **N3b Baseline VHAL · N4b Ablation A1** | *(chuẩn bị quay)* |
| | 🔴 **19:00 office hours cuối cùng còn kịp sửa** · 🆕 **N7a tổng duyệt LIVE lần 1** ||||
| **07/08** | 🆕 **N1 Claim–Evidence Map** | 🔴 V13 README · 🆕 **N5 bảng trạng thái integration** | 🔴 T12 báo cáo safety | 🟡 D11 sơ đồ kiến trúc |
| **08/08** | 🔴 **L12 write-up** · **L15 slide** · L13 phần voice | 🔴 V15 phần delivery | 🔴 T13 phần climate+safety | 🔴 **D12 quay 3' UNCUT** |
| | ⚫ **C3 — code freeze · slide pitch · test chạy được** ||||
| **09/08** | 🔴 L13 bản dựng · 🆕 **N6 artifact identity** | 🔴 V14 smoke + người ngoài thử README 20:00 | *(soát tài liệu)* | 🔴 D13 video 5–7' · D14 backup 20:00 |
| | 🔴 **Tối 09/08 — 🆕 N7b tổng duyệt LIVE + Q&A**, cài APK sạch, logcat không crash ||||
| **10/08** | 🔴 **L14 nộp trước 12:00** ||| |

---

## PHẦN 8 — KẾT QUẢ RÀ SOÁT (vì sao có bản v2)

### A. Ba lỗi phân vai — đã sửa

| # | Lỗi ở v1 | Nguồn đối chiếu | Sửa thành |
|---|---|---|---|
| ① | Delivery flow giao **Long** | Proposal slide 3: Vĩ phụ trách *"delivery simulator"* · `03-contracts.md` §5: `DeliverySkill` = **Vĩ** | → **V9 (Vĩ)** |
| ② | Toàn bộ video (11h) giao **Dương** | Proposal slide 3: *"video & write-up: cả đội cùng thực hiện… mỗi thành viên chịu trách nhiệm demo phần mình"* | → Dương giữ kỹ thuật quay + dựng (8h); **mỗi người tự quay/dẫn phần mình** (L13/V15/T13) |
| ③ | DTC gộp **"Tùng + Vĩ"** ở kế hoạch gốc | Proposal slide 3: Tùng phụ trách *"DTC/UDS simulator"* | ~~→ **T10 (Tùng)** trọn gói~~ · 🚫 **29/07: T10 đã bỏ hẳn theo barem mới.** Vấn đề phân vai này không còn tồn tại |

### B. Chín hạng mục thiếu — đã bổ sung

| # | Thiếu gì | Nguồn yêu cầu | Đã thêm |
|---|---|---|---|
| ④ | **`door_lock` không có task nào** | Slide 11 cam kết *"≥5 lệnh car control (**cửa**, âm lượng, media, điều hòa)"* · `03-contracts.md` §5 `BodySkill` · Safety Guard G1 vô nghĩa nếu không có lệnh mở cửa | **T4** |
| ⑤ | **`volume_*` không có task nào** | Slide 11 · `03-contracts.md` §5 giao MediaSkill · webinar đề #3 *"điều chỉnh âm lượng"* | **D8** |
| ⑥ | **`CarAudioManager` / audio focus** | Guideline CDC mục 4 liệt kê là API cốt lõi cho *"media/voice app"*. Demo có nhạc đang phát + ra lệnh + TTS trả lời → bắt buộc phải duck | **L7** + **D8** |
| ⑦ | **`CarUxRestrictionsManager`** | Guideline CDC mục 5 (*Driver Distraction*) và mục 8 (test bằng widget **Road Simulator**) · tiêu chí *UX* của đề #1 | **T5** (vào `VehicleState`) + **D2**, **D9** (HMI) |
| ⑧ | **HVAC thiếu chế độ gió & ghế sưởi** | Webinar đề #2, chức năng chính: *"nhiệt độ, tốc độ quạt, **chế độ gió** và **điều hòa ghế**"* | **T7** — thêm nếu DBC có signal; nếu không, **ghi rõ lý do trong README** |
| ⑨ | **Media thiếu shuffle/repeat/seek** | Webinar đề #1, chức năng chính: *"phát, tạm dừng, chuyển bài, **tua nhanh**, **phát ngẫu nhiên** và **lặp lại**"* | **D7** |
| ⑩ | ~~**Tương quan DTC bị cắt nhưng là tiêu chí chấm**~~ | Webinar đề #4, tiêu chí *Analysis depth*: *"tần suất, xu hướng và **tương quan** lỗi"* | 🚫 **29/07: hết hiệu lực.** Barem Vòng 2 mới **không chấm theo tiêu chí từng đề** — chỉ có một bảng 100đ cho cả sản phẩm. Quy tắc *"làm thì làm đủ 3 trục, không làm thì bỏ hẳn"* vẫn đúng, và đội **chọn bỏ hẳn** |
| ⑪ | **Testability của đề #2 không ai lo** | Webinar đề #2, 1 trong 4 tiêu chí: *"có thể kiểm chứng mà không cần phần cứng ECU thực tế"* | **T8** |
| ⑫ | **Extensibility của đề #3 không ai lo** | Webinar đề #3, 1 trong 4 tiêu chí: *"quản lý intent rõ ràng, không cần tái cấu trúc core"*. v1 chỉ có extension point cho Media | **L11** |

### C. Ba việc phải quyết, không phải việc phải code

| # | Vấn đề | Vì sao quan trọng | Ai · khi nào |
|---|---|---|---|
| ⑬ | **Đã bỏ mất câu xin `Hackathon_IVI_CDC_2026.md`.** Nó là câu #1 trong 8 câu ở `01-tin-nhan-gui-mentor.md`, nhưng không còn trong 5 câu ở `05-tra-loi-kickoff.md` | Guideline CDC nói thẳng: file này chứa *"bảng Tiêu chí chấm điểm"* và **"chính xác API/property bắt buộc"** của đề đã chọn. Đội đang đoán bằng webinar | **Long — thêm lại vào L1, 28/07** |
| ⑭ | **Cam kết "edge-only vs hybrid" không còn thực hiện được** — slide 9 và slide 11 đều hứa, nhưng tầng T2 cloud LLM đã cắt | BGK Vòng 2 chính là mentor đã đọc proposal. Hứa mà im lặng bỏ thì tệ hơn nhiều so với đổi có giải thích | **Long — L10, 05/08.** Hai lựa chọn: giữ 1 đường T2 tối giản chỉ để đo, hoặc đổi trục sang *ASR on-device vs ASR container* và nói rõ lý do trong write-up |
| ⑮ | ⚠️ **ĐÃ SỬA 29/07 — giả định cũ SAI.** ~~Cross-vertical có điểm cộng riêng ở Vòng 2~~ | Dòng *"(+05) kết hợp nguyên liệu từ 2 domain trở lên"* nằm ở **bảng Vòng 3 chung kết**, không phải Vòng 2. **Barem Vòng 2 mới không có ô cộng điểm cross-vertical nào.** Thể lệ 6.7 vẫn khuyến khích, nhưng ở Vòng 2 chỉ hiện thực hoá qua ô *"Khác biệt có ý nghĩa đối với use case"* — **4đ**, và chỉ khi DTC thật sự chạy | **Long — trong L12.** Không claim "+5đ" nữa. Viết một đoạn: DTC/UDS là nguyên liệu Vehicle Middleware, **để dành làm đòn bẩy Vòng 3** nơi nó thật sự có 5đ. Đây cũng là căn cứ bỏ T10 |

### D. ✅ "Ba bảng 100 điểm" — ĐÃ TỰ TRẢ LỜI 29/07, BỎ KHỎI DANH SÁCH HỎI MENTOR

Bản thể lệ mới đặt bảng đúng dưới tiêu đề mục, không còn trôi. Đội đoán đúng 2/3 — nhưng bảng Vòng 2 thì **đã bị thay**:

| Bảng | Nội dung | Kết luận |
|---|---|---|
| A | Ý tưởng 35 · Khả thi 30 · Hiểu đề & starter pack 20 · Năng lực đội 15 | **Vòng 1** ✔ đoán đúng |
| B | ~~Ý tưởng 25 · Kết dính 20 · Thực thi 20 · Nền tảng 15 · README 10 · Khách hàng 10~~ | 🚫 **ĐÃ BỊ XOÁ KHỎI THỂ LỆ** |
| B′ | Demo 25 · Kỹ thuật 20 · **Team-owned 25** · Platform 15 · Khách hàng 10 · Trình bày 5 | **Vòng 2** — bảng mới, kèm trọng số hạng mục con + thang L0–L3 |
| C | Tài liệu-slide 5 · Thuyết trình 10 · Trả lời BGK 10 · Sáng tạo 35 · **Demo 40** | **Vòng 3 chung kết** ✔ đoán đúng |

Giải mã đầy đủ bảng B′ ở **`08-BAREM-VONG-2-CHINH-THUC.md`**.

### D′. 🆕 Ba câu MỚI phải hỏi mentor (thay câu cũ đã tự trả lời)

| # | Câu | Vì sao gấp |
|---|---|---|
| a | **Vòng 2 có phiên demo trực tiếp + Q&A không? Lịch, thời lượng, hình thức?** Thể lệ có mục "Demo và Q&A" ghi *"BTC thông báo riêng"* mà đội chưa nhận | **11đ** (demo live 6 + trình bày 5) phụ thuộc câu này |
| b | **Claim–Evidence Map và Product & Integration Card có template mẫu không?** | Deliverable **bắt buộc** mới, thể lệ không kèm mẫu |
| c | **"Core flow chạy trên CarSky" được chấp nhận ở mức nào** — Device trong Room có đủ, hay phải kèm log/trace từ platform? | Quyết định trần điểm của cả **15đ** khối Platform utilization |

> Câu ⑬ (xin `Hackathon_IVI_CDC_2026.md`) **vẫn giữ nguyên giá trị** — thể lệ mới vẫn không có
> danh sách API/property bắt buộc theo từng đề.

---

## PHẦN 9 — VIỆC NGÀY 28/07 *(đã qua — giữ lại làm hồ sơ)*

> 📌 **Việc ngày 29/07 xem `08-BAREM-VONG-2-CHINH-THUC.md` PHẦN 9.**
> 📌 ⭐ **Việc ngày 31/07 xem `11-PHAN-HOI-MENTOR-KICKOFF-30-07.md` PHẦN 9.** Bốn đầu việc gấp nhất:
> Tùng **M1a spike quyền** + **M4 đọc Script Node có sẵn** · Dương **M6 đưa APK lên Device** ·
> Long + Tùng **M2 bảng intent→PropertyID**. Standup 21:30 chạy **4 câu hỏi** ở PHẦN 6, không phải 2.

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
