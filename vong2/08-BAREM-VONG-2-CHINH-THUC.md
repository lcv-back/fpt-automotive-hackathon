# BAREM VÒNG 2 — BẢN CHÍNH THỨC ĐÃ CẬP NHẬT
### Đọc từ 4 PDF mới tải 29/07/2026 · thay thế mọi giả định barem trong `00`, `04`, `06`, `07`

> **Nguồn:** `Automotive-Hackathon-2026-The-le.pdf` (bản mới, 1187 dòng — bản cũ trong `docs/` chỉ 645 dòng),
> `Automotive-Hackathon-2026-Regulations.pdf` (bản tiếng Anh, đối chiếu),
> `Automotive-Hackathon-2026-Terms-of-Use.pdf` + `-Thoa-thuan-su-dung.pdf` (thỏa thuận website — không ảnh hưởng kỹ thuật, xem PHẦN 8).
>
> BTC đã đăng lên mục **Regulations** của website. Đây là bản có hiệu lực (điều 9.1: *"có hiệu lực kể từ thời điểm được đăng tải"*).

---

## PHẦN 0 — MỘT CÂU: BAREM ĐỘI ĐANG DÙNG ĐÃ BỊ THAY

Đội đang lập kế hoạch theo bảng này (`04` PHẦN 4, `06` PHẦN 8 mục D, `07` PHẦN 5 mục ⑬):

> ~~Ý tưởng & insight 25 · Kết dính 20 · Chất lượng thực thi 20 · Tận dụng nền tảng 15 · README 10 · Hiểu khách hàng 10~~

**Bảng đó đã bị xoá khỏi thể lệ.** Nó có thật trong bản cũ — không phải đội đọc nhầm — nhưng bản mới thay
toàn bộ mục Vòng 2 bằng một barem khác, kèm **bảng con có trọng số từng hạng mục** và **thang evidence L0–L3**.

Hai hệ quả lớn nhất:

1. Xuất hiện khối **"Giá trị tăng thêm và phần team-owned" — 25đ**, ngang bằng khối Demo. **Kế hoạch hiện tại không có một task nào phục vụ khối này.** Đây là lỗ hổng lớn nhất.
2. Xuất hiện **2 deliverable bắt buộc mới**: **Claim–Evidence Map** và **Product & Integration Card**. Chưa ai được giao.

Tin tốt: câu hỏi *"ba bảng 100 điểm, bảng nào cho vòng nào"* ở `06` PHẦN 8 mục D **đã tự trả lời** — không phải hỏi mentor nữa. Xem PHẦN 7.

---

## PHẦN 1 — BAREM VÒNG 2 MỚI, ĐẦY ĐỦ TRỌNG SỐ

Tổng 100đ, 6 khối. Cột "đội đang ở đâu" là đánh giá của tôi theo kế hoạch `06`/`07` hiện tại.

| # | Khối | Điểm | Đội đang ở đâu |
|---|---|---|---|
| ① | Demo end-to-end và chức năng cốt lõi | **25** | 🟢 Mạnh — đúng trọng tâm kế hoạch |
| ② | Chất lượng kỹ thuật và bằng chứng thực thi | **20** | 🟢 Mạnh — harness của Vĩ, test của Tùng |
| ③ | **Giá trị tăng thêm và phần team-owned** | **25** | 🔴 **Trống hoàn toàn** |
| ④ | Platform utilization / ecosystem alignment | **15** | 🟡 Có nguyên liệu, chưa có bằng chứng đóng gói |
| ⑤ | Hiểu người dùng, khách hàng và khả năng triển khai | **10** | 🔴 Không có task nào |
| ⑥ | Trình bày và trả lời làm rõ | **5** | 🟡 Có video, thiếu phần live + Q&A |

### ① Demo end-to-end và chức năng cốt lõi — 25đ

| Hạng mục con | Trọng số | Đạt L2 khi | Đạt L3 khi |
|---|---|---|---|
| Độ đầy đủ của luồng cốt lõi | **8** | Luồng core chạy end-to-end nhưng còn một khoảng trống xác định | Toàn bộ claim cốt lõi chạy end-to-end, **không bypass phần team-owned** |
| **Demo live online và độ ổn định** | **6** | Có **một lần chạy hoàn chỉnh**, còn lỗi phụ hoặc chưa chứng minh lặp lại | **Đội tự vận hành ổn định**, có lần lặp / bằng chứng repeatability |
| Tính đúng của kết quả | **5** | Có oracle, rule, ground truth hoặc metric nhưng phạm vi hẹp | Đối chiếu bằng oracle/metric **version hoá** trên dữ liệu đại diện |
| Kịch bản đại diện và tình huống biên | **4** | Có kịch bản đại diện và **ít nhất một edge/error case** | Bộ kịch bản phủ claim chính + edge case + expected behavior |
| Minh bạch phạm vi demo | **2** | Khai đủ trạng thái trước demo, khớp kiến trúc | Nhất quán với log, artifact, version và evidence map |

> ⚠️ **"Demo live online" 6đ.** L0 nguyên văn: *"Không chạy trực tiếp do nguyên nhân thuộc phía đội."*
> Thể lệ có riêng mục **"Demo và Q&A"**: *"Đội nên chuẩn bị khả năng tự vận hành core flow và trả lời câu hỏi…
> Lịch, thời lượng và hình thức demo/Q&A được BTC thông báo riêng."*
> → **Vòng 2 nhiều khả năng có phiên demo trực tiếp, không chỉ nộp video.** Kế hoạch hiện tại chỉ có 2 video.

### ② Chất lượng kỹ thuật và bằng chứng thực thi — 20đ

| Hạng mục con | Trọng số | Ai đang lo |
|---|---|---|
| Kiến trúc và lựa chọn kỹ thuật | **5** | Long L11 · Dương D11 |
| Tích hợp và giao tiếp (contract, trace qua ranh giới, error behavior) | **5** | `03-contracts.md` + Long L2 LatencyTrace |
| Test và phương pháp kiểm chứng | **4** | Tùng T8/T9 · Vĩ V11 |
| Xử lý lỗi và khả năng quan sát | **4** | 🟡 **Yếu** — có log, chưa có failure behavior có chủ đích |
| **Nhận diện artifact được chấm** | **2** | 🔴 **Không ai** — xem N6 |

> L3 của "Nhận diện artifact": *"Artifact, config, evidence và **video dự phòng cùng một identity** đã nộp."*
> → Video backup D14 phải khớp đúng version APK + commit đã nộp, không phải bản quay tuần trước.

### ③ Giá trị tăng thêm và phần team-owned — 25đ 🔴 KHỐI ĐANG TRỐNG

| Hạng mục con | Trọng số | Yêu cầu |
|---|---|---|
| Xác lập baseline đúng | **3** | Baseline Manifest + tài sản tái sử dụng đã khai |
| Tách phần team-owned | **5** | Ranh giới **provided / configured / modified / new** rõ và xuất hiện trong core flow |
| **Mức quyết định của phần team-owned** | **6** | L3 = **counterfactual/ablation**: bỏ phần mới thì claim chính **thất bại hoặc suy giảm rõ** |
| Lợi ích so với baseline | **7** | Metric / test / workflow outcome / before-after; L3 cần so cùng điều kiện + nêu trade-off |
| Khác biệt có ý nghĩa đối với use case | **4** | L0 = *"chủ yếu reskin, packaging hoặc tái triển khai hành vi đã có"* |

**Vì sao đây là cơ hội chứ không phải gánh nặng cho VIVA:** baseline của đội cực kỳ thuận lợi để chứng minh.
CarSky starter pack **không có** ASR tiếng Việt, **không có** voice pipeline, **không có** TTS tiếng Việt,
VHAL chỉ tới qua Script Node Luau. Gần như toàn bộ core flow là `new`. Ablation cũng gần như miễn phí vì
harness của Vĩ đã có sẵn:

| Ablation | Bỏ đi cái gì | Kết quả kỳ vọng |
|---|---|---|
| A1 | Tắt `SafetyGuard` | "Mở cửa" lúc `Speed=60` vẫn chạy → claim an toàn sụp |
| A2 | Thay ASR container bằng đường cloud | p95 vượt 1500ms → claim latency sụp |
| A3 | Bỏ `VhalRepository` callback | HMI không phản chiếu real-time → claim kết dính sụp |

Ba dòng này ăn thẳng vào ô 6đ. Chi phí: chạy lại harness có sẵn, không viết thêm tính năng.

### ④ Platform utilization / ecosystem alignment — 15đ

| Hạng mục con | Trọng số | Ghi chú |
|---|---|---|
| Đường align hệ sinh thái theo track | **5** | 🚨 xem cảnh báo dưới |
| Độ sâu tích hợp vào core flow | **4** | L3 = bỏ đường tích hợp thì workflow chính thất bại |
| Evidence từ platform | **4** | *"log/trace/output **từ CarSky**"* — CSV harness của Vĩ đúng dạng này |
| Ranh giới và tính tương xứng | **2** | Không được đánh đồng node/container với ECU/vECU/in-car |

> 🚨 **CỔNG CỨNG.** Nguyên văn: *"Với Digital Cockpit, Connected Car Services và Vehicle Middleware,
> đường align là CarSky: **để đạt từ L2, đội cần chứng minh core flow của implementation chạy trên CarSky**."*
> Không chạy được core flow trên CarSky → trần cứng L1 cho cả 15đ. Đây là rủi ro nền tảng số 1 của đội.

> 📌 **Starter pack KHÔNG còn nằm trong điểm Platform utilization** ở Vòng 2:
> *"dùng hay không dùng không tự cộng hoặc trừ điểm"*. Khác hẳn barem cũ (*"Tận dụng nền tảng và starter pack 15đ"*)
> và khác Vòng 3 (vẫn còn *"+10 Tận dụng tối đa nền tảng & starter pack"*). **Đừng tối ưu cho việc "dùng nhiều starter pack".**

### ⑤ Hiểu người dùng, khách hàng và khả năng triển khai — 10đ 🔴 KHÔNG AI LO

5 hạng mục con **2đ đều nhau** — nghĩa là 10đ này rất dễ nhặt, chỉ cần viết đúng, không cần code:

| Hạng mục con | 2đ mỗi ô — trả lời được là có điểm |
|---|---|
| Người dùng và người quyết định | Phân biệt **user** (tài xế) với **buyer/process owner** (OEM? hãng vận tải?) |
| Offering và quan hệ tiếp nhận | App / service / module / SDK / tool? B2B / B2C / B2B2C / nội bộ? |
| Outcome và giả thuyết áp dụng | Vì sao đối tượng sẽ dùng, phê duyệt hoặc chi trả; **đánh dấu rõ đâu là giả định** |
| Tích hợp và phụ thuộc bên ngoài | Nêu đủ dependency + gắn trạng thái **thật / mô phỏng / kế hoạch** |
| Bước kiểm chứng tiếp theo | Next validation step + **rào cản lớn nhất** |

> Thể lệ nói rõ: *"Không bắt buộc có TAM, pricing, LOI hoặc tích hợp đối tác thật."* → Không cần làm business case.
> Chỉ cần **Product & Integration Card** viết đúng 5 ô trên. ~2h công việc cho 10đ. Tỉ lệ tốt nhất trong cả barem.

### ⑥ Trình bày và trả lời làm rõ — 5đ

Claim–Evidence Map · Dẫn dắt demo · Quản lý thời lượng · Minh bạch giới hạn · Trả lời làm rõ — **1đ mỗi ô.**
Bốn trong năm ô chỉ đạt được **trong phiên demo/Q&A trực tiếp**, không đạt được bằng video nộp.

---

## PHẦN 2 — SÁU THAY ĐỔI LÀM ĐỔI THỨ TỰ ƯU TIÊN

| # | Thể lệ mới nói gì | Kế hoạch đội phải đổi gì |
|---|---|---|
| **1** | *"**Không cộng điểm theo số lượng chức năng, màn hình, module** hoặc độ khó build tự thân"* + *"không cộng điểm theo số lượng claim"* | **Làm rộng không còn ăn điểm.** 15 intent không hơn 10 intent. Media đủ shuffle/repeat/seek không hơn media tối giản. Giờ tiết kiệm được phải đổ vào khối ③ và ⑤ |
| **2** | Khối ③ 25đ đòi baseline + ranh giới + **ablation** | Thêm N3, N4 — xem PHẦN 4 |
| **3** | 2 deliverable mới: **Claim–Evidence Map**, **Product & Integration Card** | Thêm N1, N2 |
| **4** | *"Demo live online"* 6đ + mục **"Demo và Q&A"** riêng | Thêm N7 tổng duyệt **live**, không phải quay video |
| **5** | *"Việc tự xây lại những gì đã có sẵn trong starter pack… không được cộng thêm điểm"* + *"Chạy lại hoặc đóng gói lại capability sẵn có không tự tạo Added Value cao"* | Kiểm lại: phần nào đội đang tự xây mà starter pack đã có? Nếu có thì nói rõ **lý do** trong Baseline Manifest |
| **6** | **Ba trạng thái integration** phải khai: *đã tích hợp / mô phỏng / kế hoạch* | `FakeAsrClient`, emulator, synthetic data — phải khai đúng nhãn. Khai sai = mất điểm ở cả ①(2đ), ⑤(2đ), ⑥(1đ) |

> Về **synthetic data / replay**: được dùng, nhưng *"đội phải công bố cách tạo, phạm vi và giới hạn"*.
> Bộ 20 utterance × 3 mức nhiễu của Vĩ (V12) thuộc diện này — cần một đoạn mô tả cách tạo.

---

## PHẦN 3 — DELIVERABLES CUỐI VÒNG: BẢN CŨ vs BẢN MỚI

| Bản cũ (đội đang theo) | Bản mới | Trạng thái |
|---|---|---|
| Source code | **Source snapshot hoặc commit reference** theo hướng dẫn nộp bài BTC | ✅ L14 có |
| Documentation | Documentation/README | ✅ L11 · V13 · T11 |
| Video demo 5–7' | Video demo theo format BTC | ✅ D13 |
| Write-up | Write-up | ✅ L12 |
| — | **Slide thuyết trình** | ✅ L15 (may mắn đã thêm ở bản `07`) |
| — | **Artifact / version / config** tương ứng sản phẩm demo | 🔴 **N6** |
| — | **Claim–Evidence Map** | 🔴 **N1** |
| — | **Product & Integration Card** | 🔴 **N2** |

Checklist nộp bài ở `07` PHẦN 7 hiện ghi *"Đủ 4 deliverable"* — **sai, giờ là 8.** Đã sửa trong `07`.

---

## PHẦN 4 — BẢY TASK MỚI

| # | Task | Ai | Giờ | Hạn | Ăn điểm ô nào |
|---|---|---|---|---|---|
| **N1** | **Claim–Evidence Map** — mỗi claim cốt lõi ↔ baseline ↔ phần team-owned ↔ expected result ↔ **evidence ID** | Long | 3.5h | 🔴 07/08 | ⑥ 1đ + là xương sống của ③ 25đ |
| **N2** | **Product & Integration Card** — 5 ô ở PHẦN 1 ⑤ | Long | 2h | 🟡 01/08 | ⑤ **10đ** |
| **N3a** | **Baseline Manifest — phần platform** + compile bảng tổng `provided / configured / modified / new` | Vĩ | 1.5h | 🟡 06/08 | ③ 3đ + 5đ |
| **N3b** | **Baseline Manifest — phần VHAL/CAN/Luau**: CarSky wire sẵn signal nào, đội tự wire cái nào | Tùng | 1.5h | 🟡 06/08 | ③ 3đ + 5đ |
| **N4a** | **Ablation A2 + A3** — A2 ASR container → cloud (p95 vượt 1500ms) · A3 bỏ callback `VhalRepository` (HMI mất real-time) | Vĩ | 2h | 🟡 06/08 | ③ **6đ + 7đ** |
| **N4b** | **Ablation A1** — tắt `SafetyGuard` → "mở cửa" lúc `Speed=60` vẫn chạy | Tùng | 1h | 🟡 06/08 | ③ **6đ + 7đ** |
| **N5** | Bảng **3 trạng thái integration** trong README (thật / mô phỏng / kế hoạch) + đoạn mô tả cách tạo synthetic data | Vĩ *(trong V13)* | 1h | 🔴 07/08 | ①2đ ⑤2đ ⑥1đ |
| **N6** | **Artifact identity** — version APK + commit + config + video backup **cùng một identity**, ghi vào 1 trang | Dương + Vĩ + Long | 1h | 🔴 09/08 | ② 2đ |
| **N7** | **Tổng duyệt LIVE + Q&A** ×2 — chạy core flow trực tiếp, mỗi người trả lời câu hỏi về phần mình | Cả đội *(việc chung)* | 3.5h | 🟡 06/08 · 🔴 09/08 | ① **6đ** + ⑥ 4đ |
| — | **Tổng** | | **17h** | | |

> **N7 khác tổng duyệt đã có trong `06` PHẦN 4.** Bản cũ là tổng duyệt *để quay video*. N7 là tập **chạy trực tiếp
> trước người lạ và bị hỏi ngược** — bao gồm cả tình huống một lệnh fail giữa chừng. Đây là 6đ + 4đ.

### ✅ ĐÃ THỰC HIỆN 29/07 — lấy 17h ở đâu

**Bốn thay đổi phạm vi đã áp vào `06` và `07`:**

| # | Thay đổi | Thu về | Lý do |
|---|---|---|---|
| ⭐ 1 | 🚫 **Bỏ hẳn T10 (DTC)** — không còn là "tầng điều kiện" | **9h của Tùng** | Cross-vertical **không còn là dòng điểm ở Vòng 2** (PHẦN 7). T10 chỉ ăn ô "Khác biệt có ý nghĩa" tối đa 4đ, làm nửa vời ăn 0. Đổi lấy N3+N4 = **9đ ăn thẳng** |
| 2 | Hạ L5 từ 15 intent xuống **10 intent lõi** (`03-contracts.md` §3 v2 đã sửa) | 2.5h của Long | Barem ghi thẳng: *không cộng điểm theo số lượng chức năng*. Chọn 10 chứ không phải 8 để **giữ nguyên mọi cam kết đã nộp**: slide 11 *"≥5 lệnh car control"* + 3 intent delivery của Vĩ theo slide 3 |
| 3 | Hạ media: bỏ `LocalMediaProvider` (D5), bỏ disk cache (D6) | 4h của Dương | Cùng lý do. Giữ đủ để core flow "phát nhạc + duck khi TTS nói" chạy |
| 4 | L12 write-up 5h → 3h vì **N1 thay L12a** làm dàn ý | 2h của Long | Claim–Evidence Map chính là bộ khung của write-up, không phải việc trùng |

**Kết quả cân bằng lại — 9h của Tùng KHÔNG trả về đệm mà tái đầu tư đúng chỗ:**

| Người | Task bắt buộc | Việc chung | Đệm |
|---|---|---|---|
| Long | 53h → **55h** | 16h → 19.5h | 11h → **5.5h** |
| Vĩ | 50h → **54.5h** | 16h → 19.5h | 14h → **6h** |
| Tùng | 47h → **49.5h** *(T10 bỏ, nhận N3b+N4b)* | 16h → 19.5h | 17h → **11h** |
| Dương | 51h → **48h** | 16h → 19.5h | 13h → **12.5h** |

Chênh lệch tối đa **7h**, đều hơn bản trước. Tùng giữ đệm nhiều nhất (11h) là **cố ý**: T2 Script Node Luau
8h vẫn là task rủi ro nhất đội, và anh ấy là người nhận việc đầu tiên ở mốc cân 2 nếu ai đó trễ.

Đây là **đổi phạm vi lấy bằng chứng**, không phải làm thêm giờ — đúng hướng barem mới.

---

## PHẦN 5 — SỬA BA MỐC CÂN BẰNG

**⚖️ Mốc cân 1 (31/07 21:30)** — giữ nguyên câu hỏi, **thêm một câu**:
> *"Lần chạy vừa rồi có phải trên CarSky không?"* — nếu core flow mới chỉ chạy trên emulator local thì
> Platform utilization đang bị trần L1. Đây là 15đ, phải biết sớm chứ không phải 08/08 mới phát hiện.

**⚖️ Mốc cân 2 (03/08)** — **đổi thứ tự nhận việc.** Bản cũ: ① T9 → ② V11 → ③ README → ④ T10 DTC.
Bản mới:

> ① **N3 Baseline Manifest** → ② **N4 Ablation** → ③ T9 safety pack → ④ V11 harness v2 → ⑤ README của mình
> → ⑥ tăng độ phủ kịch bản biên (ô *"Kịch bản đại diện và tình huống biên"* 4đ).

🚫 **T10 DTC đã bỏ hẳn — không nằm trong danh sách và không ai được tự bật lại.** Nếu có người đề nghị mở
lại: *9h đổi lấy tối đa 4đ, trong khi ⑥ rẻ hơn và ăn đúng bằng đó.*

**🚫 Feature freeze (05/08 23:59)** — giữ nguyên, nhưng **thêm loại việc thứ 5** vào danh sách được phép sau freeze:
> sửa lỗi · đo đạc · tài liệu · quay video · **~~và~~ dựng bằng chứng (Claim–Evidence Map, ablation, integration card)**

---

## PHẦN 6 — RỦI RO MỚI PHÁT SINH

| Rủi ro | Mức | Xử lý |
|---|---|---|
| **Core flow không chạy trên CarSky đúng hạn** → trần L1 cho 15đ | 🔴 Cao | Thêm câu hỏi vào mốc cân 1. Nếu 31/07 chưa chạy trên CarSky, đây là việc số 1 của cả đội, trên cả tính năng |
| **Có phiên demo live + Q&A mà đội chỉ tập quay video** | 🔴 Cao | N7. Và hỏi mentor lịch/hình thức demo — BTC nói *"thông báo riêng"*, chưa thấy thông báo |
| Khai sai trạng thái integration (gọi `FakeAsrClient` là "đã tích hợp") | 🟡 Vừa | N5. Thà khai "mô phỏng" mà đúng còn hơn khai "đã tích hợp" rồi bị hỏi ngược ở Q&A |
| Write-up AI vẫn quan trọng nhưng **không còn dòng điểm riêng ở Vòng 2** | 🟡 Vừa | Thể lệ 6.7 vẫn ghi *"Đây là một tiêu chí được BGK đánh giá cao"*. Giữ L12 nguyên 5h, nhưng biết rằng nó ăn điểm gián tiếp qua ③, không phải một ô riêng. Ở **Vòng 3** mới có *"+05 Câu chuyện AI ấn tượng"* |

---

## PHẦN 7 — HAI CÂU HỎI MENTOR ĐÃ TỰ TRẢ LỜI ĐƯỢC

### ✅ Câu "ba bảng 100 điểm" (`06` PHẦN 8 mục D · `07` mục ⑬) — ĐÃ RÕ, BỎ KHỎI DANH SÁCH HỎI

Bản thể lệ mới đặt bảng đúng dưới tiêu đề mục, không còn trôi:

| Bảng | Nội dung | Thuộc vòng |
|---|---|---|
| A | Ý tưởng 35 · Khả thi 30 · Hiểu đề & starter pack 20 · Năng lực đội 15 | **Vòng 1** ✔ đội đoán đúng |
| B | ~~Ý tưởng 25 · Kết dính 20 · Thực thi 20 · Nền tảng 15 · README 10 · Khách hàng 10~~ | **Đã bị xoá** |
| B′ | Demo 25 · Kỹ thuật 20 · **Team-owned 25** · Platform 15 · Khách hàng 10 · Trình bày 5 | **Vòng 2** — bảng mới |
| C | Tài liệu-slide 5 · Thuyết trình 10 · Trả lời BGK 10 · Sáng tạo 35 · **Demo 40** | **Vòng 3 chung kết** ✔ đội đoán đúng |

### ⚠️ Câu cross-vertical (`06` mục ⑮ · `07` mục ⑮) — GIẢ ĐỊNH CŨ SAI, PHẢI SỬA CÁCH VIẾT

Dòng *"(+05) Tích hợp đa dạng bài tập: kết hợp nguyên liệu từ 2 domain trở lên"* nằm ở **bảng C — Vòng 3 chung kết**,
**không phải Vòng 2**. Barem Vòng 2 mới **không có ô cộng điểm cross-vertical nào**.

Thể lệ 6.7 vẫn giữ câu *"có tiêu chí cộng điểm riêng"* và *"giải pháp kết hợp nhiều vertical sẽ được đánh giá cao"*
— nhưng ở Vòng 2 điều đó chỉ hiện thực hoá qua ô **"Khác biệt có ý nghĩa đối với use case" (4đ)**.

→ **Không đổi quyết định cắt cross-vertical. Đổi cách viết:** trong write-up, thay vì claim "+5 điểm cross-vertical",
viết một đoạn ngắn rằng DTC/UDS là nguyên liệu Vehicle Middleware và **để dành làm đòn bẩy cho Vòng 3**, nơi nó
thật sự có 5đ. Ở Vòng 2 nó không đáng 9h của Tùng.

### Vẫn phải hỏi mentor

| # | Câu | Vì sao |
|---|---|---|
| 1 | **`Hackathon_IVI_CDC_2026.md`** — vẫn xin | Thể lệ mới vẫn không có API/property bắt buộc theo từng đề. Câu này giữ nguyên giá trị |
| 2 | 🆕 **Vòng 2 có phiên demo trực tiếp + Q&A không? Lịch, thời lượng, hình thức?** | Thể lệ ghi *"BTC thông báo riêng"* mà chưa thấy. **11đ (6+5) phụ thuộc câu này** |
| 3 | 🆕 **Claim–Evidence Map và Product & Integration Card có template không?** | Deliverable bắt buộc mới, không có mẫu trong thể lệ |
| 4 | 🆕 **"Core flow chạy trên CarSky" được chấp nhận ở mức nào?** — Device trong Room có đủ, hay phải kèm trace từ platform? | Quyết định trần điểm của cả 15đ khối ④ |

---

## PHẦN 8 — HAI PDF CÒN LẠI: KHÔNG ẢNH HƯỞNG KỸ THUẬT

`Terms-of-Use.pdf` và `Thoa-thuan-su-dung.pdf` là **thỏa thuận sử dụng website** (bản Anh + Việt, nội dung
giống nhau): định nghĩa, đăng ký tài khoản, bảo mật thông tin cá nhân, hành vi bị cấm, sở hữu trí tuệ website.

Chỉ 3 điểm đáng nhớ, đều đã nằm trong thể lệ:

- **Mỗi cá nhân 01 tài khoản duy nhất**, cấm chia sẻ/mạo danh tài khoản đội khác.
- Thỏa thuận có thể được sửa đổi và **có hiệu lực từ lúc đăng tải** — nên kiểm mục Regulations định kỳ.
- Sở hữu trí tuệ: **Tác phẩm dự thi thuộc quyền sở hữu của đội** (thể lệ 4.2), nhưng đội cấp cho BTC giấy phép
  không độc quyền, miễn phí, không huỷ ngang, phạm vi toàn cầu để lưu trữ/chạy thử/đánh giá/trưng bày và
  dùng cho truyền thông. **Không có chuyển giao quyền sở hữu** — không cần lo về mã nguồn.

Nhắc lại một điều đã có trong thể lệ và vẫn nằm trong checklist nộp bài của Long:
> 3.6 — *"Các đội thắng cuộc **không được công khai nền tảng của Ban tổ chức**, chỉ được công khai giải pháp
> và bài tập do đội nhóm xây dựng."* → README public không được kèm hướng dẫn/ảnh chụp nội bộ CarSky quá chi tiết.

---

## PHẦN 9 — VIỆC PHẢI LÀM NGAY (29/07)

| Ai | Việc | Hạn |
|---|---|---|
| **Long** | Đăng file này vào nhóm, nêu ở standup: **barem đã đổi, khối 25đ đang trống** | 21:30 hôm nay |
| **Long** | Gửi mentor **3 câu hỏi mới** (#2, #3, #4 ở PHẦN 7) — ghép vào lượt hỏi đang chờ | 21:30 hôm nay |
| **Long** | ✅ **Thông báo T10 DTC đã bỏ** — quyết định đã chốt, standup chỉ cần xác nhận Tùng nhận N3b + N4b | standup 29/07 |
| **Tùng** | Xác nhận nhận **N3b + N4b** thay T10. Từ giờ **không viết dòng code DTC nào** | standup 29/07 |
| **Vĩ** | N3a/N4a về tay mình từ 06/08 — thiết kế harness V8 sao cho **bật/tắt được từng thành phần** (`SafetyGuard`, đường ASR, callback VHAL) | ghi nhớ khi làm V8 (31/07) |
| **Dương** | Biết trước D5 bỏ `LocalMediaProvider`, D6 bỏ disk cache — **đừng làm rồi mới nghe** | trước 04/08 |
| **Long** | Kiểm ở mốc cân 1: core flow đã chạy **trên CarSky** chưa | 31/07 21:30 |

> Barem đổi giữa vòng là tin xấu cho lịch, nhưng là tin tốt cho VIVA: khối 25đ mới thưởng đúng thứ đội có sẵn
> — một pipeline tự xây từ đầu trên nền tảng không có sẵn gì cho tiếng Việt. Điều đội thiếu không phải năng lực,
> mà là **viết nó ra thành bằng chứng có tên**.
