# HỢP ĐỒNG INTERFACE — VIVA

> **Chủ sở hữu: Long.** Chốt bản v1 trước 28/07. Sau khi chốt, **không ai sửa file này một mình** — muốn đổi phải báo nhóm.
> Mục đích: 4 người làm song song mà không giẫm chân nhau. Ai cũng code theo interface này, kể cả khi module bên kia chưa xong (dùng stub).

---

## 0. Sơ đồ luồng dữ liệu

```
Audio ──▶ VadSegmenter ──▶ AsrClient ──▶ IntentRouter ──▶ SafetyGuard ──▶ SkillDispatcher ──▶ Skill
                                                                │                                │
                                                                └── DENY/CONFIRM ────────────────┤
                                                                                                 ▼
                                                                                     TtsSpeaker + HmiState
```

Mỗi mũi tên là một interface dưới đây. **Mọi bước đều ghi mốc thời gian vào `LatencyTrace`.**

---

## 1. `LatencyTrace` — đo latency (Long sở hữu)

Bắt buộc mọi module phải gọi. Đây là nguồn dữ liệu duy nhất cho benchmark report của đề #3.

```kotlin
data class LatencyTrace(
    val traceId: String,                        // UUID, sinh khi VAD phát hiện bắt đầu nói
    val marks: MutableMap<String, Long> = mutableMapOf()   // tên chặng -> elapsedRealtimeNanos
) {
    fun mark(stage: String) { marks[stage] = android.os.SystemClock.elapsedRealtimeNanos() }
    fun ms(from: String, to: String): Double =
        (marks[to]!! - marks[from]!!) / 1_000_000.0
}
```

**Tên chặng chuẩn — không đặt tên khác:**

| Mốc | Ai gọi | Nghĩa |
|---|---|---|
| `speech_start` | VadSegmenter | VAD phát hiện bắt đầu có tiếng nói |
| `speech_end` | VadSegmenter | VAD xác định đã nói xong (endpoint) |
| `asr_sent` | AsrClient | Đã gửi audio đi |
| `asr_done` | AsrClient | Đã nhận text về |
| `nlu_done` | IntentRouter | Đã ra intent |
| `guard_done` | SafetyGuard | Đã có phán quyết |
| `exec_done` | Skill | Hành động đã thực thi xong (VHAL/Media trả về) |
| `render_done` | HMI | Frame đầu tiên phản ánh trạng thái mới |
| `tts_start` | TtsSpeaker | Bắt đầu phát tiếng |

**Log format bắt buộc** (harness của Vĩ parse dòng này):
```
adb logcat tag = VIVA_TRACE
VIVA_TRACE|<traceId>|<stage>|<elapsedRealtimeNanos>
```
Kết thúc mỗi lượt in thêm 1 dòng tổng kết:
```
VIVA_TRACE_SUMMARY|<traceId>|<utterance>|<intent>|<verdict>|e2e_ms=<số>
```

---

## 2. `AsrClient` — Long gọi, Vĩ hiện thực service

**Interface phía app:**
```kotlin
interface AsrClient {
    suspend fun transcribe(pcm16: ShortArray, sampleRate: Int, trace: LatencyTrace): AsrResult
}

data class AsrResult(
    val text: String,
    val confidence: Float,      // 0.0 .. 1.0
    val serverMs: Int,          // thời gian xử lý phía server, để tách khỏi latency mạng
    val isPartial: Boolean = false
)
```

**API phía container `viva-asr`:**
```
POST /asr
Content-Type: application/octet-stream
Header: X-Sample-Rate: 16000
Header: X-Trace-Id: <traceId>
Body: raw PCM 16-bit LE mono

200 OK
{ "text": "hạ điều hòa xuống 22 độ", "confidence": 0.94, "server_ms": 210 }

# Health check bắt buộc có:
GET /health -> {"status":"ok","model":"phowhisper-small-int8"}
```

**Địa chỉ service** — chốt sau spike S1/S4:
- PA-1: `http://10.99.0.2:8080` (Container Node qua Ethernet Bridge)
- PA-2: `http://127.0.0.1:8080` (laptop qua `adb reverse tcp:8080 tcp:8080`)

Config trong app đọc từ `BuildConfig.ASR_BASE_URL` — **không hard-code**.

---

## 3. `Intent` — Long sở hữu

```kotlin
data class Intent(
    val name: String,           // xem bảng dưới, dạng snake_case
    val slots: Map<String, Any>,
    val confidence: Float,
    val tier: Tier              // T0 = grammar, T1 = classifier, T2 = cloud LLM
) {
    enum class Tier { T0, T1, T2 }
}
```

**Danh mục intent v2 — 🆕 sửa 29/07: rút xuống 10 intent lõi + `unknown`.**

Barem Vòng 2 mới ghi thẳng *"không cộng điểm theo số lượng chức năng"* → 15 intent không hơn 10 intent.
Bộ 10 dưới đây được chọn để **giữ nguyên mọi cam kết đã nộp**: slide 11 hứa *"≥5 lệnh car control (cửa,
âm lượng, media, điều hòa)"* ✔ và proposal slide 3 giao Vĩ *"delivery simulator"* ✔ (giữ đủ 3 intent).

| `name` | Slots | Ví dụ câu | Skill xử lý |
|---|---|---|---|
| `hvac_set_temp` | `value: Float`, `zone: String?` | "hạ điều hòa xuống 22 độ" | Climate ⭐ lệnh xương sống |
| `hvac_set_fan` | `level: Int` (0–4) | "quạt mức 2" | Climate — chứng minh `areaId` thứ 2 |
| `door_lock` | `lock: Boolean` | "khóa cửa", "mở cửa" | Body ⚠️ nhạy cảm — **cần cho ablation A1** |
| `volume_adjust` | `delta: Int` | "tăng âm lượng" | System — kéo theo audio focus |
| `media_play` | `query: String?` | "phát nhạc", "phát playlist đi làm" | Media |
| `media_pause` | — | "dừng nhạc" | Media |
| `media_next` | — | "chuyển bài" | Media |
| `delivery_next_stop` | — | "chặng tiếp theo là gì" | Delivery |
| `delivery_order_status` | `orderId: String?` | "đơn A12 thế nào" | Delivery |
| `delivery_confirm` | `orderId: String?` | "xác nhận giao thành công" | Delivery ⚠️ nhạy cảm |
| `unknown` | `rawText: String` | — | fallback: hỏi lại |

**5 intent đã cắt 29/07** — ~~`hvac_power`~~ · ~~`hvac_ac`~~ · ~~`volume_set`~~ · ~~`media_prev`~~ ·
~~`dtc_query`~~ *(theo T10 đã bỏ)*. Cả 5 đều là biến thể của intent còn lại, không mở thêm năng lực nào mới.

> ⚠️ **Grammar vẫn phải nhận diện và từ chối lịch sự** 5 câu đã cắt, đừng để rơi vào `unknown` im lặng —
> ô *"Xử lý lỗi và khả năng quan sát"* (4đ) chấm việc này.

---

## 4. `SafetyGuard` — Tùng sở hữu ⭐ điểm khác biệt của đội

```kotlin
interface SafetyGuard {
    fun evaluate(intent: Intent, state: VehicleState, trace: LatencyTrace): Verdict
}

sealed class Verdict {
    object Allow : Verdict()
    data class Deny(
        val rule: String,          // "G1_SPEED_LOCK"
        val reasonVi: String,      // câu TTS đọc cho tài xế
        val suggestion: String?    // "sẽ thực hiện khi xe dừng hẳn"
    ) : Verdict()
    data class Confirm(
        val rule: String,
        val questionVi: String     // "Bạn có chắc muốn mở cửa không?"
    ) : Verdict()
}

data class VehicleState(
    val speedKph: Float,
    val gear: String,              // "P" | "R" | "N" | "D"
    val parkingBrake: Boolean,
    val ignition: String,
    val doorsLocked: Boolean,
    val timestampNanos: Long       // tuổi của snapshot — quá 500ms thì coi là stale
)
```

**Bộ luật v1 (chốt cứng, mỗi luật phải có test case):**

| ID | Điều kiện | Áp dụng cho | Kết quả |
|---|---|---|---|
| `G1_SPEED_LOCK` | `speedKph > 5` | `door_lock(lock=false)` | Deny — "Xe đang chạy, mình chưa mở cửa được. Bạn dừng hẳn rồi nói lại nhé." |
| `G1_GEAR_LOCK` | `gear != "P"` | `door_lock(lock=false)` | Deny |
| `G2_CONFIRM_DOOR` | `speedKph == 0 && gear == "P"` | `door_lock(lock=false)` | Confirm — "Bạn có chắc muốn mở khóa cửa không?" |
| `G2_CONFIRM_DELIVERY` | luôn | `delivery_confirm` | Confirm — "Xác nhận đã giao đơn ___ phải không?" |
| `G1_STALE_STATE` | `now - timestampNanos > 500ms` | mọi intent nhạy cảm | Deny — "Mình chưa đọc được trạng thái xe, thử lại giúp mình." |
| `G3_LOW_CONFIDENCE` | `intent.confidence < 0.6` | mọi intent nhạy cảm | Confirm — hỏi lại nguyên câu |
| `G3_LLM_WHITELIST` | `tier == T2 && name !in whitelist` | mọi intent từ cloud | Deny — không thực thi |

**Nguyên tắc bất di bất dịch:** LLM chỉ được **đề xuất** intent. Chỉ `SafetyGuard` mới quyết định thực thi. Không có đường tắt từ tier T2 xuống Skill.

---

## 5. `Skill` — mỗi người sở hữu skill của mình

```kotlin
interface Skill {
    val handles: Set<String>                    // tên intent skill này xử lý
    suspend fun execute(intent: Intent, trace: LatencyTrace): SkillResult
}

data class SkillResult(
    val ok: Boolean,
    val spokenVi: String,          // câu TTS đọc lại cho tài xế
    val hmiPatch: Map<String, Any>, // phần trạng thái HMI cần cập nhật
    val error: String? = null
)
```

| Skill | Owner | Intent xử lý |
|---|---|---|
| `ClimateSkill` | Tùng | `hvac_*` |
| `MediaSkill` | Dương | `media_*`, `volume_*` |
| `DtcSkill` | Tùng | `dtc_query` |
| `DeliverySkill` | Vĩ | `delivery_*` |
| `BodySkill` | Tùng | `door_lock` |

---

## 6. `MediaSourceProvider` — Dương sở hữu ⭐ tính năng chữ ký đề #1

Interface plugin này chính là thứ ăn điểm "Extensibility" của đề Media.

```kotlin
interface MediaSourceProvider {
    val id: String                              // "usb", "local", "stream"
    val displayName: String
    suspend fun isAvailable(): Boolean
    suspend fun browse(parentId: String?): List<MediaItem>
    suspend fun search(query: String): List<MediaItem>
    fun uriFor(itemId: String): android.net.Uri
}

data class MediaItem(
    val id: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val albumArtUri: android.net.Uri?,
    val sourceId: String
)
```

Hiện thực bắt buộc: `UsbMediaProvider` (đọc từ mount point của USB image) và `LocalMediaProvider`.
Đăng ký qua `MediaSourceRegistry.register(provider)` — thêm nguồn mới **không đụng vào core**. Ghi rõ điều này trong README.

Album art: `LruCache` in-memory (32MB) + disk cache. Load **ngoài main thread**, có placeholder — chấm tiêu chí "không ANR khi tải cao".

---

## 7. ~~`DtcClient`~~ — 🚫 **KHÔNG TRIỂN KHAI Ở VÒNG 2** (quyết định 29/07)

> **Bỏ 29/07:** barem Vòng 2 mới **không chấm theo tiêu chí từng đề** — chỉ có một bảng 100đ cho cả sản
> phẩm, và dòng cộng điểm cross-vertical *"+05 kết hợp ≥2 domain"* thuộc **Vòng 3**, không phải Vòng 2.
> Task **T10 đã bị bỏ**, 9h chuyển sang N3b + N4b. Xem `08-BAREM-VONG-2-CHINH-THUC.md`.
>
> **Contract dưới đây giữ lại nguyên vẹn làm tài sản cho Vòng 3** — nếu đội vào chung kết, đây là chỗ
> cross-vertical thật sự có 5đ và `DtcClient` đáng làm. Đừng xoá, cũng đừng hiện thực ở Vòng 2.
>
> *(Lịch sử: bản 28/07 chuyển sở hữu từ "Vĩ hiện thực, Tùng dùng" sang Tùng trọn gói, theo proposal slide 3.)*

**API phía container `viva-svc`:**
```
GET /dtc
200 OK
{
  "codes": [
    { "code":"P0301", "raw":"0x010420", "status":"pending",
      "group":"P", "system":"engine", "severity":"medium",
      "description":"Misfire xy-lanh 1",
      "firstSeen":"2026-08-01T10:00:00Z", "count": 3 }
  ],
  "source": "uds_isotp"     // hoặc "simulated" — phải trung thực
}

GET /dtc/analysis
200 OK
{
  "frequency":   [ {"code":"P0301","count":12} ],
  "trend":       [ {"date":"2026-08-01","total":3} ],
  "correlation": [ {"pair":["P0301","P0171"],"cooccurrence":0.82} ],
  "bySystem":    {"engine":5,"transmission":1,"body":0,"chassis":0,"network":2}
}

GET /dtc/export?format=json    # tải file, phục vụ Output Request của đề
```

**Cách container lấy DTC thật (KHÔNG mock):**
```python
# Socket ISO-TP runtime mở sẵn cho mỗi CAN Bus pin
SOCK = "/run/nydus/uds-<tên_pin_can>.sock"
# 1. Config: [0x01][len LE][addr_mode=0][tx=0x7E0][rx=0x7E8][can_fd=0][p2=1000]
# 2. Request ReadDTCInformation by status mask: [0x10][len][0x19 0x02 0xFF]
# 3. Response tag 0x20, payload bắt đầu 0x59 0x02 ...
```

Phân loại nhóm theo ký tự đầu: `P` powertrain · `C` chassis · `B` body · `U` network.

---

## 8. `TtsSpeaker` — Long sở hữu

```kotlin
interface TtsSpeaker {
    suspend fun speak(textVi: String, trace: LatencyTrace)
    fun stop()
}
```

Thứ tự fallback:
1. Android `TextToSpeech` với `Locale("vi","VN")` — kiểm tra bằng `isLanguageAvailable()`
2. Nếu không có: phát file audio pre-render (~30 câu cố định) đóng gói trong `res/raw/`
3. Nếu vẫn không được: hiển thị text trên HMI + ping âm báo

⚠️ Widget Text-to-Speech của CarSky **chỉ có zh-TW và en-US** — không dùng được cho tiếng Việt.

---

## 9. Quy ước chung

- **Ngôn ngữ log:** không dấu tiếng Việt trong `Log.i` (tránh vỡ encoding trên logcat Windows). Text đọc cho tài xế thì có dấu bình thường.
- **Tag logcat:** `VIVA_<MODULE>` — `VIVA_ASR`, `VIVA_GUARD`, `VIVA_MEDIA`, `VIVA_DTC`, `VIVA_TRACE`
- **Không hard-code URL/key.** Tất cả qua `BuildConfig` hoặc `.env`.
- **Mỗi module có 1 stub** để người khác chạy được khi module thật chưa xong: `FakeAsrClient` (trả text cố định), `FakeDtcClient`, `FakeVehicleState`.
- **Branch:** `main` luôn build được. Mỗi người làm trên `feat/<tên>-<module>`, merge khi xanh.

---

## 10. Bảng "ai chờ ai" — dùng để gỡ tắc

| Module | Chặn bởi | Có stub để chạy song song? |
|---|---|---|
| `AsrClient` | Spike S1 (mic) | ✅ `FakeAsrClient` trả text cố định |
| `ClimateSkill` | Spike S2 (property) | ✅ log ra thay vì gọi VHAL |
| `MediaSkill` | Spike S3 (usb.img) | ✅ đọc mp3 trong `res/raw` |
| `DtcSkill` | Mentor trả lời câu #6 | ✅ `FakeDtcClient` trả 3 mã mẫu |
| `SafetyGuard` | **Không chặn bởi ai** | — làm được ngay hôm nay |
| `DeliverySkill` | **Không chặn bởi ai** | — làm được ngay hôm nay |
| Benchmark harness | `LatencyTrace` log format | ✅ tự sinh log giả để test parser |

> **Kết luận: `SafetyGuard`, `DeliverySkill` và benchmark harness không bị chặn bởi bất kỳ ẩn số nào.** Ai xong spike sớm thì bắt tay vào 3 thứ này ngay, đừng ngồi chờ mentor trả lời.
