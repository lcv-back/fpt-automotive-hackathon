# `:voice` — voice pipeline module (Long)

Android **library** module. VAD · push-to-talk · ASR client · intent router · TTS ·
latency trace. Không phải app — app shell là D1 của Dương, module này cắm vào đó.

## Vì sao là library riêng, không viết thẳng vào app

`06-PHAN-CONG-4-NGUOI.md` PHẦN 5: *"App shell — nếu trễ, mỗi người chạy module bằng unit
test, ghép sau."* Tách library là cách biến câu đó thành sự thật thay vì lời hứa: toàn bộ
logic ở đây **không import `android.*`**, nên `./gradlew :voice:test` chạy trên JVM, không
cần Device, không cần emulator, không cần Robolectric.

Đúng **2 file** được phép chạm framework, và chúng chỉ làm mỗi việc dịch:

| File | Chạm gì |
|---|---|
| `trace/AndroidTrace.kt` | `SystemClock.elapsedRealtimeNanos`, `Log.i` |
| `audio/AndroidPcmSource.kt` | `AudioRecord` |

Thêm framework vào bất kỳ file nào khác là **làm hỏng tính chất này** — test sẽ cần emulator,
và test cần emulator là test không ai chạy.

## Ghép vào app shell (Dương)

`settings.gradle.kts`:

```kotlin
include(":voice")
project(":voice").projectDir = file("android/voice")   // chỉnh theo layout thật của repo
```

`app/build.gradle.kts`:

```kotlin
dependencies { implementation(project(":voice")) }
```

Rồi:

```bash
./gradlew :voice:test          # JVM, không cần Device
```

> ⚠️ **Chưa chạy được test cho tới khi có project Gradle.** Repo hiện chưa có
> `settings.gradle.kts` / gradle wrapper — chúng thuộc D1. Nếu D1 trượt qua 30/07, dựng
> tạm wrapper + `settings.gradle.kts` chỉ include `:voice` mất ~5 phút và đủ để chạy test.

## Đang có gì

| Package | Task | Trạng thái |
|---|---|---|
| `trace/` | **L2** `LatencyTrace` + log format `VIVA_TRACE\|` | ✅ code + test + log mẫu |
| `audio/` | **L3a** push-to-talk `AudioRecord` + WAV | ✅ code + test |
| `audio/` | **L3b** Silero VAD ONNX | ⬜ 30/07 |
| `asr/` | **L4** `AsrClient` + `FakeAsrClient` | ⬜ 30/07 |
| `intent/` | **L5** grammar T0 | ⬜ 31/07 (5 lệnh) → 01/08 (10 intent) |
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

## Thư viện ngoài

Hiện **không có** dependency runtime nào ngoài AAOS SDK. Khi thêm ONNX Runtime cho L3b,
ghi nguồn vào đây và vào README gốc — checklist nộp bài 10/08 bắt buộc *"ghi rõ nguồn mọi
thư viện mã nguồn mở ngoài AAOS SDK"*.
