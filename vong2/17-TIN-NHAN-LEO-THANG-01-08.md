# Tin nhắn leo thang — 01/08/2026

Hai việc quá hạn từ 31/07 và một câu hỏi chặn gửi mentor. **Gửi hôm nay, không đợi standup 21:30.**

---

## A. Nhắn nhóm đội — gửi ngay

> Chào cả nhà, mình chốt lại tình hình sáng nay cho rõ.
>
> Hai việc của mốc cân tối qua **vẫn chưa có câu trả lời**, và cả hai đều đứng trên đường găng:
>
> **① Tùng — M1a: spike quyền VHAL.** APK thường gọi `setProperty(HVAC_TEMPERATURE_SET)` có bị từ chối
> quyền `CONTROL_CAR_CLIMATE` không? Mình chỉ cần **một dòng logcat** cho thấy property đổi thật, hoặc
> một dòng báo `SecurityException`. Việc này 1.5h.
> Nói thẳng vì sao mình sốt ruột: nếu không set được property thì **không có 6 chặng**, và dồn người
> vào cũng không cứu được — phải đổi cách tiếp cận. Mình muốn biết điều đó hôm nay chứ không phải 05/08.
>
> **② Dương — M6: một APK lên Device AAOS.** Dù app trống rỗng cũng được. Hiện `app-mock-debug.apk` và
> `app-real-debug.apk` đã build xanh trên máy mình, nên phần "có APK để cài" **không còn là việc phải làm nữa** —
> chỉ còn cài lên Device và chụp lại màn hình + logcat.
>
> Ai vướng ở đâu thì nhắn trong ngày, đừng để tới 21:30. Nếu vướng hạ tầng (chưa vào được Room, chưa có
> Device) thì báo mình, mình hỏi chị Linh luôn trong hôm nay.
>
> Cập nhật phần mình: grammar 10 intent · TTS · audio focus · VAD đã xong và đã **nối vào app** — app giờ
> phát log `VIVA_TRACE` mỗi lượt nói, nên Vĩ có dữ liệu thật để chạy harness ngay khi có Device.

---

## B. Nhắn mentor (anh Đức / anh Thủy, cc chị Linh) — gửi hôm nay, không đợi office hours 04/08

> Em chào anh ạ. Đội VIVA xin hỏi anh sớm một câu vì nó đang chặn phần xương sống, không đợi được tới
> buổi 04/08 ạ.
>
> **Device AAOS trên CarSky có phải bản `userdebug` và `adb root` / `adb remount` được không ạ?**
>
> Bối cảnh: theo luồng anh sửa hôm kick-off, đội đang dựng `VivaCarService` làm tầng *service fw*, giữ một
> kết nối `CarPropertyManager` và sở hữu bảng `intent → PropertyID`. Nhưng các quyền
> `android.car.permission.CONTROL_CAR_*` là privileged — nếu APK thường bị từ chối `setProperty`, đội phải
> cài vào `/system/priv-app` kèm allowlist XML, và việc đó cần `adb root` / `remount`.
>
> Nếu Device **không** cho remount thì đội xin anh hướng đi thay thế ạ — đội đã chuẩn bị sẵn file
> `privapp-permissions-com.sopa.viva_automotive.xml` và cả hai biến thể APK (`mock` / `real`), nên chỉ cần
> anh chỉ đúng đường là đội làm theo được ngay.
>
> Hai câu nữa em xin giữ tới buổi 04/08 ạ: (e) đội được sửa tới đâu trên guest AAOS, và (f) Script Node
> IVI Gateway / PWT Gateway thì clone rồi sửa mapping có đúng cách anh gợi ý không ạ.

---

## C. Sau khi gửi

| Nếu nhận được | Thì |
|---|---|
| ✅ remount được | Tùng cài privileged theo hướng dẫn, M1a đóng trong ngày, giữ nguyên đường **B** ở `07` PHẦN 5 ⑯ |
| ❌ không remount được | **Mở lại quyết định ⑯ ngay.** Đường A (build lại image) vẫn loại vì không đủ thời gian → phương án còn lại là demo trên biến thể `mock` và khai nhãn *mô phỏng* trung thực ở N5, đồng thời hạ claim "full-stack tới CAN" khỏi write-up |
| 🕐 không trả lời trong 24h | Hỏi lại qua chị Linh (kênh liên lạc chính), và giả định trường hợp ❌ để không mất thêm ngày |

> Ghi lại câu trả lời vào `11-PHAN-HOI-MENTOR-KICKOFF-30-07.md` PHẦN 8 ngay khi có — đó là nguồn,
> và nó sẽ được dùng lại ở Baseline Manifest N3 (nhãn `provided/configured/modified/new`).
