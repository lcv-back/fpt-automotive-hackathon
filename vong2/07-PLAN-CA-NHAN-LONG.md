# PLAN CÁ NHÂN — NGÔ VĂN LONG
### Vòng 2 · 28/07 → 10/08/2026 · tách từ `06-PHAN-CONG-4-NGUOI.md`

> File này chỉ chứa việc **của bạn**. Mở mỗi sáng, biết hôm nay làm gì, tối đối chiếu "xong khi".
> Việc của 3 người còn lại xem ở `06-PHAN-CONG-4-NGUOI.md` — đừng làm hộ, chỉ gỡ tắc.
>
> **🚨 Cập nhật 29/07 — BTC đã đăng barem Vòng 2 mới lên mục Regulations.**
> Barem cũ (*Ý tưởng 25 · Kết dính 20 · Thực thi 20 · Nền tảng 15 · README 10 · Khách hàng 10*) **đã bị xoá khỏi thể lệ**.
> Bảng mới: **Demo 25 · Kỹ thuật 20 · Team-owned 25 · Platform 15 · Khách hàng 10 · Trình bày 5.**
> Giải mã đầy đủ + 7 task mới ở **`08-BAREM-VONG-2-CHINH-THUC.md`** — đọc file đó trước khi làm tiếp file này.
> File này đã được cập nhật theo bản mới: thêm **N1, N2, N7** vào PHẦN 2/3, sửa mục ⑬ ⑮ ở PHẦN 5, sửa checklist PHẦN 7.

---

## PHẦN 0 — BẠN GIỮ HAI VAI, VÀ CHÚNG XUNG ĐỘT

| Vai | Nội dung | Chiếm |
|---|---|---|
| **Người làm** — Voice AI | VAD · ASR client · Intent Router · TTS · audio focus · latency · 🆕 **dựng bằng chứng (N1, N2, N6)** | 53h → **55h** |
| **Đội trưởng** | Mentor · gác 3 mốc cân bằng · gỡ tắc · điều phối demo · 🆕 **tổng duyệt LIVE (N7)** · nộp bài | 16h → **19.5h** việc chung + gánh nặng ngắt quãng |

**Quy tắc khi hai vai xung đột:** vai đội trưởng thắng ở **4 thời điểm cố định** — 21:30 mỗi tối (standup),
31/07 21:30 (mốc cân 1), 03/08 (C2), 05/08 23:59 (freeze). Ngoài 4 thời điểm đó, bạn là người làm voice
và **không nhận việc gỡ tắc kéo dài quá 30 phút** — đẩy sang standup.

Lý do: bạn nằm trên đường găng. Đội trưởng bị kéo đi làm việc vặt là cách phổ biến nhất để xương sống trễ.

---

## PHẦN 1 — ⚠️ ĐỌC TRƯỚC: 4 NGÀY ĐẦU KHÔNG VỪA LỊCH

Khi tách plan riêng tôi cộng lại giờ và thấy vấn đề:

| | 28–31/07 cần | Có (4h/ngày × 4 − kick-off − standup) | Thiếu |
|---|---|---|---|
| **Bạn** | 20h | 14h | **−6h** |
| Tùng | 24h | 14h | −10h |
| Vĩ | 24h | 14h | −10h |
| Dương | 14h | 14h | 0 |

Đích 31/07 (*"một câu, một luồng, chạy được"*) **không đạt được ở nhịp 4h/ngày**. Đây là vấn đề của cả đội,
không riêng bạn — và bạn là người phải quyết. Ba lựa chọn:

| | Cách | Đánh đổi |
|---|---|---|
| **A** *(khuyến nghị)* | **Chẻ nhỏ task, hạ đích 31/07 xuống mức tối thiểu**: chỉ cần **5 lệnh grammar** chứ không phải đủ bộ intent; phần còn lại đẩy sang cuối tuần | Không ai phải thức đêm. Đích 31/07 vẫn đúng tinh thần *"một câu, một luồng"*. **Lịch PHẦN 2 đã áp dụng cách này** |
| B | Nâng nhịp 29–31/07 lên 6h/ngày | Ba đêm liên tiếp trước cuối tuần 10h — rủi ro kiệt sức đúng lúc cần nhất |
| C | Dời xương sống sang tối 01/08 | Mất 1 ngày trong 2 ngày duy nhất trước C2. Không nên |

**Việc bạn phải làm hôm nay:** thông báo cách A ở standup, và nói rõ với Tùng + Vĩ rằng hai người đang
thiếu 10h — hai người đó cần chẻ task của mình y hệt cách bạn đã chẻ.

---

## PHẦN 2 — LỊCH NGÀY-THEO-NGÀY CỦA RIÊNG BẠN

### 🔴 T3 28/07 — 4h · *chốt mentor, khởi động đo đạc*

| Việc | Giờ | Xong khi |
|---|---|---|
| **L1** Gửi kick-off + 5 câu hỏi + báo cáo 1 trang C1. **Nhớ thêm lại 2 câu đã rơi mất** (PHẦN 5) | 2h | Đã gửi, có xác nhận đã đọc |
| Đăng `06-PHAN-CONG-4-NGUOI.md` vào nhóm, yêu cầu **mỗi người xác nhận bằng chữ** | 0.5h | 3/3 xác nhận |
| **L2a** `LatencyTrace` — dựng khung + 6 tên chặng | 1.25h | Class biên dịch được |
| Standup — nêu vấn đề sức chứa PHẦN 1, chốt cách A | 0.25h | Tùng + Vĩ đồng ý chẻ task |

### 🔴 T4 29/07 — 4h · *giao LatencyTrace cho Vĩ*

| Việc | Giờ | Xong khi | |
|---|---|---|---|
| **L2b** Log format `VIVA_TRACE\|<traceId>\|<stage>\|<nanos>` + dòng `_SUMMARY` | 1.5h | **Vĩ parse được 1 dòng mẫu** ← có người chờ | ✅ code + log mẫu `android/voice/fixtures/`, contract §1 viết lại |
| **L3a** `AudioRecord` + push-to-talk (nút giữ để nói) | 2.25h | Giữ nút → ra file wav nghe rõ | ✅ code + test · ⚠️ chưa nghe được, cần Device |
| Standup | 0.25h | | script sẵn ở `10-BAN-GIAO-L2-29-07.md` PHẦN 5 |

> ⚠️ **Chưa tick được ô "biên dịch được" của L2.** Repo chưa có project Gradle nào
> (`settings.gradle.kts` + wrapper thuộc D1), nên L2 và L3a **chưa từng compile**.
> `06` PHẦN 5 dự phòng D1 trễ bằng *"chạy module bằng unit test"* — nhưng unit test cũng cần
> project Gradle. Đây là việc số 1 tối nay, xem `10-BAN-GIAO-L2-29-07.md` PHẦN 4.

### 🔴 T5 30/07 — 4h · *VAD + kick-off mentor 19:00*

| Việc | Giờ | Xong khi |
|---|---|---|
| **L3b** Silero VAD ONNX — nạp model, cắt đoạn nói | 2.75h | Cắt đúng điểm bắt đầu/kết thúc |
| **Kick-off mentor** — demo ≤5', còn lại làm quen + hỏi đáp | 1h | 5 câu hỏi có câu trả lời hoặc có hẹn trả lời |
| Standup | 0.25h | |

> Nếu Vĩ giao `viva-asr` đúng hạn tối nay, **đừng đổi endpoint hôm nay** — để mai. Tối nay giữ stub.

### 🔴 T6 31/07 — 4h · *ngày xương sống*

| Việc | Giờ | Xong khi |
|---|---|---|
| **L3c** Tinh chỉnh ngưỡng VAD | 1h | Không cắt sớm khi nói chậm |
| **L4** `AsrClient` + `FakeAsrClient` stub + đổi sang endpoint thật của Vĩ | 1.75h | Nói → ra text tiếng Việt |
| **L5a** Grammar **5 lệnh xương sống** (đủ để demo, chưa cần 15) | 1h | 5/5 câu ra đúng intent |
| **⚖️ MỐC CÂN 1 — 21:30** — vai đội trưởng, xem PHẦN 4 | 0.25h | Có phán quyết ĐƯỢC / CHƯA |

### 🟡 T7 01/08 — 10h · *ngày dài thứ nhất*

| Việc | Giờ |
|---|---|
| **L5b** ~~Đủ 15 intent~~ → nốt **5 intent còn lại** cho đủ **10 intent lõi** (`03-contracts.md` §3 v2) + unit test *(barem mới: không cộng điểm theo số lượng chức năng)* | 2.5h |
| **L6** `TtsSpeaker` + pre-render ~30 câu vào `res/raw/` | 4h |
| 🆕 **N2 Product & Integration Card** — 5 ô: user vs buyer · offering & quan hệ tiếp nhận · outcome + giả định · dependency có nhãn thật/mô phỏng/kế hoạch · bước kiểm chứng tiếp theo | 2h |
| Đệm / gỡ tắc cho đội | 1.75h |
| Standup | 0.25h |

> **N2 là task lãi nhất trong cả plan của bạn: 2h lấy 10đ.** Làm sớm hôm nay chứ đừng để cuối vòng — nó là
> khung cho cả write-up L12 lẫn slide L15. Thể lệ nói rõ *không cần* TAM, pricing hay LOI, chỉ cần 5 ô trên.

### 🟡 CN 02/08 — 10h · *ngày dài thứ hai · chốt kịch bản*

| Việc | Giờ | Xong khi |
|---|---|---|
| **L7** Audio focus `CarAudioManager` — duck nhạc khi TTS nói | 3h | Nhạc đang phát + ra lệnh + TTS trả lời, không chồng tiếng |
| **L8** **Chốt kịch bản demo 3' uncut** — ≤6 lệnh, có lời thoại, có đường thoát khi 1 lệnh fail | 2h | Văn bản + đã chạy thử 1 lần |
| **Tổng duyệt demo 10' cho C2** (cả đội) | 3h | Chạy trọn không dừng |
| Đệm | 1.75h | |

> L8 là task **rẻ nhất mà đắt nhất** trong plan của bạn. Chốt hôm nay = có 6 ngày tập.
> Để tới 08/08 mới nghĩ kịch bản = quay 3' uncut trong lo lắng.

### 🟢 T2 03/08 — 4h · **C2**

| Việc | Giờ |
|---|---|
| Nộp demo 10' end-to-end + số đo đầu tiên của Vĩ | 2h |
| **⚖️ MỐC CÂN 2** — phân bổ lại người dư giờ (PHẦN 4) | 0.5h |
| Đệm | 1.5h |

### 🔵 T3 04/08 — 4h

| Việc | Giờ |
|---|---|
| **L9a** Bắt đầu tinh chỉnh latency — đo từng chặng, tìm chặng chậm nhất | 2.5h |
| **Office hours mentor** | 1h |
| Standup | 0.25h |

### 🔵 T4 05/08 — 5h · **FEATURE FREEZE 23:59**

| Việc | Giờ | Xong khi |
|---|---|---|
| **L9b** Đạt **p95 < 1500ms** đường edge | 2.5h | **Số của Vĩ xác nhận**, không phải bạn tự đo |
| **L10** Chốt trục so sánh thay *"edge vs hybrid"* đã hứa ở slide 9/11 (PHẦN 5) | 2h | Có quyết định + 1 đoạn giải thích |
| **⚖️ Tuyên bố FREEZE lúc 23:59** — vai đội trưởng | 0.25h | Cả 3 người xác nhận |

### 🟣 T5 06/08 — 5h *(tăng 1h)*

| Việc | Giờ |
|---|---|
| **L11** README: kiến trúc + voice pipeline + **extension point thêm intent mới không sửa core** | 3h |
| **Office hours — buổi cuối cùng còn kịp sửa gì đó** | 1h |
| 🆕 **N7a Tổng duyệt LIVE lần 1** — chạy core flow trực tiếp, **không quay**, mỗi người bị hỏi ngược về phần mình | 1h |

### 🟣 T6 07/08 — 4h · *ngày dựng bằng chứng*

| Việc | Giờ | Xong khi |
|---|---|---|
| 🆕 **N1 Claim–Evidence Map** — mỗi claim cốt lõi ↔ baseline ↔ phần team-owned ↔ expected result ↔ **evidence ID** | 3.5h | Mỗi claim trỏ được tới 1 file log/CSV/ảnh có tên |
| Standup | 0.25h | |

> **N1 thay chỗ L12a, không phải thêm vào.** Claim–Evidence Map chính là dàn ý của write-up — làm nó trước
> thì L12 hôm sau viết nhanh hơn nhiều. Đây cũng là thứ BGK dùng để chấm khối team-owned 25đ.

### ⚫ T7 08/08 — 10h · **C3**

| Việc | Giờ |
|---|---|
| **L12** Write-up hoàn chỉnh: câu chuyện AI (prompt · AI đúng ở đâu · **sai ở đâu** · MCP-driven testing) + đoạn cross-vertical đã sửa cách viết (PHẦN 5 ⑮) | 3h |
| **L15 Slide pitch** cho C3 | 3h |
| **L13a** Quay video 3' uncut — bạn dẫn chuyện, tự demo phần voice | 2h |
| Tổng duyệt: cài APK sạch → chạy full kịch bản → logcat không crash | 2h |

### ⚫ CN 09/08 — 10h · *bản nộp chính thức*

| Việc | Giờ |
|---|---|
| **L13b** Dẫn chuyện bản dựng 5–7' (Dương dựng) | 2h |
| Đọc lại toàn bộ README + write-up như người ngoài | 2h |
| 🆕 **N6 Artifact identity** (với Vĩ + Dương) — version APK · commit · config · **video backup phải cùng một identity** với bài nộp | 1h |
| 🆕 **N7b Tổng duyệt LIVE + Q&A lần cuối** — chạy trực tiếp, có người đóng vai BGK hỏi ngược | 2.5h |
| Đệm — đây là quỹ dự phòng cuối cùng, đừng tiêu sớm | 2.5h |

### 🏁 T2 10/08 — 3h · **NỘP TRƯỚC TRƯA**

| Giờ | Việc |
|---|---|
| 10:00 | **L14** Push repo cuối · soát `.env` / API key **không** bị commit |
| 11:00 | Đối chiếu checklist nộp bài (PHẦN 7) · nộp |
| 12:00 | **Đã nộp xong.** 12 tiếng còn lại là bảo hiểm, không phải thời gian làm việc |

---

## PHẦN 3 — 15 TASK CỦA BẠN, GOM MỘT CHỖ

| # | Task | Giờ | Deadline | Có ai chờ? |
|---|---|---|---|---|
| L1 | Kick-off + 5 câu hỏi + báo cáo C1 | 2h | 🔴 28/07 20:00 | mentor |
| L2 | `LatencyTrace` + log format | 3h | 🔴 29/07 | **Vĩ** |
| L3 | Push-to-talk + VAD Silero ONNX | 6h | 🔴 30/07 *(chẻ a/b/c)* | — |
| L4 | `AsrClient` + stub + đổi endpoint thật | 3h | 🔴 31/07 | — |
| L5 | Intent T0 grammar — 5 lệnh *(31/07)* → ~~15~~ **10 intent lõi** *(01/08)* | ~~6h~~ **3.5h** | 🔴 31/07 / 🟡 01/08 | — |
| L6 | `TtsSpeaker` + pre-render 30 câu | 4h | 🟡 01/08 | — |
| L7 | Audio focus `CarAudioManager` | 3h | 🔴 02/08 | — |
| L8 | Chốt kịch bản demo 3' uncut | 2h | 🔴 02/08 | **cả đội** |
| L9 | Latency p95 < 1500ms | 5h | 🔴 05/08 | — |
| L10 | Chốt trục thay "edge vs hybrid" | 2h | 🔴 05/08 | — |
| L11 | README kiến trúc + voice + extension point | 3h | 🟡 06/08 | Vĩ (lắp ghép) |
| L12 | Write-up câu chuyện AI + cross-vertical *(dàn ý đã có sẵn từ N1)* | ~~5h~~ **3h** | 🔴 08/08 | — |
| L13 | Dẫn chuyện + tự demo phần voice, 2 video | 4h | 🔴 08–09/08 | Dương |
| L15 | **Slide pitch cho C3** | 3h | 🔴 08/08 | — |
| L14 | Checklist nộp + push cuối | 2h | 🔴 10/08 11:00 | — |
| | *— dưới đây là task sinh ra từ barem mới, xem `08` —* | | | |
| **N1** | 🆕 **Claim–Evidence Map** | 3.5h | 🔴 07/08 | **cả đội** (dùng cho slide + Q&A) |
| **N2** | 🆕 **Product & Integration Card** | 2h | 🟡 01/08 | — |
| **N6** | 🆕 **Artifact identity** (cùng Vĩ + Dương) | 1h | 🔴 09/08 | — |
| — | **Tổng task cá nhân** | ~~53h~~ **55h** | | |
| **N7** | 🆕 **Tổng duyệt LIVE + Q&A** ×2 — *tính vào **việc chung**, không vào 55h trên* | 3.5h | 🟡 06/08 · 🔴 09/08 | **cả đội** |

**Tổng cộng +5.5h so với bản cũ:** task cá nhân 53h → **55h**, việc chung 16h → **19.5h**.
Lấy từ quỹ đệm (**11h → 5.5h**). Không phải làm thêm giờ, nhưng đệm mỏng đi thật —
nếu mốc cân 1 (31/07) đỏ thì phải cắt tiếp, và **đừng cắt vào N1/N2/N7**.

> **L15 là task mới.** C3 (08/08) yêu cầu *"test chạy được + video 3' uncut + **slide pitch**"* nhưng bản
> `06` chưa giao slide cho ai. Bạn giữ vì đã sở hữu write-up và kịch bản demo — dùng lại cùng nội dung.
> **Thể lệ mới xác nhận bạn giữ đúng:** slide giờ là deliverable bắt buộc của Vòng 2, không chỉ của C3.
>
> **Bốn task N ăn thẳng vào 3 khối yếu nhất:** N1 → team-owned 25đ · N2 → khách hàng 10đ ·
> N7 → demo live 6đ + trình bày 5đ. Cộng lại **46đ** đang không có ai lo trước ngày 29/07.

---

## PHẦN 4 — VIỆC ĐỘI TRƯỞNG (không phải code)

### Bốn thời điểm bạn bắt buộc đứng ra

**① Standup 21:30 mỗi tối — 15 phút, đúng 3 câu**
hôm qua xong gì · hôm nay làm gì · **đang bị ai chặn**.
Việc của bạn: nghe câu thứ ba. Ai bị chặn > 4h mà không nói ra là lỗi quy trình, nhắc ngay.

**② ⚖️ 31/07 21:30 — Mốc cân 1**
Câu hỏi duy nhất: *nói "Hạ điều hòa xuống 22 độ" → 6 chặng đổi thật → HMI cập nhật → log đủ 6 mốc. ĐƯỢC hay CHƯA?*

🆕 **Và một câu thứ hai, mới, bắt buộc hỏi:** *"Lần chạy vừa rồi có phải **trên CarSky** không?"*
Thể lệ mới: Digital Cockpit **phải chứng minh core flow chạy trên CarSky mới đạt từ L2** ở khối Platform
utilization. Chạy trên emulator local = trần cứng L1 = mất phần lớn 15đ. Nếu 31/07 chưa chạy trên CarSky
thì đó là việc số 1 của cả đội, **đứng trên mọi tính năng còn lại**.

| | Bạn làm gì |
|---|---|
| ✅ ĐƯỢC | Giữ nguyên `06`. Dương bắt đầu media từ 01/08 |
| ❌ CHƯA | **Hoãn vô điều kiện D5–D9.** Dương sang hỗ trợ Tùng. Bạn dừng L7. Cả 4 dồn tới khi xanh<br>*(T10 DTC không còn trong danh sách — đã bỏ hẳn 29/07)* |

Đây là quyết định khó nhất của bạn trong 13 ngày. Đừng "để mai xem sao" — nói ra ngay tối 31/07.

**③ ⚖️ 03/08 sau C2 — Mốc cân 2**
Ai dư > 6h thì nhận việc từ đường găng của người khác, **không tự mở phạm vi mới**.
🆕 **Thứ tự nhận đã đổi theo barem mới** — bằng chứng đứng trước tính năng:
① **N3 Baseline Manifest** → ② **N4 Ablation** → ③ T9 safety pack của Tùng → ④ V11 harness v2 của Vĩ →
⑤ viết trước mục README của mình → ⑥ tăng độ phủ kịch bản biên (ô *"Kịch bản đại diện và tình huống biên"* 4đ).

🚫 **T10 DTC đã bỏ hẳn — không nằm trong danh sách và không ai được tự bật lại.** Nếu có người đề nghị
mở lại ở mốc cân 2, câu trả lời của bạn: *9h đổi lấy tối đa 4đ, trong khi ⑥ rẻ hơn và ăn đúng bằng đó.*

**④ 🚫 05/08 23:59 — Tuyên bố FEATURE FREEZE**
Sau mốc này chỉ còn **5** loại việc: sửa lỗi · đo đạc · tài liệu · quay video ·
🆕 **dựng bằng chứng** (Claim–Evidence Map, ablation, integration card).
Ai viết tính năng mới sau mốc này là **rủi ro cho cả đội, không phải nỗ lực thêm**. Bạn phải nói câu đó ra.

### Bạn đang gác cửa cho ai

| Người | Task dài nhất của họ | Bạn canh gì |
|---|---|---|
| **Tùng** | T2 Script Node Luau 8h, hạn 30/07 | **Đây là task rủi ro nhất cả đội** — 8h liền, cả 3 người còn lại chờ. Hỏi tiến độ vào tối 29/07, đừng đợi tới 30/07 |
| **Vĩ** | V6+V7 container ASR, hạn 30/07 | Bạn có stub nên không tắc. Nhưng nếu 31/07 vẫn chưa có ASR thật thì demo C2 mất một nửa sức thuyết phục |
| **Dương** | D1 App shell, hạn 29/07 | Cả 3 người chờ. Deadline sớm nhất trong nhóm, và đang là ngày thứ hai — kiểm tra tối 28/07 xem đã dựng project chưa |

---

## PHẦN 5 — BA QUYẾT ĐỊNH CHỈ BẠN RA ĐƯỢC

### ⑬ Xin lại `Hackathon_IVI_CDC_2026.md` — **hôm nay, trong L1**

Nó là câu #1 trong 8 câu ở `01-tin-nhan-gui-mentor.md` nhưng **đã rơi khỏi 5 câu** ở `05-tra-loi-kickoff.md`.
Guideline CDC nói thẳng file này chứa *"bảng Tiêu chí chấm điểm"* và **"chính xác API/property bắt buộc"**
của đề đã chọn. Đội đang đoán bằng slide webinar.

~~Hỏi kèm luôn câu về barem ba bảng 100 điểm.~~ ✅ **Câu barem đã tự trả lời — bỏ khỏi danh sách hỏi.**
Thể lệ bản mới (29/07) đặt bảng đúng dưới tiêu đề mục: bảng *Demo 40đ* là **Vòng 3 chung kết**, không phải Vòng 2.
Bảng đội đang giả định cho Vòng 2 thì **đã bị xoá hẳn** và thay bằng bảng khác. Chi tiết `08-BAREM-VONG-2-CHINH-THUC.md`.

**Thay bằng 3 câu mới, gửi cùng lượt hỏi đang chờ:**

| # | Câu | Vì sao gấp |
|---|---|---|
| a | **Vòng 2 có phiên demo trực tiếp + Q&A không? Lịch, thời lượng, hình thức?** Thể lệ ghi *"BTC thông báo riêng"* mà đội chưa nhận được | **11đ** (demo live 6 + trình bày 5) phụ thuộc câu này. Nếu có thì kịch bản L8 phải thiết kế cho người vận hành, không chỉ cho máy quay |
| b | **Claim–Evidence Map và Product & Integration Card có template mẫu không?** | Deliverable bắt buộc mới, thể lệ không kèm mẫu |
| c | **"Core flow chạy trên CarSky" được chấp nhận ở mức nào** — Device trong Room có đủ, hay phải kèm log/trace từ platform? | Quyết định trần điểm của cả **15đ** khối Platform utilization |

### ⑭ Cam kết *"edge-only vs hybrid"* — **L10, hạn 05/08**

Slide 9 và slide 11 đều hứa so sánh này. Tầng T2 cloud LLM đã cắt → không còn "hybrid" để so.
**BGK Vòng 2 chính là mentor đã đọc proposal của bạn.** Hứa rồi im lặng bỏ tệ hơn nhiều so với đổi có giải thích.

| Lựa chọn | Chi phí | Đánh giá |
|---|---|---|
| Giữ 1 đường T2 tối giản chỉ để đo | ~4h của Vĩ sau freeze | Giữ đúng chữ đã hứa, nhưng đi ngược quyết định cắt |
| **Đổi trục sang *ASR on-device vs ASR container*** | 0h thêm — Vĩ đã có cả hai | **Khuyến nghị.** Vẫn là so sánh hai đường có thật, vẫn ra p50/p95, và giải thích được: *"chúng tôi bỏ cloud vì cam kết 1,5s và vì mất mạng"* |

Dù chọn gì, **viết ra trong write-up**. Đó là chỗ ăn điểm, không phải chỗ giấu.

### ⑮ Claim cross-vertical — ⚠️ **GIẢ ĐỊNH CŨ SAI, ĐÃ SỬA** — trong L12, hạn 08/08

~~Bảng chấm có dòng *"(+05) kết hợp nguyên liệu từ 2 domain trở lên"*.~~
Dòng đó nằm ở **bảng Vòng 3 chung kết**, không phải Vòng 2. **Barem Vòng 2 mới không có ô cộng điểm
cross-vertical nào.** Thể lệ 6.7 vẫn khuyến khích, nhưng ở Vòng 2 nó chỉ hiện thực hoá qua ô
*"Khác biệt có ý nghĩa đối với use case"* — **4đ**, và chỉ khi DTC thật sự chạy.

**Hệ quả — đây là lý do 🚫 ĐÃ CHỐT BỎ T10 DTC (29/07), 9h của Tùng chuyển sang N3b + N4b:**

| So sánh | Nếu làm T10 | Nếu đổi 9h đó lấy N3 + N4 |
|---|---|---|
| Ăn được | tối đa 4đ, và chỉ khi chạy đủ 3 trục | **9đ** (baseline 3 + mức quyết định 6), gần như chắc chắn |
| Rủi ro | 9h vào tầng điều kiện, làm nửa vời ăn 0 | Chạy lại harness đã có, không viết tính năng mới |

→ **Không claim "+5đ cross-vertical" ở Vòng 2 nữa.** Trong write-up viết một đoạn ngắn: DTC/UDS là nguyên liệu
Vehicle Middleware, đội **để dành làm đòn bẩy cho Vòng 3** nơi nó thật sự có 5đ. Nói ra chủ động vẫn ăn điểm
minh bạch; im lặng mới mất.

---

## PHẦN 6 — BẠN CHỜ AI, AI CHỜ BẠN

```
BẠN GIAO:
  L2 LatencyTrace (29/07) ─────► Vĩ V8 Harness v1 (31/07) ─► V10 số cho C2 (02/08)
  L8 kịch bản 3' (02/08) ──────► cả đội tập 6 ngày
  L11 README (06/08) ──────────► Vĩ V13 lắp ghép (07/08)

BẠN CHỜ:
  Vĩ V7 node ASR (30/07) ──────► L4 đổi stub sang thật
       └─ KHÔNG TẮC: đã có FakeAsrClient trong 03-contracts.md
  Dương D1 App shell (29/07) ──► chỗ cắm code voice
       └─ TẮC THẬT: nếu trễ, chạy module bằng unit test, ghép sau
  Tùng T3 VhalRepository (31/07) ─► không tắc bạn, tắc demo
```

Chỉ có **một** thứ chặn bạn thật sự: **App shell của Dương, hạn 29/07.** Kiểm tra tối 28/07.

---

## PHẦN 7 — CHECKLIST NỘP BÀI (10/08, việc của bạn)

- [ ] Build release/debug không lỗi
- [ ] Đã cài và test trên đúng Device của đội trên CarSky
- [ ] Đã smoke test qua Signal Watch / GPIO Panel, không crash
- [ ] **Đã quay video demo dự phòng** (guideline CDC bắt buộc)
- [ ] Repo Git đầy đủ README, đã push trước deadline
- [ ] **Ghi rõ nguồn mọi thư viện mã nguồn mở** ngoài AAOS SDK
- [ ] `.env` / API key **không** nằm trong repo
- [ ] README có **bảng Vehicle Property + CAN signal đã dùng** (Tùng giao, bạn kiểm)
- [ ] Write-up có đủ: prompt đã dùng · AI hỗ trợ tốt ở đâu · **AI sai ở đâu** · MCP-driven testing
- [ ] Đã xử lý cam kết ⑭ (edge vs hybrid) bằng văn bản

### 🆕 Đủ **8** deliverable — thể lệ mới, không phải 4

- [ ] ① **Artifact / version / config** tương ứng sản phẩm demo — *Dương + Vĩ, N6*
- [ ] ② **Source snapshot hoặc commit reference** theo hướng dẫn nộp bài BTC — *Long, L14*
- [ ] ③ Documentation / README — *Long L11 · Vĩ V13 · Tùng T11*
- [ ] ④ **Write-up** — *Long, L12*
- [ ] ⑤ **Slide thuyết trình** — *Long, L15*
- [ ] ⑥ **Video demo** theo format BTC — *Dương D13, backup D14*
- [ ] ⑦ 🆕 **Claim–Evidence Map** — claim ↔ baseline ↔ team-owned ↔ expected result ↔ evidence ID — *Long, N1*
- [ ] ⑧ 🆕 **Product & Integration Card** — offering · user · buyer/process owner · dependency · next validation step — *Long, N2*

### 🆕 Ba thứ dễ mất điểm oan, soát trước khi nộp

- [ ] **Ba trạng thái integration đã khai đúng nhãn** — *đã tích hợp / mô phỏng / kế hoạch*. `FakeAsrClient`,
      emulator, synthetic data phải nằm ở nhãn "mô phỏng", không được khai là "đã tích hợp"
- [ ] **Synthetic data đã công bố cách tạo, phạm vi và giới hạn** (bộ 20 utterance × 3 mức nhiễu của Vĩ)
- [ ] **Video backup cùng một identity** với APK + commit + config đã nộp — không phải bản quay tuần trước
- [ ] README public **không lộ chi tiết nội bộ nền tảng CarSky** (thể lệ 3.6: chỉ được công khai giải pháp của đội)
