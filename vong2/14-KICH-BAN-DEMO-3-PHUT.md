# L8/M8 — Kịch bản demo 3 phút không cắt

Trạng thái: **văn bản đã chốt; router dry-run đã có test; rehearsal trên Device đang chờ integration gate**.
Mục tiêu là sáu câu thoại tối đa, có hai tình huống M7, có media và có đường thoát khi một lệnh lỗi.

## Preflight ngoài thời lượng quay

- APK/config/commit đúng artifact identity; logcat đã xóa và lọc `VIVA_TRACE` + crash.
- Xe đứng yên, số `P`, cửa khóa; HVAC 26°C, quạt mức 1; một bài nhạc đang phát.
- Mở HMI chính và Signal Watch. Người demo không chạm chuột sau khi bắt đầu trừ nút push-to-talk.

## Timeline chính

| Mốc | Người demo nói/làm | Kết quả nhìn/nghe được | Claim/evidence |
|---|---|---|---|
| 0:00–0:15 | Giới thiệu: “VIVA xử lý giọng nói tại edge; mọi lệnh xe phải qua service và safety gate.” | HMI + trạng thái ban đầu | C-ARCH |
| 0:15–0:35 | **① “Viva ơi, hạ điều hòa xuống 24 độ.”** | TTS xác nhận setpoint mục tiêu; HVAC mở; HMI 24°C; trace đủ chặng khả dụng | C-HVAC |
| 0:35–0:55 | **② “Quạt mạnh lên.”** | Không đoán: hỏi “mức mấy, từ 0 đến 5?” | M7-04 / C-ERROR |
| 0:55–1:10 | **③ “Quạt mức 3.”** | TTS xác nhận, HMI và Signal Watch về mức 3 | C-HVAC |
| 1:10–1:30 | **④ “Chuyển bài.”** | MediaSession chuyển track; TTS duck nhạc rồi nhả focus | M8 / C-MEDIA |
| 1:30–1:50 | **⑤ “Đặt bàn ăn tối.”** | Từ chối lịch sự; không có property/media action | M7-05 / C-ERROR |
| 1:50–2:10 | **⑥ “Khóa cửa.”** | Door state phản chiếu trên HMI; không claim CAN nếu chỉ chạy mock | C-DOOR |
| 2:10–2:45 | Mở trace summary/Signal Watch đã chuẩn bị sẵn trong UI demo | Chỉ ra intent, verdict và latency; phân biệt `Đã tích hợp`/`Mô phỏng` | C-OBS |
| 2:45–3:00 | Kết: “Không có đường tắt từ AI xuống xe; LLM chỉ đề xuất intent.” | Dừng đúng trước 3:00 | C-SAFETY |

## Đường thoát không dừng quay

- **Một lệnh không phản hồi sau 3 giây:** nói “Lượt này timeout và đã được ghi `Error:<stage>`; tôi tiếp tục
  bằng trạng thái trước đó”, rồi sang dòng kế tiếp. Không lặp quá một lần.
- **Media adapter chưa sẵn:** vẫn nói câu ④ để chứng minh router/trace, chỉ rõ nhãn `Kế hoạch`; không giả vờ
  track đã đổi. Trước bản nộp chính thức phải thay bằng adapter thật hoặc bỏ claim C-MEDIA.
- **VHAL/CarSky lỗi:** chuyển sang APK mock nhưng nói rõ “mô phỏng”; không dùng câu “full-stack tới CAN”.
- **TTS không có tiếng:** chỉ HMI text + cue dự phòng; người demo không tự đọc thay câu xác nhận như thể hệ thống nói.

## Gate còn lại trước khi đánh dấu L8 hoàn tất

- [ ] Chạy nguyên kịch bản một lần trên đúng Device, không dừng.
- [ ] Câu ④ đổi track thật và nhạc duck trong lúc TTS.
- [ ] Lưu log/ảnh theo các evidence ID của Claim–Evidence Map.
- [ ] Tổng thời lượng thực tế ≤ 3:00, không chỉ là ước lượng trong bảng.
