# PHẢN HỒI MENTOR + TAKE-NOTE KICK-OFF 30/07
### Ghi ngày 31/07/2026 · nguồn: tin nhắn anh Đức trong nhóm + buổi kick-off tối 30/07 + ảnh blueprint `Demo`

> **File này là nguồn.** `06-PHAN-CONG-4-NGUOI.md` và `07-PLAN-CA-NHAN-LONG.md` đã được sửa theo nó
> (PHẦN 10 của `06`, PHẦN 8 của `07`). Ai thấy mâu thuẫn giữa các file thì **file này thắng**, vì nó
> chép lại nguyên văn lời mentor.
>
> ⚠️ Đọc file này **trước mốc cân 1 tối nay 31/07 21:30**. Nó đổi câu hỏi phải hỏi ở mốc cân.

---

## PHẦN 0 — SÁU MƯƠI GIÂY: BỐN THỨ ĐỔI

| # | Đổi gì | Ai đụng | Hạn |
|---|---|---|---|
| ① | **Luồng đội vẽ bị thiếu một tầng.** Mentor sửa: intent **không** đi tới VHAL. Nó phải được dịch thành **PropertyID** ở tầng *service fw* trước đó | Long · Tùng | 🔴 **31/07** |
| ② | **Đội chưa có "service fw".** `VhalRepository` (T3) là library trong app, không phải framework service. Phải dựng **vendor car service của riêng đội** | Tùng + Vĩ | 🔴 **02/08** |
| ③ | **T2 (đường găng dài nhất) có thể ngắn đi nhiều.** Starter kit đã có Script Node truyền dữ liệu Skycraft ↔ KUKSA, và BTC đã dựng sẵn CAN DB. **CCU được phép giả lập** | Tùng + Vĩ | 🔴 **31/07** |
| ④ | **Chỗ ăn điểm mentor nhấn mạnh: "con AI xử lý độ phức tạp thế nào"** — không phải số lượng lệnh. Đội đang không có task nào cho việc này | Long | 🔴 **02/08** |

---

## PHẦN 1 — NGUYÊN VĂN VÀ GIẢI MÃ

Cột trái là **lời mentor, không diễn giải**. Cột phải là thứ đội phải làm — nếu bạn thấy cột phải suy
diễn quá xa cột trái thì cãi lại ngay, đừng để nó thành kế hoạch.

| # | Mentor nói | Nghĩa với đội | Task |
|---|---|---|---|
| 1 | *"Đường găng dài nhất là Script Node Luau nối VHAL ↔ CAN, hạn 30/07 — trượt cái này là trượt cả demo"* | Mentor **xác nhận** đánh giá rủi ro ở `06` PHẦN 5 là đúng. Không phải góp ý mới, là phê chuẩn | T2 giữ nguyên ưu tiên số 1 |
| 2 | *"Ở phần starter kit bạn có thể check script node để truyền data giữa skycraft và kuksa data broker"* | **Đội không phải viết từ đầu.** Có mẫu sẵn cho đúng chặng khó nhất: guest AAOS (Skycraft) ↔ KUKSA | **M4** |
| 3 | *"Bạn có thể sử dụng candb BTC đã dựng và support sẵn — tiết kiệm thời gian"* | Không phải tự parse DBC thô. V1/T1 rút ngắn | **M4** |
| 4 | *"Phần trên android các bạn cũng nên thử cài đặt phần AI để kiểm tra vận hành ứng dụng — ví dụ các bạn có app → triển khai trên AAOS thử xem hoạt động ntn"* | **Cài sớm, đừng để tích hợp vào cuối vòng.** Hôm nay 31/07 code voice của Long **chưa từng biên dịch** (`10` PHẦN 4) — đây là lời cảnh báo đúng chỗ đau nhất | **M6** |
| 5 | *"Làm sao triển khai phần fw server / fw service"* + link [AAOS 101 Day3 — Car Framework Core](https://source.android.com/static/docs/automotive/car-framework-core/gapb-2024-aaos-101-day3-carframework-core.pdf) | Đây là **câu hỏi mentor đặt ngược cho đội**, không phải gợi ý. Đội phải trả lời được: service fw của đội là cái gì, cài lên AAOS bằng cách nào | **M1** · PHẦN 3 |
| 6 | *"Luồng chạy này của các bạn chưa đủ — không có phần vhal nào nhận intent cả"* | ⭐ **Lỗi kiến trúc.** Xem PHẦN 2 | **M2** |
| 7 | *"Chính xác thì luồng các bạn cần xử lý như thế này: (Agent → STT → command) APP → service fw → PropertyID ← vhal → CAN signal → CCU"* | Ranh giới giữa app và VHAL là **PropertyID**, không phải intent | **M1 + M2** |
| 8 | *"Phần CCU nếu các bạn không biết có thể giả lập nhận gửi CAN signal"* | **Được phép mô phỏng CCU.** Gỡ rủi ro cuối đường T2/T7 — nhưng phải khai nhãn *"mô phỏng"* ở N5, không được khai *"đã tích hợp"* | **M5** |

### Take-note buổi kick-off (cả nhóm ghi)

| # | Ghi được gì | Nghĩa với đội | Task |
|---|---|---|---|
| 9 | *"Vĩ và Tùng hợp tác với nhau xử lý phần ứng dụng bắt tay với AI Agent, HVAC"* | Khớp với M1 — **chỗ "bắt tay" chính là service fw**. Hai người đồng sở hữu | **M1** |
| 10 | *"Anh Dương làm phần Media"* | Không đổi so với `06` | — |
| 11 | *"Làm thế nào để sửa đổi AAOS, xây dựng vendor car service của riêng mình, thao tác nó với app HVAC mới của nhóm (có thể không đầy đủ như Google, nhưng có chức năng cơ bản)"* | **"Của riêng mình"** là từ khoá ăn thẳng vào ô *Tách phần team-owned* **5đ** và *Mức quyết định của phần team-owned* **6đ** | **M1** |
| 12 | *"Cần xây dựng lên app HVAC và DOOR, sau đó tích hợp AI Agent vào service thì app phải được kích hoạt lên, hiển thị lên app"* | Không phải 1 HMI 3 vùng — là **2 app có mặt riêng**, và **ra lệnh bằng giọng thì app tương ứng phải bật lên màn hình**. Đây là khung hình demo mạnh | **M3** |
| 13 | *"AI + media: chuyển bài, phát bài…"* | Đã có sẵn `media_play` / `media_next` / `media_pause` trong `03-contracts.md` §3. **Không phát sinh giờ** — chỉ phải đảm bảo có trong kịch bản demo L8 | ghi vào **L8** |
| 14 | *"Ăn điểm, chìa khoá: con AI sẽ xử lý độ phức tạp ntn / xử lý AI cho các tình huống"* | ⭐ Xác nhận quyết định 29/07 (cắt 15 → 10 intent) là **đúng hướng**, và đòi thêm một bước: **chiều sâu tình huống**. Đội chưa có task nào | **M7** |
| 15 | *"Biến từ ý tưởng thành sản phẩm phải làm ntn"* · *"AI Agent cần gì trên CarSky"* | Chính là **N2 Product & Integration Card** (01/08) và **N1 Claim–Evidence Map** — đã có task, giữ nguyên ưu tiên | N1 · N2 |

---

## PHẦN 2 — ⭐ LUỒNG ĐÚNG: INTENT CHẾT Ở TẦNG SERVICE, KHÔNG ĐI XUỐNG VHAL

**Đội đã trình bày:**

```
(Agent → STT → command) APP → service fw → vhal → CAN signal → CCU
```

**Mentor sửa:**

```
(Agent → STT → command) APP → service fw → PropertyID ← vhal → CAN signal → CCU
                                              ▲
                                    ranh giới thật nằm ở đây
```

Câu chốt: *"không có phần vhal nào nhận intent cả"*.

### Hiểu đúng chỗ này

VHAL **không biết** `hvac_set_temp` là gì. Nó chỉ biết `(propertyId, areaId, value)`. Nghĩa là giữa
`Intent` và VHAL **bắt buộc phải có một bảng dịch**, và bảng đó là tài sản của đội — không phải thứ
Android hay CarSky cho sẵn. Chiều mũi tên `PropertyID ← vhal` là chiều VHAL **trả giá trị ngược lên**
(get + callback real-time), tức ranh giới này **hai chiều và cả hai chiều đều nói bằng PropertyID**.

Hệ quả cụ thể — đây là thứ `03-contracts.md` §5 đang thiếu: `Skill.execute(intent)` hiện **nhảy thẳng**
từ intent sang hành động, không ghi ra nó dịch thành property nào.

### Luồng đầy đủ, viết lại

```
 ┌─ trong APK của đội ─────────────────────────────────┐
 │ Mic → VAD → ASR → IntentRouter → SafetyGuard        │   Long (·Tùng SafetyGuard)
 └───────────────────────┬─────────────────────────────┘
                         │  Intent(name, slots)          ← intent DỪNG LẠI Ở ĐÂY
                         ▼
 ┌─ VivaCarService (service fw của đội) ── M1 ─────────┐
 │  bảng intent → PropertyID + areaId + type ── M2     │   Tùng + Vĩ
 │  CarPropertyManager.setProperty / registerCallback  │
 └───────────────────────┬─────────────────────────────┘
                         │  (propertyId, areaId, value)  ← từ đây trở xuống KHÔNG còn intent
                         ▼
              Android Car Service  →  VHAL (pin `vhal` của Skycraft)
                         │
                         ▼
        IVI Gateway (Script Node)  ──kuksa──►  Central Broker (VSS)
                                                       ▲
                                                       │ kuksa
                                    PWT Gateway (Script Node)
                                                       │ can
                                                       ▼
                                        CAN Bus  →  CCU *(được phép mô phỏng — M5)*
```

Chiều ngược lại đi đúng đường đó ngược lên và **kết thúc ở callback của `VivaCarService`**, service
fan-out cho app HVAC / app DOOR / HMI — đó là chỗ M3 lấy dữ liệu để "app hiển thị lên".

### ⚠️ Một chỗ dễ khai sai: không phải lệnh nào cũng đi qua VHAL

| Nhóm intent | Đường đi thật | Có 6 chặng không? |
|---|---|---|
| `hvac_*`, `door_lock` | App → VivaCarService → PropertyID → VHAL → KUKSA → CAN → CCU | ✅ **có** |
| `volume_adjust` | App → `CarAudioManager` | ❌ không chạm VHAL |
| `media_*` | App → `MediaSession` / `MediaBrowserService` | ❌ không chạm VHAL |
| `delivery_*` | App → `DeliverySkill` (nội bộ) | ❌ không chạm VHAL |

→ Trong write-up L12 và Claim–Evidence Map N1, **claim "chạy full-stack tới CAN" chỉ được gắn cho nhóm
đầu tiên**. Khai gộp cả 10 intent là đúng loại sai mà ô *Minh bạch phạm vi demo* (2đ) và
*Ranh giới và tính tương xứng* (2đ) trừ điểm.

---

## PHẦN 3 — "SERVICE FW" LÀ GÌ VÀ ĐỘI TRIỂN KHAI KIỂU NÀO

Mentor hỏi ngược *"làm sao triển khai phần fw service"* và đưa link tài liệu Car Framework Core.
Đội phải trả lời được ở buổi office hours 04/08. Ba đường:

| | Cách | Chi phí | Rủi ro | Kết luận |
|---|---|---|---|---|
| **A** | Sửa AOSP thật: thêm service vào `packages/services/Car`, build lại image AAOS, flash lên Device | Nhiều ngày build | Chưa rõ CarSky có cho thay image guest không. Còn 10 ngày | ❌ **Loại** |
| **B** | **`VivaCarService` — APK riêng: `Service` + AIDL, giữ một kết nối `Car`/`CarPropertyManager` duy nhất, sở hữu bảng intent→PropertyID, gọi `SafetyGuard`, fan-out callback cho app HVAC / DOOR / HMI. Cài privileged nếu quyền đòi** | ~5h Tùng + 2h Vĩ | Quyền `android.car.permission.CONTROL_CAR_*` là **privileged** → có thể phải đẩy vào `/system/priv-app` + allowlist XML | ✅ **Khuyến nghị** |
| **C** | Giữ `VhalRepository` là library trong app như `06` T3 đang ghi | 0h | **Đúng chỗ mentor nói thiếu.** Và mất ô *Tách phần team-owned* 5đ vì không có thành phần `new` nào ở tầng framework | ❌ |

### Vì sao B chứ không phải C — không chỉ vì mentor nói

Ô *Tách phần team-owned* (5đ) chấm ranh giới `provided / configured / modified / new`, ô
*Mức quyết định* (6đ) chấm **ablation**: bỏ phần đội làm thì claim có sụp không.
Nếu `VhalRepository` chỉ là một class gọi `CarPropertyManager` trong app, ablation A3 trả lời được
*"bỏ đi thì HMI mất real-time"* — yếu. Nếu là một **service riêng ở tầng framework, các app khác bind
vào**, thì bỏ nó đi là **cả app HVAC lẫn app DOOR lẫn AI Agent cùng chết** — đó mới là L3.

**T3 không bị bỏ.** Code `VhalRepository` chuyển vào trong service; các app nhận một client AIDL mỏng.
Ước tính ~3h trong 5h của M1 là đóng gói lại T3, không phải viết mới.

### Việc phải kiểm chứng NGAY (M1a) — chưa ai biết câu trả lời

`06` PHẦN 9 giao báo cáo spike **S2 (VHAL)** từ 26/07 và **tới giờ chưa ai chốt**. Câu hỏi treo:
**APK thường có `setProperty` được HVAC không, hay bị từ chối quyền?**

Nếu bị từ chối thì đường cài là (bản `userdebug`, phải kiểm trên Device CarSky):

```bash
adb root && adb remount
adb push VivaCarService.apk /system/priv-app/VivaCarService/
adb push privapp-permissions-viva.xml /system/etc/permissions/
adb reboot
```

Manifest cần `<uses-library android:name="android.car"/>` và các quyền `CONTROL_CAR_CLIMATE`,
`CONTROL_CAR_DOORS`, `CAR_SPEED`.

> ⚠️ **Đây là rủi ro chặn cả T3, T4, T7 lẫn M1 — không riêng M1.** Nếu quyền không cấp được và không
> remount được, mọi lệnh HVAC/DOOR không đổi được property thật, và **cả xương sống đổ**. Phải biết câu
> trả lời **trong tối nay**, không phải 02/08.

### Vendor property — miếng "của riêng mình" thứ hai, gần như miễn phí

Đội **tự viết Script Node phía VHAL** nên đội tự định nghĩa được property trong dải **VENDOR**
(`VehiclePropertyGroup.VENDOR = 0x2000_0000`) và tự phục vụ nó — ví dụ một property mang trạng thái
chặng giao hàng. Đây là thứ starter pack **không có**, chứng minh được bằng một dòng trong Baseline
Manifest N3b với nhãn `new`.

*Cần kiểm chứng trên Device: dải vendor property thường đòi `android.car.permission.CAR_VENDOR_EXTENSION`.*
**Chỉ làm nếu M1 xanh trước 03/08** — đây là thứ đẹp, không phải thứ bắt buộc.

---

## PHẦN 4 — BLUEPRINT ĐÃ CÓ SẴN HAI SCRIPT NODE (đọc từ ảnh 30/07)

Ảnh canvas blueprint `Demo` (21/30 node, `Demo-deploy` **Running 21/21**) cho thấy:

| Node | Loại | Pin | Vai trò suy ra |
|---|---|---|---|
| **IVI Gateway** | Script Node · `Part Prefix: ivi-gateway` | `eth` (Client) · `kuksa` (Client) · **`vhal` (Server)** | **Skycraft (guest AAOS) ↔ KUKSA** — đúng chặng mentor bảo đi xem |
| **Central Broker (VSS)** | KUKSA Broker | `kuksa` | VSS ở giữa |
| **PWT Gateway** | Script Node | `can` · `kuksa` | **CAN ↔ KUKSA** |
| **IVI Switch** | Ethernet Bridge | `eth`, `vhal` ×n | nối các phần IVI |

### Hệ quả cho T2 — task rủi ro nhất của đội

`04-KE-HOACH-CAP-NHAT-28-07.md` PHẦN 2 và `06` T2 đang mô tả **một** Script Node "VHAL ↔ CAN hai chiều",
đội tự viết. Theo ảnh thì thực tế là **hai node, KUKSA đứng giữa, và cả hai đều đã tồn tại trong
blueprint mẫu**. Nghĩa là bảng ánh xạ của T1 phải có **ba** cột chứ không phải hai:

```
PropertyID  ↔  đường VSS (Vehicle.Cabin.HVAC…)  ↔  CAN signal (candb BTC)
     └── IVI Gateway ──┘                └── PWT Gateway ──┘
```

> ⚠️ **Đây là suy luận từ ảnh, chưa xác nhận.** Việc đầu tiên của Tùng hôm nay: bấm **Edit Script** trên
> IVI Gateway và PWT Gateway của blueprint đã clone, **đọc code có sẵn**, rồi mới quyết định sửa hay viết.
> Nếu đúng như đọc từ ảnh thì T2 giảm từ "viết mới 8h" xuống **"đọc + sửa mapping"**, và đường găng dài
> nhất của cả dự án ngắn lại đáng kể.
>
> Nếu sai thì mất 30 phút — vẫn rẻ hơn nhiều so với viết 8h Luau cho một topology tưởng tượng.

---

## PHẦN 5 — TÁM TASK MỚI (M1–M8) VÀ GIỜ LẤY TỪ ĐÂU

| # | Task | Xong khi | Ai | Giờ | Deadline |
|---|---|---|---|---|---|
| **M1a** | **Spike quyền VHAL** — APK thường `setProperty(HVAC_TEMPERATURE_SET)` được không? Không được thì cài privileged theo PHẦN 3 | Một dòng logcat cho thấy property đổi thật, kèm cách cài đã dùng | Tùng | 1.5h | 🔴 **31/07** |
| **M1** | **`VivaCarService`** — Service + AIDL, giữ kết nối `Car` duy nhất, sở hữu bảng M2, gọi `SafetyGuard`, fan-out callback | App HVAC và app DOOR **cùng bind một service**, cùng nhận callback | Tùng 5h · Vĩ 2h | 7h | 🔴 **02/08** |
| **M2** | **Bảng `intent → PropertyID + areaId + kiểu + value → đường VSS → CAN signal`** — mắt xích mentor nói đang thiếu. Mở rộng thẳng bảng T1 lên trên một tầng | Mỗi intent nhóm `hvac_*`/`door_lock` có đủ 4 cột, không dòng nào để trống | Long 1h · Tùng 1h | 2h | 🔴 **31/07** |
| **M3** | **App HVAC + app DOOR tách riêng**, mỗi cái một launcher entry; ra lệnh giọng → **app tương ứng bật lên màn hình** và phản chiếu giá trị mới | Nói "hạ điều hòa 22 độ" → app HVAC tự hiện, số đổi theo | Dương 4h · Tùng 1h | 5h | 🟡 **03/08** |
| **M4** | **Đọc Script Node có sẵn + dùng candb BTC** trước khi viết Luau (PHẦN 4) | Trả lời được: node nào giữ chặng nào, sửa hay viết mới | Tùng 1h · Vĩ 1h | 2h | 🔴 **31/07** |
| **M5** | **CCU mô phỏng** — Script Node echo `HvacCommand` → `HvacStatus` để đóng vòng phản hồi | Đổi property → Signal Watch đổi → giá trị quay ngược về HMI | Tùng | 1.5h | 🟡 **02/08** |
| **M6** | **Cài APK lên Device AAOS ngay hôm nay**, chạy thử phần AI trên máy thật dù còn thiếu tính năng | Ảnh app chạy trên Device + logcat sạch | Dương 1h · cả đội | 1h | 🔴 **31/07** |
| **M7** | **Bộ 5 tình huống phức tạp** (PHẦN 6) — chỗ mentor gọi là chìa khoá ăn điểm | 5/5 tình huống có hành vi mong đợi viết trước, chạy đúng, có log | Long | 3h | 🔴 **02/08** |
| **M8** | **AI ↔ Media**: "chuyển bài" / "phát bài …" phải nằm trong kịch bản demo | Có trong văn bản L8 | Long | **0h** *(intent đã có)* | 🔴 02/08 |

### Ngân sách — nói thẳng là đệm mỏng đi

| Người | Nhận thêm | Đệm trước | Đệm sau | Ghi chú |
|---|---|---|---|---|
| **Long** | M2 1h + M7 3h | 5.5h | **4h** | 4h thêm được bù 2.5h bằng cách cắt lại **L5b −1h · L9 −0.5h · L10 −1h** (`07` PHẦN 3), chỉ 1.5h lấy từ đệm. Cắt L5b vì 5 intent còn lại đều là biến thể — mentor nói rõ điểm nằm ở **chiều sâu tình huống** chứ không phải số lệnh |
| **Tùng** | M1a 1.5 + M1 5 + M2 1 + M3 1 + M4 1 + M5 1.5 = **11h** | 11h | **~0h** | Nhưng **M4 có thể trả lại 3–4h cho T2**. Nếu M4 không giúp được thì Tùng vỡ → xem PHẦN 6 |
| **Vĩ** | M1 2h + M4 1h | 6h | **3h** | |
| **Dương** | M3 4h + M6 1h | 12.5h | **7.5h** | Dương là người dư giờ nhất, nhận M3 là đúng chỗ |

> **Tùng là người phải theo dõi sát nhất từ giờ tới 03/08.** Anh ấy vừa giữ đường găng cũ (T2) vừa nhận
> phần lớn việc mới. Nếu M4 không rút ngắn được T2 thì **M5 (CCU mô phỏng) và M3 (phần Tùng) là hai
> thứ hoãn trước tiên** — không được hoãn M1a, M1, M2.

---

## PHẦN 6 — M7: "AI XỬ LÝ ĐỘ PHỨC TẠP" NGHĨA LÀ GÌ

Mentor nói hai lần, bằng hai cách: *"con AI sẽ xử lý độ phức tạp ntn"* và *"xử lý AI cho các tình huống"*.
Đây không phải yêu cầu thêm tính năng — là yêu cầu **chứng minh agent làm được việc mà một bảng
if/else không làm được**. Năm tình huống, mỗi cái phải viết **hành vi mong đợi trước khi code**:

| # | Tình huống | Câu ví dụ | Hành vi mong đợi |
|---|---|---|---|
| 1 | **Mơ hồ — suy ra ý định** | *"nóng quá"* | Không phải `unknown`. Suy ra `hvac_set_temp`, chọn giá trị theo trạng thái hiện tại, **nói ra mình vừa suy diễn gì** |
| 2 | **Ghép lệnh trong một câu** | *"khoá cửa rồi hạ điều hoà xuống 22"* | Tách thành 2 intent, thực thi **có thứ tự**, một câu TTS xác nhận cả hai |
| 3 | **Không an toàn** | *"mở cửa"* lúc `Speed=60` | `SafetyGuard` DENY **kèm lý do và lối thoát** — *"xe đang chạy, dừng hẳn rồi nói lại nhé"*. Đây cũng chính là ablation A1 |
| 4 | **Thiếu slot** | *"bật lên"* | Hỏi lại **đúng một câu**, không đoán bừa, không im lặng |
| 5 | **Ngoài phạm vi** | *"đặt bàn ăn tối"* | Từ chối lịch sự, khai rõ mình làm được gì. **Không rơi vào `unknown` im lặng** |

**Ăn vào đâu:** ô *Kịch bản đại diện và tình huống biên* **4đ** (L3 đòi *"phủ claim chính + edge case +
expected behavior"* — chính là cột phải của bảng trên) và ô *Xử lý lỗi và khả năng quan sát* **4đ**
mà `08` đang chấm 🟡 yếu. Cộng thêm: đây là nguyên liệu tốt nhất cho phần Q&A ở N7.

> Năm tình huống này **phải nằm trong kịch bản demo L8 (02/08)**, ít nhất 2 cái. Chạy được mà không quay
> vào video thì BGK không thấy.

---

## PHẦN 7 — ẢNH HƯỞNG TỚI MỐC CÂN 1 TỐI NAY (31/07 21:30)

`06` PHẦN 6 và `07` PHẦN 4 ghi mốc cân 1 có **hai** câu hỏi. **Giờ là bốn.**

| # | Câu hỏi | Nếu ❌ |
|---|---|---|
| ① | *"Hạ điều hoà xuống 22 độ" → 6 chặng đổi thật → HMI cập nhật → log đủ 6 mốc.* **ĐƯỢC hay CHƯA?* | Hoãn vô điều kiện D5–D9 (như cũ) |
| ② | *Lần chạy vừa rồi có phải **trên CarSky** không?* | Việc số 1 của cả đội (như cũ) |
| ③ | 🆕 *APK của đội đã **cài lên Device AAOS** và chạy được chưa?* (M6) | Cả đội dừng viết tính năng cho tới khi có một APK chạy trên Device. Code chưa từng biên dịch thì mọi ước lượng giờ còn lại đều là phỏng đoán |
| ④ | 🆕 *`setProperty` HVAC từ APK của đội có **bị từ chối quyền** không?* (M1a) | **Nặng nhất trong bốn.** Không set được property = không có 6 chặng = mất cả khối Demo lẫn khối Platform. Nếu chưa biết câu trả lời lúc 21:30 thì đây là việc duy nhất của Tùng đêm nay |

---

## PHẦN 8 — CÂU HỎI MỚI GỬI MENTOR

Ba câu cũ (demo live + Q&A · template Claim–Evidence Map · mức chấp nhận "chạy trên CarSky") **giữ nguyên**,
xem `07` PHẦN 5 ⑬. Thêm ba câu sinh ra từ buổi kick-off — hỏi ở office hours **04/08**, hoặc nhắn sớm hơn
nếu ④ ở PHẦN 7 đỏ:

| # | Câu | Vì sao gấp |
|---|---|---|
| d | **Device AAOS trên CarSky có phải bản `userdebug` và `adb root` / `adb remount` được không ạ?** Nếu app của đội cần quyền `CONTROL_CAR_CLIMATE` mà không cài privileged được thì đội phải đổi cách tiếp cận | Chặn M1a — và qua đó chặn cả xương sống |
| e | **Đội được sửa tới đâu trên guest AAOS?** Chỉ cài APK, hay được thay image / thêm service vào system partition? | Quyết định giữa đường B và đường A ở PHẦN 3 |
| f | **Script Node IVI Gateway / PWT Gateway trong blueprint mẫu — đội clone rồi sửa mapping có đúng cách anh gợi ý không ạ**, hay anh muốn đội tự viết node mới? | Ảnh hưởng trực tiếp cách khai `provided / configured / modified / new` trong Baseline Manifest N3 |

> Câu **f** còn một tác dụng phụ: nếu mentor xác nhận "clone rồi sửa" là cách đúng, đội có **câu trả lời
> sẵn cho Q&A** về ranh giới team-owned — thứ ô 5đ chấm.

---

## PHẦN 9 — GIỜ NÀY LÀM GÌ (31/07)

| Người | Việc | Hạn |
|---|---|---|
| **Tùng** | **M1a spike quyền** → **M4 đọc 2 Script Node có sẵn** → M2 phần property. *Ba việc này đứng trên mọi việc khác của anh ấy hôm nay* | trước 21:30 |
| **Long** | M2 phần intent · đăng file này vào nhóm · sửa 4 câu hỏi mốc cân 1 · chuẩn bị M7 cho 02/08 | trước 21:30 |
| **Dương** | **M6 — đưa được một APK lên Device**, dù còn trống. Nếu D1 chưa xong thì đây là D1 | 🔴 tối nay |
| **Vĩ** | M4 phần candb — xác nhận CAN DB của BTC dùng được, khỏi tự parse DBC | trước 21:30 |
| **Cả 4** | Standup 21:30 chạy **4 câu hỏi PHẦN 7**, không phải 2 | 21:30 |
