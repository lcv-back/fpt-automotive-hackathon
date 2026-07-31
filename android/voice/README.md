# `:voice-core` — voice pipeline module (Long)

Android **library** module. VAD · push-to-talk · ASR client · intent router · TTS ·
latency trace. Không phải app — app shell là D1 của Dương, module này cắm vào đó.

## Vì sao là library riêng, không viết thẳng vào app

`06-PHAN-CONG-4-NGUOI.md` PHẦN 5: *"App shell — nếu trễ, mỗi người chạy module bằng unit
test, ghép sau."* Tách library là cách biến câu đó thành sự thật thay vì lời hứa: toàn bộ
logic lõi ở đây **không import `android.*`**, nên unit test chạy trên JVM, không
cần Device, không cần emulator, không cần Robolectric.

Đúng **2 file** được phép chạm framework, và chúng chỉ làm mỗi việc dịch:

| File | Chạm gì |
|---|---|
| `trace/AndroidTrace.kt` | `SystemClock.elapsedRealtimeNanos`, `Log.i` |
| `audio/AndroidPcmSource.kt` | `AudioRecord` |

Thêm framework vào bất kỳ file nào khác là **làm hỏng tính chất này** — test sẽ cần emulator,
và test cần emulator là test không ai chạy.

## Ghép vào app shell (Dương)

Project thật ở `automotive/settings.gradle.kts` đã có:

```kotlin
include(":voice-core")
project(":voice-core").projectDir = file("../android/voice")
```

`automotive/feature/voice/build.gradle.kts` đã có:

```kotlin
dependencies { implementation(project(":voice-core")) }
```

Rồi:

```bash
cd automotive
./gradlew :voice-core:testDebugUnitTest :feature:voice:testDebugUnitTest
```

> Project Gradle và wrapper đã có sau khi merge app shell của Dương. Máy chạy lệnh vẫn cần JDK 21
> (`automotive/gradle/gradle-daemon-jvm.properties`) và Android SDK 37.

## Đang có gì

| Package | Task | Trạng thái |
|---|---|---|
| `trace/` | **L2** `LatencyTrace` + log format `VIVA_TRACE\|` | ✅ code + test + log mẫu |
| `audio/` | **L3a** push-to-talk `AudioRecord` + WAV | ✅ code + test |
| `audio/` | **L3b** Silero VAD ONNX | ⬜ 30/07 |
| `asr/` | **L4** `AsrClient` + `FakeAsrClient` | 🟡 contract + fake; endpoint thật chưa cắm |
| `intent/` | **L5a** grammar T0 — 5 lệnh xương sống | 🟡 code + test viết; chưa chạy vì thiếu Gradle root |
| `agent/` | Voice ↔ app/service boundary | 🟡 code + test viết; chờ Dương cắm adapter |
| `tts/` | **L6** `TtsSpeaker` | ⬜ 01/08 |

## `trace/` — dùng thế nào

```kotlin
// VAD (hoặc nút push-to-talk) mở lượt:
val trace = startVoiceTrace(nanos = utterance.startNanos)
trace.markAt(Stage.SPEECH_END, utterance.endNanos)

// Mỗi module đánh mốc của mình — chỉ 1 dòng:
trace.mark(Stage.GUARD_DONE)     // Tùng, trong SafetyGuard
trace.mark(Stage.EXEC_DONE)      // bất kỳ Skill nào

// Đóng lượt, đúng 1 lần:
trace.summary("hạ điều hòa xuống 22 độ", "hvac_set_temp", TraceVerdict.Allow)
trace.summary("mở cửa", "door_lock", TraceVerdict.Deny("G1_SPEED_LOCK"))
trace.summary("", "unknown", TraceVerdict.Error(Stage.ASR_DONE))   // lượt chết giữa chừng
```

Format dây và lý do từng luật: `vong2/03-contracts.md` §1. Log mẫu bàn giao cho harness:
[`fixtures/`](fixtures/README.md).

## `audio/` — dùng thế nào

```kotlin
val recorder = PushToTalkRecorder(AndroidPcmSource(), SystemNanoClock)
val utterance = recorder.record(isHeld = { talkButton.isPressed })   // chạy ở background thread
if (!utterance.isUsable) return                                      // chạm nhầm, đừng gọi ASR
val wav = WavWriter.toWav(utterance.pcm, utterance.sampleRate)       // để nghe lại khi debug
```

`record()` **block** — gọi từ main thread là ANR.

## Flow MVP đúng sau kick-off 30/07

```text
microphone front-end
  ├─ push-to-talk trigger
  └─ always-on wake-word detector ("Viva ơi" / "Vivi ơi")
  → command capture → VAD/endpointer → ASR (audio → text) → normalize
  → grammar T0 (LLM chỉ là fallback đề xuất intent)
  → CommandGateway
      ├─ media/volume → code của Dương
      └─ hvac/door   → VivaCarService → PropertyID → VHAL
  → Applied / Denied / ConfirmationRequired / Failed
  → HMI + TTS
```

Ba luật tích hợp:

1. ASR chính là STT; “lọc tiếng ồn” thuộc audio front-end/VAD, không phải một ASR thứ hai.
2. LLM không trả `passed/fail` và không được gọi thẳng Skill. Nó chỉ đề xuất `Intent` rồi vẫn đi qua
   safety/service gateway.
3. Chỉ `CommandResult.Applied` — tức tầng dưới đã xác minh trạng thái mới — mới được nói câu “Đã…”.
   Request vừa được nhận chưa phải là thành công.

`"lạnh quá"` không tự đổi nhiệt độ: câu này có nghĩa cần **ấm hơn**, nhưng thiếu mức đích. Router hỏi
`"Bạn muốn tăng nhiệt độ điều hòa lên bao nhiêu độ?"` để không làm ngược ý người dùng.

### Điểm cắm thật với app của Dương

Project `automotive/` đã include module này dưới tên `:voice-core`; `:feature:voice` phụ thuộc trực
tiếp vào nó. Dương không cần phụ thuộc vào microphone/ASR để bắt đầu, có thể cắm text giả trước:

```kotlin
val result = voiceAgent.handleText("Viva ơi, chuyển bài", trace)

when (result.status) {
    VoiceTurnStatus.APPLIED -> renderPatch(result.hmiPatch)
    else -> showVoiceMessage(result.spokenVi)
}
// Gọi sau frame đầu tiên thực sự phản ánh trạng thái mới:
trace.mark(Stage.RENDER_DONE)
```

Ranh giới kiểu dữ liệu đã có sẵn ở
`automotive/feature/voice/.../integration/CoreIntentMapper.kt`:

```kotlin
when (val action = CoreIntentMapper.map(coreIntent)) {
    is AutomotiveVoiceAction.VehicleControl -> executeVehicleControl(action.intent)
    is AutomotiveVoiceAction.VolumeAdjust -> adjustVolume(action.delta)
    AutomotiveVoiceAction.MediaNext -> skipToNext()
    null -> showVoiceMessage("Lệnh chưa có adapter hoặc thiếu slot")
}
```

Mapper này là chỗ duy nhất biết cả `com.viva.voice.intent.Intent` và `VehicleIntent` của app. Phần kế
tiếp chỉ cần nối ba nhánh action vào `ExecuteVehicleControlUseCase`, MediaSession và CarAudioManager;
`VoiceAgent` vẫn JVM thuần và không import Activity/ViewModel.

## Thư viện ngoài

Hiện **không có** dependency runtime nào ngoài AAOS SDK. Khi thêm ONNX Runtime cho L3b,
ghi nguồn vào đây và vào README gốc — checklist nộp bài 10/08 bắt buộc *"ghi rõ nguồn mọi
thư viện mã nguồn mở ngoài AAOS SDK"*.
