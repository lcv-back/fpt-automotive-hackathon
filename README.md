# VIVA Digital Cockpit

VIVA là prototype buồng lái số cho Android Automotive OS (AAOS), tập trung vào trợ lý giọng nói offline, điều khiển HVAC/cửa, media và quan sát độ trễ đầu-cuối. Mục tiêu của Vòng 2 là chứng minh luồng do đội sở hữu có ranh giới rõ ràng, đo được và không bỏ qua tầng an toàn/service khi thực thi lệnh xe.

## Trạng thái hiện tại

| Hạng mục | Trạng thái | Bằng chứng / giới hạn |
|---|---|---|
| App AAOS, UI HVAC/vehicle status, mock repository | **Đã tích hợp** | Hai flavor `mock` và `real` build được; mock dùng simulator trong bộ nhớ |
| Voice core: trace, thu âm, VAD, grammar 10 intent, TTS | **Đã tích hợp ở mức code/build** | Unit test và APK build xanh; nghe/thu trong cabin vẫn là Device Integration Gate |
| Audio focus cho TTS | **Đã tích hợp ở mức code/build** | Xin transient focus trước khi nói, trả focus sau success/failure; kiểm chứng ducking với media thật trên Device còn chờ |
| Vosk/MiniLM assets và pipeline hiện hữu của app | **Đã tích hợp** | Chạy offline; đường nối cuối từ pipeline service tới grammar core đang được hoàn thiện theo từng adapter |
| `FakeAsrClient`, dữ liệu TTS/noise synthetic, mock vehicle | **Mô phỏng** | Dùng cho test tái lập, không được xem là bằng chứng cabin/xe thật |
| `VivaCarService` → VHAL → gateway → CAN/CCU | **Kế hoạch / tích hợp đội** | Contract đã chốt; quyền privileged và luồng Device phải được chứng minh riêng |
| ASR container `viva-asr` | **Kế hoạch / phụ thuộc đội** | Trục benchmark đã chốt, còn chờ số đo từ harness |

Không claim toàn bộ 10 intent đi tới CAN. Chỉ `hvac_*` và `door_lock` thuộc đường Vehicle Property; media, volume và delivery đi qua adapter riêng. Xem [contract tích hợp](vong2/03-contracts.md).

## Kiến trúc

```text
Microphone / push-to-talk
  → VAD / endpoint
  → ASR (audio → text)
  → normalize + grammar router
  → CommandGateway + SafetyGuard
      ├─ HVAC / door → VivaCarService → PropertyID → VHAL → gateway → CAN/CCU
      ├─ media       → MediaSession
      ├─ volume      → Android car audio adapter
      └─ delivery    → in-app skill
  → Applied / Denied / ConfirmationRequired / Failed
  → HMI + TTS (audio focus)
```

`Intent` dừng ở biên app/service; VHAL chỉ nhận `(propertyId, areaId, value)`. TTS chỉ được nói câu xác nhận dạng “Đã…” sau khi tầng thực thi trả `Applied`.

Các module chính:

```text
automotive/                 app AAOS, feature modules, vehicle-service API/impl
android/voice/              voice-core JVM + Android adapters
backend/                    Go benchmark harness và CarSky devops helper
vong2/03-contracts.md       interface và mapping intent → PropertyID → VSS → CAN
vong2/13-M7A-*.md           tình huống phức tạp và hành vi mong đợi
vong2/14-KICH-BAN-*.md      kịch bản demo 3 phút và đường thoát lỗi
vong2/15-QUYET-DINH-*.md    quyết định trục benchmark ASR
```

## Build và kiểm thử

Yêu cầu: Temurin JDK 21, Android SDK 37 và Android build-tools 37.0.0. Không cần secret để build/test local.

```powershell
cd automotive
./gradlew :voice-core:testDebugUnitTest `
  :feature:voice:testDebugUnitTest `
  :vehicle-service:api:testDebugUnitTest `
  :vehicle-service:impl:testDebugUnitTest `
  :core:common:testDebugUnitTest

./gradlew :app:assembleMockDebug :app:assembleRealDebug
```

APK debug được sinh dưới `automotive/app/build/outputs/apk/<flavor>/debug/`. Flavor `real` cần app privileged/platform-signed và permission allowlist của OEM để ghi các Vehicle Property được bảo vệ; chi tiết cài đặt nằm trong [README Android](automotive/README.md).

Backend harness không có dependency ngoài Go standard library:

```powershell
cd backend
go test ./...
go run ./cmd/viva-tools harness report --input testdata/sample_trace.log --out report.csv
```

Nếu dùng CarSky helper, sao chép `backend/.env.example` thành `backend/.env` và tự điền credential. `.env`, token, API key, keystore và APK không được commit.

## Thêm intent mà không sửa grammar core

`GrammarIntentRouter` nhận các `GrammarRule` bổ sung ở composition root. Rule chỉ phân tích câu đã lowercase, chuẩn hóa dấu câu và bỏ wake phrase; nó đề xuất `RouteResult`, không được tự thực thi lệnh.

```kotlin
val trunkRule = GrammarRule { command ->
    if (command == "mở cốp") {
        RouteResult.Matched(
            Intent(
                name = "trunk_open",
                slots = emptyMap(),
                confidence = 1.0f,
                tier = Intent.Tier.T0,
            ),
        )
    } else {
        null
    }
}

val router = GrammarIntentRouter(extensionRules = listOf(trunkRule))
```

Sau khi đăng ký rule, cần bổ sung mapper/action ở module sở hữu domain, đưa action qua `CommandGateway` và `SafetyGuard`, rồi thêm test cho parse, slot, deny/confirm và kết quả thực thi. Extension chạy sau toàn bộ core rule và safety pre-filter, nên không thể ghi đè intent core hoặc khôi phục biến thể đã chủ động loại bỏ.

## Tài liệu Vòng 2

- [Product & Integration Card](vong2/12-PRODUCT-INTEGRATION-CARD.md)
- [5 tình huống phức tạp M7a](vong2/13-M7A-TINH-HUONG-PHUC-TAP.md)
- [Kịch bản demo 3 phút L8](vong2/14-KICH-BAN-DEMO-3-PHUT.md)
- [Quyết định benchmark ASR L10](vong2/15-QUYET-DINH-BENCHMARK-ASR.md)
- [N1 Claim–Evidence Map](vong2/18-CLAIM-EVIDENCE-MAP.md)
- [Runbook tổng duyệt C2 10 phút](vong2/19-TONG-DUYET-C2-10-PHUT.md)
- [Write-up câu chuyện AI Vòng 2](vong2/20-WRITE-UP-AI-VONG-2.md)
- [Q&A BGK theo Claim–Evidence Map](vong2/21-QA-BGK-VONG-2.md)
- [Slide pitch Vòng 2](docs/VIVA_Pitch_Vong2.pptx)
- [Plan cá nhân và các Device Integration Gate](vong2/07-PLAN-CA-NHAN-LONG.md)

## Mã nguồn mở và tài sản mô hình

- Vosk Android `0.3.75` — Apache-2.0; model EN/VI theo license của từng model upstream.
- ONNX Runtime Android `1.20.0` — MIT.
- Silero VAD `v6.2.1` — MIT; bản license được giữ tại `android/voice/third_party/silero-vad-LICENSE`.
- AndroidX, Kotlin, Coroutines, Hilt và Room — xem version catalog tại `automotive/gradle/libs.versions.toml` và license upstream tương ứng.
- Các WAV fallback tiếng Việt được tạo offline cho demo; không chứa credential hay dữ liệu người dùng.

Trước khi phát hành, đội phải đối chiếu lại license của từng model được đóng gói và hoàn tất Device Integration Gate; kết quả unit test/synthetic không thay thế bằng chứng chạy trên AAOS Device.
