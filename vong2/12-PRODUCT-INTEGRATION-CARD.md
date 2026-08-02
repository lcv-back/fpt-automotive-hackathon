# PRODUCT & INTEGRATION CARD — VIVA

> **Owner:** Long · **Task:** N2 · **Ngày:** 01/08/2026  
> **Phạm vi:** VIVA — trợ lý giọng nói tiếng Việt chạy trên Android Automotive OS (AAOS).  
> Card này trả lời đúng 5 hạng mục của barem Vòng 2; không phải business case và không claim trạng thái tích hợp cao hơn bằng chứng hiện có.

---

## 1. Người dùng và người quyết định

| Vai | Đối tượng | Nhu cầu / quyết định |
|---|---|---|
| **User trực tiếp** | Tài xế dùng cockpit, ưu tiên tài xế Việt Nam và tài xế giao vận | Điều khiển điều hòa, cửa và media bằng giọng nói, giảm thao tác chạm và không phụ thuộc mạng cho core flow |
| **Buyer / người quyết định sản phẩm** | OEM hoặc Tier-1 sở hữu roadmap Digital Cockpit | Quyết định tích hợp module vào image/ứng dụng AAOS, cấp quyền privileged VHAL, duyệt an toàn và chịu trách nhiệm phát hành |
| **Process owner của pilot giao vận** | Quản lý vận hành / an toàn đội xe | Chọn nhóm tài xế thử nghiệm, định nghĩa tình huống vận hành và đánh giá tác động đến quy trình giao hàng |

**Ranh giới:** tài xế là người sử dụng nhưng không phải người cấp quyền VHAL hay quyết định tích hợp vào xe. VIVA không giả định bán ứng dụng trực tiếp cho tài xế ở giai đoạn này.

## 2. Offering và quan hệ tiếp nhận

**Offering:** một gói phần mềm tích hợp AAOS gồm:

- VIVA Agent: mic/VAD/ASR tiếng Việt, intent router, `SafetyGuard`, TTS và HMI;
- `VivaCarService`: service do đội sở hữu, dịch intent đã được phép thành `(PropertyID, areaId, value)`, giữ kết nối `CarPropertyManager` và fan-out callback;
- contract M2 và bộ test/trace để OEM/Tier-1 thêm intent mới mà không đưa intent xuống VHAL.

**Quan hệ tiếp nhận:** **B2B2C** — đội cung cấp module và integration kit cho OEM/Tier-1; OEM/Tier-1 tích hợp, platform-sign, kiểm thử và phát hành tới tài xế. Với pilot giao vận, fleet là process owner/co-design partner, không thay thế vai trò phê duyệt kỹ thuật của OEM/Tier-1.

## 3. Outcome và giả thuyết áp dụng

| Đối tượng | Outcome mong đợi | Giả thuyết cần kiểm chứng |
|---|---|---|
| Tài xế | Hoàn thành core command mà không chạm màn hình; phản hồi đúng trạng thái thực thi và vẫn dùng được khi mất mạng | **H1:** 5 lệnh xương sống đạt tỉ lệ hoàn thành ≥ 90% trong cabin/noise test của đội; **H2:** p95 end-to-end < 1.500 ms trên đường edge |
| OEM / Tier-1 | Có lớp voice-to-vehicle tách khỏi app UI và VHAL; thêm intent bằng contract thay vì sửa xuyên nhiều tầng | **H3:** một intent vehicle-control mới có thể được thêm bằng mapping + policy + test, không sửa VHAL và không để LLM sinh trực tiếp PropertyID |
| Fleet operations | Giảm thao tác tay/mắt trong các bước lặp lại và có policy chặn hành động không an toàn | **H4:** tài xế pilot hoàn thành kịch bản đại diện với ít thao tác chạm hơn baseline màn hình; process owner chấp nhận policy deny/confirm |

Các con số trên là **mục tiêu/giả thuyết**, chưa phải kết quả đo. Chỉ công bố kết quả sau khi có log/trace và protocol test tương ứng.

## 4. Tích hợp và phụ thuộc bên ngoài

### Quy ước trạng thái

- **THẬT:** dùng implementation thật trong source/core flow; nếu chưa có bằng chứng Device thì ghi rõ.
- **MÔ PHỎNG:** thay thế có chủ đích để phát triển hoặc demo; không được dùng làm bằng chứng tích hợp thật.
- **KẾ HOẠCH:** contract hoặc hướng triển khai đã chốt nhưng runtime integration chưa hoàn tất.

| Dependency / điểm nối | Trạng thái 02/08 | Bằng chứng hoặc giới hạn |
|---|---|---|
| VIVA Agent + `voice-core` trong app AAOS | **THẬT — source/build, chưa Device-verified** | Bridge `CoreIntentMapper` đã tích hợp; 139 unit test và hai APK variant build xanh bằng JDK 21, kiểm lại ngày 02/08 |
| ASR on-device (Vosk EN/VI) + intent routing | **THẬT — source, chưa đo trên Device** | Model/task Gradle và pipeline tồn tại trong `automotive/feature/voice`; chưa claim accuracy/latency thực tế |
| `SafetyGuard` trước vehicle execution | **KẾ HOẠCH — contract/trace đã chốt** | Voice/LLM chỉ sinh intent; guard quyết định allow/deny/confirm, nhưng implementation của Tùng chưa có trong repo |
| `MockVehicleRepository` | **MÔ PHỎNG** | Dùng cho emulator/unit test; không chứng minh core flow chạy trên CarSky |
| `VivaCarService` riêng + AIDL | **KẾ HOẠCH (M1)** | Contract M2 đã chốt; Tùng/Vĩ triển khai service và quyền privileged |
| VHAL/`CarPropertyManager` trên CarSky | **KẾ HOẠCH — real flavor có source** | Cần platform signing/privapp allowlist và xác nhận `setProperty` trả `Applied` trên Device |
| VHAL ↔ KUKSA/VSS ↔ CAN qua Script Node | **KẾ HOẠCH — contract verified** | M2 đã đối chiếu PropertyID, VSS và DBC; chưa có runtime trace CarSky/CAN |
| CCU nhận/gửi CAN | **MÔ PHỎNG** | Mentor cho phép mô phỏng; phải giữ đúng nhãn trong demo/write-up |
| `MediaSession` / `CarAudioManager` | **KẾ HOẠCH kiểm chứng tích hợp** | Media/volume không đi qua VHAL; cần smoke test riêng trên Device |

**Không phụ thuộc cloud cho core flow.** Network chỉ là dependency của bước tải model/build ban đầu, không phải dependency khi tài xế ra lệnh.

## 5. Bước kiểm chứng tiếp theo và rào cản lớn nhất

**Rào cản lớn nhất:** quyền privileged VHAL và khả năng cài APK/service lên đúng Device CarSky. Nếu không ghi được property thật, core flow không đủ bằng chứng platform L2 dù UI và mock chạy đúng.

**Validation gate kế tiếp — Device Integration Gate:**

1. ✅ Dùng JDK 21 build `mockDebug` và `realDebug`; 139 unit test xanh *(65 voice-core + 74 automotive)*, 0 failure/error/skipped (kiểm lại 02/08).
2. Cài bản `realDebug`/`VivaCarService` theo allowlist OEM trên Device CarSky.
3. Chạy 3 intent vehicle-control M2: đặt nhiệt độ 24°C, đặt fan mức 5, khóa cửa tài xế.
4. Với từng lệnh, chỉ tính thành công khi service trả `Applied`; lưu cùng `traceId`: intent → policy → PropertyID/area/value → VHAL callback → VSS/CAN evidence.
5. Đối chiếu `cmd car_service get-property-value`, app HVAC/DOOR và CAN/CCU mô phỏng; không gộp media/volume vào claim VHAL.

**Tiêu chí qua gate:** 3/3 lệnh đúng mapping, không có lệnh bị xác nhận “Đã…” trước `Applied`, log không crash, và có ít nhất một trace CarSky hoàn chỉnh cho mỗi intent. Nếu quyền VHAL thất bại, mở lại quyết định packaging/service với mentor thay vì thay bằng mock rồi khai là thật.

---

## Nguồn nội bộ

- Barem 5 ô và quy tắc gắn nhãn: `08-BAREM-VONG-2-CHINH-THUC.md` §1.5.
- Luồng mentor đã sửa và ranh giới intent/PropertyID: `11-PHAN-HOI-MENTOR-KICKOFF-30-07.md` §1–2.
- Mapping M2: `03-contracts.md` §0.2.
- Trạng thái build/Device và thứ tự công việc: `07-PLAN-CA-NHAN-LONG.md` §2, §6.
- Định vị sản phẩm ban đầu: `../Proposal_Vong1_VIVA_DigitalCockpit.md` slide 5–7.
