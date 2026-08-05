# Phiên 22 câu — sau khi đóng B20 và nối bộ điều khiển âm lượng

> 05/08/2026 16:53 (+07). Đọc `evidence/emulator/README.md` trước:
> **đây là emulator, không phải CarSky.**

## Kết quả

```
22 cases · 17 PASS · 5 FAIL · 0 MISSING (5 of them known gaps)
```

Diễn biến qua ba phiên trong chiều nay:

| | 16:16 | 16:34 | phiên này |
|---|---|---|---|
| PASS | 16 | 16 | **17** |
| B20 intent | `vehicle_status_speed` ❌ | `unknown` ✅ | `unknown` ✅ |
| B20 verdict | `Allow` ❌ | `Error:nlu_done` ⚠️ | **`Deny:G3_UNSUPPORTED`** ✅ |

## B20 đã đóng — vì hợp đồng vốn đã định nghĩa nó

`03-contracts.md` §4 ghi sẵn: `G3_UNSUPPORTED` = *"wake phrase/trợ lý khác hoặc
câu nằm ngoài 10 intent lõi"* → *"Deny — nói rõ phạm vi, không gọi Skill"*.
Luật này chưa từng được hiện thực; app trả về một `CommandValidationException`
bằng **tiếng Anh** (*"Sorry, I didn't understand…"*).

Chỗ áp luật là nơi điều phối intent, **không** phải `GuardedVehicleRepository`:
một câu ngoài phạm vi không sinh ra lệnh ghi property nào để mà chặn ở biên đó.

Tài xế giờ nghe: *"Mình chỉ hỗ trợ điều hoà, cửa xe, âm lượng, nhạc và lộ trình
giao hàng. Bạn thử nói lại theo một trong các nhóm đó nhé."*

⚠️ Câu này **chưa có clip TTS dựng sẵn**, nên trên image không có giọng vi-VN nó
phát ra một tiếng ping. Cần Long render thêm một WAV — cùng danh sách với câu hỏi
xác nhận mở cửa.

## Năm câu còn FAIL — không câu nào sửa được bằng mã app hôm nay

### B11 · B12 — âm lượng: nền tảng không cho

Đã nối thật `AudioManager` (không còn `CommandNotWiredException` giả định), và
**đo được lý do**:

```
AudioManager.isVolumeFixed = true
AudioManager: getStreamVolume(STREAM_MUSIC) = 15, max = 15   (cứng, không đổi)
dumpsys car_service: CarVolumeGroup(0) gain index 32/38      (nơi âm lượng thật nằm)
```

`cmd media_session volume --stream 3 --set 7` cũng không đổi được gì. Âm lượng
thuộc **CarAudioService**, muốn chạm phải qua `CarAudioManager.setGroupVolume`,
mà API đó đòi `CAR_CONTROL_AUDIO_VOLUME` — signature|privileged, tức phải cài app
vào `/system/priv-app`. Đó chính là **M1a**, nút thắt đang chặn cả E06–E09.

Một cái bẫy đã suýt lọt: lúc đầu, khi âm lượng đang ở 15/15, câu *"tăng âm
lượng"* trả lời *"đã ở mức cao nhất rồi"* → verdict `Allow` → **B11 PASS**. Bảng
xanh cho một tính năng không điều khiển được gì. Nay `isVolumeFixed` được xét
**trước** nhánh biên, nên nó ra `Error:exec_done` — đỏ mà thật.

### B13 · B14 · B15 — media: không có gì để điều khiển

```
dumpsys media_session:
  com.android.car.media.localmediaplayer  state=ERROR(7)  error=Missing permission.
  com.google.android.bluetooth            state=ERROR(7)  error=Bluetooth audio disconnected
  (không session nào active=true)
```

Trên image này không có phiên phát nhạc nào sống. `dispatchMediaKeyEvent` sẽ gửi
phím vào hư không và **không có cách nào biết có ai nhận hay không** — mọi lời
khai `Allow` sẽ là lời khai không kiểm chứng được. Muốn đóng B13–B15 cần một app
nhạc thật đang phát, tức D7 + một media session hoạt động, không phải vài dòng
mã.

## Nói ngắn gọn về 5 câu còn đỏ

Chúng đỏ vì **cùng một nút thắt với toàn bộ phần còn lại của dự án**: chưa cài
được app ở chế độ privileged (M1a), và chưa có nội dung/nền tảng thật để điều
khiển. Chúng không đỏ vì thiếu mã trong app — mã đã nối tới đúng API của nền
tảng và đang báo cáo trung thực thứ nền tảng trả lời.
