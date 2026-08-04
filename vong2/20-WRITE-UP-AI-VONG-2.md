# VIVA — Câu chuyện AI và bằng chứng Vòng 2

## 1. Tóm tắt

VIVA là trợ lý giọng nói tiếng Việt cho tài xế giao hàng chặng cuối. Mục tiêu không phải tạo thêm một chatbot
trong xe, mà giảm thao tác chạm: người lái nói một câu ngắn, hệ thống nhận giọng nói, hiểu ý định, chuyển thành
lệnh xe có kiểu dữ liệu rõ ràng và chỉ xác nhận thành công sau khi tầng thực thi trả kết quả.

Phần đội tự xây đã có bằng chứng ở mức source/JVM: push-to-talk, Silero VAD ONNX, hai biên ASR có thể thay thế,
router grammar cho 10 intent lõi, TTS tiếng Việt có 36 câu dự phòng, audio focus và trace latency theo từng chặng.
Push-to-talk, Silero VAD và `AsrClient` hiện là **module có test nhưng chưa nằm trên đường chạy APK**;
đường chạy thật dùng Vosk tự thu và tự endpoint. Xem `25-LECH-KIEN-TRUC-VOICE-PIPELINE.md`.
Tại snapshot E12, 139 test JVM chạy xanh, hai biến thể APK `mockDebug` và `realDebug` build thành công. Các claim
về CarSky, VHAL/CAN thật, SafetyGuard và p95 chỉ được nâng trạng thái sau khi có evidence từ Device.

## 2. Vấn đề người dùng

Tài xế van hoặc xe tải giao hàng phải liên tục kiểm tra chặng tiếp theo, điều chỉnh điều hòa, đổi nhạc và cập nhật
trạng thái công việc. Những tác vụ nhỏ này cạnh tranh trực tiếp với sự chú ý dành cho đường đi. Giao diện giọng nói
chỉ có giá trị khi thỏa ba điều kiện: hiểu tiếng Việt đủ ổn định, hoạt động khi mạng kém và không có đường tắt từ mô
hình xác suất xuống actuator.

Người dùng trực tiếp là tài xế. Bên quyết định tiếp nhận là doanh nghiệp giao vận/fleet hoặc OEM/Tier-1. Offering phù
hợp là một module tích hợp B2B/B2B2C vào cockpit AAOS hiện có, không phải một thiết bị phần cứng mới.

## 3. AI được dùng ở đâu — và không được dùng ở đâu

Luồng voice của VIVA gồm sáu chặng có thể quan sát:

`speech_start → speech_end → asr_done → intent_done → guard_done → exec_done → tts_start`

1. **Đường chạy hiện tại:** `VoskSpeechRecognitionEngine` mở `AudioRecord` với source
   `VOICE_RECOGNITION` và để Vosk tự quyết điểm cuối câu. APK không có VAD endpointer riêng
   (`VoiceAssistantService.kt:93`); `speech_start` là lúc bắt đầu nghe, không phải onset VAD.
2. **Kiến trúc đích đã có module/test nhưng chưa cắm:** `PushToTalkRecorder` thu PCM 16 kHz,
   `VadEndpointer` dùng Silero VAD ONNX, và `AsrClient` là boundary cho container `viva-asr`.
   Khoảng lệch và roadmap đóng nó nằm ở `25-LECH-KIEN-TRUC-VOICE-PIPELINE.md`.
3. `GrammarIntentRouter` ánh xạ câu tiếng Việt vào 10 intent lõi. Grammar là lựa chọn có chủ đích cho các lệnh xe:
   nhanh, chạy offline, dễ kiểm thử và không tự sinh hành động ngoài contract.
4. Intent được chuyển qua gateway thực thi. `LatencyTrace` ghi từng mốc và verdict để không biến timeout hoặc lỗi
   thành một lượt “thành công”.
5. TTS dùng engine Android khi có voice `vi-VN`; nếu thiếu, hệ thống phát câu pre-render từ `res/raw`.
6. `AndroidAudioFocusController` yêu cầu transient focus khi nói và trả focus sau khi hoàn tất.

AI chỉ chịu trách nhiệm nhận biết và đề xuất intent. Quyền thực thi phải thuộc về boundary tất định: intent có schema,
giá trị được kiểm tra miền, policy an toàn đối chiếu trạng thái xe và adapter chỉ báo thành công sau callback thực.
Ở snapshot hiện tại, SafetyGuard và service boundary hoàn chỉnh chưa có evidence; vì vậy C-SAFETY vẫn là claim bị
khóa, không được trình bày như tính năng đã tích hợp.

## 4. Xử lý tình huống phức tạp

Đội viết expected behavior trước khi tích hợp để tránh “demo chạy sao thì sửa claim theo vậy”:

| Tình huống | Hành vi | Trạng thái hiện tại |
|---|---|---|
| “Quạt mạnh lên” thiếu mức | Hỏi đúng một câu: mức 0–5 | Đã tích hợp ở router/JVM |
| “Đặt bàn ăn tối” ngoài phạm vi | Từ chối lịch sự, không gọi gateway | Đã tích hợp ở router/VoiceAgent |
| “Nóng quá” mơ hồ | Hỏi setpoint; chỉ tự suy ra khi đọc được trạng thái hiện tại | Baseline mô phỏng; target chờ service |
| Hai lệnh trong một câu | Giữ thứ tự, guard từng lệnh, báo partial result | Kế hoạch |
| “Mở cửa” khi xe chạy | Từ chối với rule `G1_SPEED_LOCK`, không gọi setter | Kế hoạch, chờ SafetyGuard/Device |

Hai tình huống đầu đã đủ để chứng minh hệ thống không im lặng, không đoán slot và không rơi tự do xuống `unknown`.
Ba tình huống sau chỉ được đánh dấu hoàn tất khi có đúng dependency và evidence end-to-end.

## 5. Quyết định kiến trúc quan trọng

Proposal ban đầu nêu so sánh edge-only và hybrid. Sau spike, đội loại cloud LLM khỏi core flow vì network dependency
trái với mục tiêu offline và làm tăng rủi ro vượt ngân sách phản hồi 1,5 giây. Đội không dựng một đường giả để giữ
claim cũ. Trục ablation mục tiêu là Vosk on-device và `viva-asr` container, nhưng snapshot
04/08 chưa thoả tiền đề cùng PCM/cùng endpoint và container chưa cắm. Vì vậy Vòng 2 chưa
công bố số so sánh; thiết kế đo giữ cùng dữ liệu, mức nhiễu, WER/intent accuracy và p50/p95.

Đường mặc định chỉ được chọn sau benchmark. Cold run và steady-state phải tách riêng; timeout/lỗi vẫn nằm trong mẫu;
model, config và commit phải cùng identity với APK/video được nộp.

## 6. Phần đội tự xây và baseline

| Thành phần | Phân loại | Bằng chứng hiện có |
|---|---|---|
| AAOS/Car APIs, AudioRecord, Android TTS | Platform cung cấp | Source/build |
| Push-to-talk + WAV + Silero VAD ONNX | Team-owned — **module chưa nằm trên đường chạy APK** | Unit test JVM |
| ASR: Vosk trên đường chạy; `AsrClient` container chưa cắm | Team-owned/tích hợp thư viện | Unit test/build; Device pending |
| Grammar router 10 intent + extension point | Team-owned | Unit test JVM |
| Latency trace + verdict/error attribution | Team-owned | Golden fixture + unit test |
| TTS fallback 36 câu + audio focus | Team-owned | Resource/test JVM; duck thật pending |
| VivaCarService/SafetyGuard/VHAL-CAN | Team-owned mục tiêu | Chưa đủ evidence ở snapshot này |

Điểm khác biệt không nằm ở số lượng màn hình hay intent. Nó nằm ở boundary rõ, offline core có thể kiểm thử, error
được quy trách nhiệm đúng stage, và khả năng chứng minh từng claim bằng artifact có tên.

## 7. Evidence và giới hạn claim

| Claim | Mức được phép nói ở snapshot hiện tại | Evidence |
|---|---|---|
| C-VOICE | Source/JVM: pipeline và 10 intent lõi tồn tại | E12 + test module voice |
| C-ERROR | Source/JVM: timeout/lỗi có stage và lượt lỗi không được ghi thành công | E12 + golden trace |
| C-MODULAR | Source/JVM: thêm grammar rule không sửa core | E12 + README |
| C-HVAC/C-DOOR/C-MEDIA | Chưa claim Device | Chờ E05/E07/E08 |
| C-SAFETY | Chưa claim | Chờ SafetyGuard + E06 |
| C-LATENCY | Chưa công bố p50/p95 | Chờ benchmark E04 |
| C-PLATFORM | Chưa nói “core flow chạy trên CarSky” | Chờ E09/E11 |

Sự minh bạch này là một phần của thiết kế đánh giá: mock có thể dùng để phát triển, nhưng không được đổi nhãn thành
“đã tích hợp”; log local không được gọi là evidence CarSky; số dự toán không được gọi là benchmark.

## 8. Giá trị triển khai và bước kiểm chứng tiếp theo

Giả thuyết sản phẩm là thao tác rảnh tay sẽ giảm thời gian mắt rời khỏi đường và giảm số lần chuyển ngữ cảnh giữa
cockpit với ứng dụng giao vận. Điều này chưa phải kết luận thị trường. Bước kiểm chứng tiếp theo là cho tài xế/fleet
ops thử một kịch bản chặng giao, đo completion time, lỗi hiểu intent và số lần phải chạm màn hình.

Rào cản lớn nhất hiện tại không phải thêm intent mà là quyền và đường đi thật `APP → service fw → PropertyID →
VHAL → CAN`, cùng evidence từ đúng Device. Nếu gate đó chưa mở, bản demo phải dùng mock có nhãn và hạ mọi claim
full-stack khỏi slide, video và write-up.

## 9. Prompt và kiểm thử có điều khiển

Core flow hiện không cần LLM runtime. Nếu tầng T2 được bật lại cho câu tự do, prompt chỉ được phép làm một việc:
đề xuất intent trong whitelist theo schema, không mô tả rằng hành động đã được thực thi. Contract prompt dự kiến là:

```text
Bạn là bộ phân loại intent trong xe, không phải tầng thực thi.
Chỉ trả một intent nằm trong whitelist và slots đúng schema.
Thiếu slot thì trả clarification; ngoài phạm vi thì trả unsupported.
Không được tuyên bố actuator đã đổi trạng thái.
```

Output này vẫn phải qua validation, policy và gateway như grammar intent. Prompt injection không thể tạo thêm function
hay bỏ qua guard vì danh sách action nằm trong code, không nằm trong nội dung người dùng.

MCP-driven testing là hướng **kế hoạch**, chưa phải evidence ở snapshot E12. Harness dự kiến đặt trạng thái xe bằng
`send_signals`, phát/replay câu thử, chụp HMI bằng `screenshot`, kiểm text bằng `find_text` và lưu PASS/FAIL cùng trace.
Cho đến khi chuỗi này chạy trên đúng Device, các unit test và mock hiện tại chỉ chứng minh tầng source/JVM.

## 10. AI hỗ trợ tốt ở đâu và sai ở đâu

**AI hỗ trợ tốt ở đâu.** AI rút ngắn các việc có ranh giới rõ và có oracle để kiểm: mở
rộng grammar cho 10 intent cùng test biến thể; dựng logic endpointer quanh Silero; và viết
parser/aggregator cho `VIVA_TRACE` khớp fixture. Với những việc này, test hoặc format log có
thể phủ định ngay một kết quả sai.

**AI sai ở đâu.** Quy trình phát triển có AI hỗ trợ đã tạo ra hai nhánh hợp lý khi đọc riêng
nhưng không nối với nhau. `android/voice` có push-to-talk, Silero VAD và `AsrClient`; app
`automotive/` lại đi đường ngắn chạy được: Vosk tự mở `AudioRecord`, tự endpoint, rồi vào
grammar. `SafetyGuard` chỉ tồn tại trong contract/verdict. Unit test của từng module đều xanh,
nên không test cục bộ nào phát hiện rằng `audio/` và `asr/` không được app tham chiếu.

**Phát hiện và xử lý.** Đội tìm ngược từng symbol từ `android/voice` sang `automotive/`, rồi
đối chiếu comment runtime tại `VoiceAssistantService.kt:93`. Bài học là unit test xanh chỉ
chứng minh component đúng, không chứng minh component được dùng. Trước freeze, đội chọn khai
đúng thay vì vá vội: tách nhãn integration, sửa Claim–Evidence Map, write-up và slide; ghi
tiền đề benchmark chưa thoả; đưa một capture stream dùng chung, live VAD driver và audio
benchmark vào roadmap tại `25-LECH-KIEN-TRUC-VOICE-PIPELINE.md`.

## 11. Hướng sau Vòng 2

DTC/UDS được giữ như hướng Vehicle Middleware cho Vòng 3, nơi cross-vertical có dòng điểm riêng. Ở Vòng 2, đội ưu
tiên hoàn tất core flow, SafetyGuard, benchmark và evidence thay vì mở rộng bề ngang. Sau khi có Device evidence,
framework grammar/skill có thể mở rộng sang coaching, OTA hoặc tác vụ giao vận mà không thay core voice pipeline.

## 12. Tài liệu truy vết

- Claim–Evidence Map: `vong2/18-CLAIM-EVIDENCE-MAP.md`
- Product & Integration Card: `vong2/12-PRODUCT-INTEGRATION-CARD.md`
- Kịch bản demo 3 phút: `vong2/14-KICH-BAN-DEMO-3-PHUT.md`
- Quyết định benchmark ASR: `vong2/15-QUYET-DINH-BENCHMARK-ASR.md`
- Evidence JVM/build: `evidence/c2/jvm-test-summary.txt`
