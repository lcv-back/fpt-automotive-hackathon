# N4 — Ablation: bỏ phần đội làm thì claim nào sụp

> **Chủ sở hữu:** N4a (A2 + A3) — Vĩ · N4b (A1) — Tùng. Hạn 🟡 06/08.
> Bản này là **quy trình chạy + khung bảng kết quả**. Mọi ô số đang trống là
> **chưa đo**, không phải bằng 0 — không ai được điền ước lượng vào đó.
>
> Vì sao: ô *mức quyết định của phần team-owned* trong barem hỏi "bỏ phần các
> bạn làm thì sao". Câu trả lời có sức nặng nhất là hai lần chạy cùng một bộ
> câu, khác đúng một thành phần, và một bảng before/after.

## Công cụ — đã có, không phải viết thêm

```powershell
cd backend

# 1. Chạy baseline (hệ đầy đủ) và biến thể, mỗi lần một artifact set
.\scripts\run_benchmark.ps1 -Variant full     -Adb
.\scripts\run_benchmark.ps1 -Variant no-guard -Adb   # ⚠️ xem cảnh báo ngay dưới

# 2. Lập bảng before/after
go run ./cmd/viva-tools harness compare `
  --baseline runs\<stamp>-full\capture.log `
  --candidate runs\<stamp>-no-guard\capture.log `
  --baseline-label full --candidate-label no_guard `
  --out ablation_a1.csv --verdicts-out ablation_a1_verdicts.csv
```

`--verdicts-out` là cột sống của A1: nó đếm `Deny:G1_SPEED_LOCK` ở hai lần chạy.
Điều đó chỉ hoạt động vì `verdict` mang theo mã luật (`03-contracts.md` §1.2).

> 🔴 **`-Variant` mới chỉ là cái nhãn — nó KHÔNG tắt guard.** Kiểm 05/08:
> `run_benchmark.ps1` chỉ dùng `$Variant` để đặt tên thư mục artifact (dòng 56) và đóng
> dấu vào `run_manifest.txt` rồi truyền cho harness (dòng 73, 80). Nó không đổi APK đang
> chạy. Trong app cũng chưa có đường tắt nào: `app/src/{mock,real}/.../VehicleServiceModule.kt:30`
> đều bind thẳng `DefaultSafetyGuard`, không có build flag hay binding no-op.
>
> **Hệ quả:** chạy đúng hai dòng trên hôm nay sẽ ra **hai lần chạy giống hệt nhau** với hai
> nhãn khác nhau, và bảng before/after sẽ cho thấy guard "không thay đổi gì" — đó là số
> sai đội nhà tự tạo ra, tệ hơn hẳn việc để trống *chưa đo*. **Đừng chạy cột `no_guard`
> cho tới khi có cơ chế tắt thật**, và nếu không kịp thì cột đó giữ *chưa đo* — cột `full`
> vẫn chạy và vẫn có giá trị riêng.

---

## A1 — Tắt `SafetyGuard` (N4b, Tùng)

> ✅ **Tiền đề đã thoả một nửa (cập nhật 05/08).** `SafetyGuard` **đã có trong mã sản
> phẩm** từ PR #20: `vehicle-service/api/SafetyGuard.kt`, `impl/DefaultSafetyGuard.kt`,
> `impl/GuardedVehicleRepository.kt`, cắm vào cả hai biến thể app. PR #23 nối tiếp phán
> quyết ra dòng trace, nên B09 sinh được `Confirm:G2_CONFIRM_DOOR` và B10 sinh được
> `Deny:G1_SPEED_LOCK`.
>
> 🔴 **Thứ còn thiếu là cơ chế tắt** cho cột `no_guard` — xem cảnh báo ở mục *Công cụ*.
> Không có nó thì A1 chỉ đo được cột `full`.
>
> ⚠️ B20 (`Deny:G3_UNSUPPORTED`) vẫn chưa sinh được, vì hai lý do độc lập: câu ngoài phạm
> vi rơi vào `VehicleIntent.Unknown` nên **không bao giờ chạm tới guard** (không có
> `setProperty` nào được gọi), và `SafetyRules` cũng không có mã `G3_UNSUPPORTED`. Đúng
> như ghi chú sẵn trong `benchmark_v1.csv`: hôm nay B20 ra `Error:nlu_done`.

**Giả thuyết:** bỏ tầng an toàn của đội thì *"mở cửa"* lúc `Speed=60` vẫn thực thi.

| Cách tắt | Kết quả mong đợi | Câu thử |
|---|---|---|
| Bypass `SafetyGuard.evaluate` (build flag / DI thay bằng no-op) | `Deny:G1_SPEED_LOCK` biến mất, property `DOOR_LOCK` bị ghi thật | B09, B10 trong `suites/benchmark_v1.csv` |

| Chỉ số | full | no_guard | Ghi chú |
|---|---|---|---|
| `Deny:G1_SPEED_LOCK` (số lượt) | *chưa đo* | *chưa đo* | Kỳ vọng >0 → 0 |
| `Allow` trên `door_lock` khi đang chạy | *chưa đo* | *chưa đo* | Kỳ vọng 0 → >0 |
| p95 `e2e_computed` | *chưa đo* | *chưa đo* | Kỳ vọng gần như không đổi — an toàn **không** phải thứ làm chậm |
| `safety_guard` (chặng) | *chưa đo* | *chưa đo* | Kỳ vọng biến mất ở no_guard |

> ⚠️ Chạy A1 **trên Road Simulator, xe mô phỏng đang chạy**, không phải xe đứng yên —
> nếu tốc độ bằng 0 thì luật không kích hoạt và bảng này vô nghĩa ở cả hai cột.

---

## A2 — Thay `viva-asr` container bằng đường cloud (N4a, Vĩ)

> ⚠️ **Tiền đề chưa thoả tính đến snapshot 04/08:** `AsrClient` chưa được cắm vào app và
> hai engine chưa nhận cùng một PCM. Xem mục *Tiền đề hợp lệ* ở
> `15-QUYET-DINH-BENCHMARK-ASR.md`.

**Giả thuyết:** bỏ ASR chạy trong Room, đi qua mạng ngoài, thì p95 vượt ngân sách 1500ms.

| Cách đổi | Ghi chú |
|---|---|
| Trỏ `BuildConfig.ASR_BASE_URL` sang endpoint cloud thay vì Container Node | Không sửa code app — contract §2 đã bắt buộc đọc URL từ `BuildConfig` |

**⚙️ 04/08 — đã có nửa đầu của bảng.** Image `viva-asr` build và chạy được lần đầu;
36 clip tiếng Việt cho `server_ms` **p50=439 · p95=667**, RTF median **0.167**
(`evidence/asr/`). Đây là chặng ASR **đo trên CPU máy dev**, chưa phải node CarSky
và chưa phải giọng người thật — nhưng đủ để nói một điều cho L9: nếu chặng ASR một
mình đã chiếm ~670ms ở p95 thì ngân sách 1500ms cho cả đường còn lại **rất chặt**.

| Chỉ số | asr_container | asr_cloud | Ghi chú |
|---|---|---|---|
| p50 / p95 `asr_processing` | **439 / 667 ms** *(local, không phải CarSky)* | *chưa đo* | |
| p50 / p95 `e2e_computed` | *chưa đo* | *chưa đo* | Ngưỡng cam kết: p95 < 1500ms |
| Số lượt `Error:asr_done` | *chưa đo* | *chưa đo* | Timeout phải nằm trong mẫu, không được lọc ra |
| WER / intent accuracy | *chưa đo* | *chưa đo* | Từ `results.csv` của `harness verify` |

> Đây cũng chính là trục so sánh đã chốt ở `15-QUYET-DINH-BENCHMARK-ASR.md`
> (*ASR on-device Vosk* vs *`viva-asr` container*). Nếu chạy được cả ba đường
> (Vosk / container / cloud) thì bảng có ba cột; nếu không, khai đúng số cột đã đo.

---

## A3 — Bỏ callback của `VhalRepository` (N4a, Vĩ)

**Giả thuyết:** bỏ đường callback real-time thì HMI không còn phản chiếu trạng thái xe.

| Cách tắt | Kết quả mong đợi |
|---|---|
| Không đăng ký callback, chỉ đọc property theo yêu cầu | Chặng `hmi_render` **không còn mốc nào** |

| Chỉ số | full | no_callback | Ghi chú |
|---|---|---|---|
| `hmi_render` (n mẫu) | *chưa đo* | *chưa đo* | Kỳ vọng: n > 0 → **n = 0**, harness in `not comparable` |
| `screen_latency` p95 | *chưa đo* | *chưa đo* | |
| Đổi giá trị ở GPIO Panel → HMI tự đổi | *chưa đo* | *chưa đo* | Bằng chứng bằng ảnh, không bằng lời |

> `harness compare` in dòng `not comparable` khi một chỉ số biến mất hẳn ở một
> phía. Ở ablation, **chính sự biến mất đó là kết quả**, không phải lỗ hổng dữ liệu.

---

## A4 — Tắt tầng grammar T0 ✅ **ĐÃ ĐO 04/08**

**Giả thuyết** (`16-QUYET-DINH-DUONG-NLU.md`): bỏ grammar thì mọi lệnh phụ thuộc
ngưỡng cosine, và các câu bị từ chối ở bước 4 mất chốt chặn.

**Cách tắt:** `ProcessVoiceCommandUseCase` nhận `IntentRouter` qua constructor
(production wiring bind `GrammarIntentRouter` trong `VoiceModule`). Nhánh ablation
thay bằng router trả `Unsupported(canFallback = true)` cho mọi câu → tất cả rơi
xuống keyword + embedding. **Không cần Device.**

```
cd automotive
.\gradlew :feature:voice:testDebugUnitTest --tests "*GrammarAblationTest*"
```

Đầu vào là **chính** `backend/suites/benchmark_v1.csv` — test đọc thẳng file đó,
không chép lại danh sách, nên bảng ablation và bảng benchmark không thể nói về hai
tập câu khác nhau.

| Kết quả trên 22 câu | Số câu |
|---|---|
| `COMMAND_LOST` — lệnh lõi ngừng hoạt động | **12** |
| `REFUSAL_LOST` — câu đáng lẽ bị từ chối lại thành lệnh xe thật | **2** |
| `changed` — mất câu hỏi lại / mất lý do từ chối | 2 |
| `same` | 6 |

**Hai dòng đáng trích nhất:**

| Câu | Có grammar | Bỏ grammar |
|---|---|---|
| *"đặt nhiệt độ 40 độ"* | `Clarification` — ngoài dải 16–32°C, hỏi lại | **`SetTemperature(40.0)`** — giá trị ngoài dải đi thẳng xuống lệnh xe |
| *"bật điều hòa"* (1 trong 5 lệnh đã cắt 29/07) | `Clarification` — từ chối lịch sự, nói rõ phạm vi | **`SetAc(true)`** — thực thi |

Tức là grammar không chỉ là "bộ nhận câu": nó đang giữ **miền giá trị hợp lệ** và
**ranh giới phạm vi**. Bỏ nó thì cả hai biến mất cùng lúc.

Mất luôn cả phần hỏi lại: *"quạt mạnh lên"* từ câu hỏi *"mức mấy, từ 0 đến 5?"*
thành `Unknown`; *"Siri ơi hạ điều hòa xuống 24 độ"* từ lời giải thích wake phrase
thành `Unknown` chung chung.

**Artifact:** `evidence/ablation/a4-grammar-ablation.csv` + `a4-run-manifest.txt`
(có commit, JDK, lệnh chạy).

> ⚠️ **Giới hạn phải khai khi trích.** Embedding thật (MiniLM ONNX) không nạp được
> trong unit test JVM, nên nhánh no-grammar ở đây chỉ còn keyword mapping. Bảng này
> **đánh giá thấp** mức hư hại: trên máy thật, embedding còn có thể suy ra một lệnh
> xe cho những câu ở đây rơi vào `Unknown`. Đây là ablation tầng JVM, **không phải**
> bằng chứng chạy trên Device.

## Ba luật khi viết kết quả vào write-up

1. **Không ngoại suy.** Ô *chưa đo* để nguyên chữ "chưa đo" trong bản nộp nếu đến 06/08 vẫn chưa chạy được.
2. **Nói rõ cách tắt.** "Tắt SafetyGuard" phải kèm *tắt bằng cách nào* — bypass DI, build flag, hay sửa luật — nếu không thì không ai lặp lại được.
3. **Cùng bộ câu, cùng mức nhiễu, cùng commit.** `run_manifest.txt` của mỗi lần chạy ghi sẵn commit và hash của suite; nếu hai lần chạy khác commit thì bảng so sánh đó không dùng được.
