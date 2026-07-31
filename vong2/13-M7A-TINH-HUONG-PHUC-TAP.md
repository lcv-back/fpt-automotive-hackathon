# M7a — Năm tình huống phức tạp và hành vi mong đợi

Ngày chốt: 01/08/2026. Đây là đặc tả **expected behavior trước khi hoàn tất M7b**, không phải tuyên bố
mọi dòng đã chạy end-to-end. Trạng thái dùng đúng ba nhãn nộp bài: `Đã tích hợp`, `Mô phỏng`, `Kế hoạch`.

| ID | Tình huống / câu thử | Hành vi mong đợi | Không được làm | Trạng thái và evidence gate |
|---|---|---|---|---|
| M7-01 | Mơ hồ: **“nóng quá”** | Nhận ra người dùng muốn mát hơn. Baseline an toàn hỏi đúng một câu: **“Bạn muốn giảm nhiệt độ điều hòa xuống bao nhiêu độ?”**. Target state-aware sau M1 đọc setpoint hiện tại, giảm 1°C trong miền 16–32°C và nói rõ giá trị suy ra | Không tăng nhiệt độ; không tự chọn một setpoint khi chưa đọc được trạng thái xe | **Mô phỏng**: baseline clarification đã có unit test. **Kế hoạch**: target state-aware chờ `VivaCarService`/snapshot của Tùng |
| M7-02 | Ghép lệnh: **“khóa cửa rồi hạ điều hòa xuống 22 độ”** | Tách thành hai intent có thứ tự; từng intent đi qua SafetyGuard; báo rõ kết quả từng bước nếu chỉ một bước thành công | Không bypass guard; không nói “đã xong cả hai” khi bước hai lỗi | **Kế hoạch**: contract hiện trả một `Intent`; cần ordered command envelope + gateway của M1 trước khi code |
| M7-03 | Không an toàn: **“mở cửa”** khi `Speed=60 km/h` | `Deny:G1_SPEED_LOCK`; TTS: **“Xe đang chạy, mình chưa mở cửa được. Bạn dừng hẳn rồi nói lại nhé.”**; không gọi property setter | Không chỉ báo lỗi chung; không đổi `DOOR_LOCK` | **Kế hoạch**: contract/log format đã chốt; chạy thật chờ SafetyGuard + snapshot tốc độ của Tùng |
| M7-04 | Thiếu slot: **“quạt mạnh lên”** | Nhận đúng nhóm HVAC và hỏi đúng một câu: **“Bạn muốn đặt quạt ở mức mấy, từ 0 đến 5?”**; câu trả lời “mức 3” tạo `hvac_set_fan(level=3)` | Không tự chọn mức; không rơi vào `unknown` | **Đã tích hợp ở router**, unit test JVM; Device/TTS là integration gate |
| M7-05 | Ngoài phạm vi: **“đặt bàn ăn tối”** | Từ chối lịch sự, nói ngắn gọn phạm vi hỗ trợ; `Deny:G3_UNSUPPORTED`; không gọi gateway | Không im lặng; không gửi câu này xuống VHAL/Media | **Đã tích hợp ở router/VoiceAgent**, unit test JVM; Device/TTS là integration gate |

## Thứ tự hiện thực M7b

1. Chạy M7-04 và M7-05 trong kịch bản L8 vì đã độc lập với thành viên khác.
2. Khi SafetyGuard có snapshot thật, thêm M7-03 và lưu log `Deny:G1_SPEED_LOCK` làm evidence ablation A1.
3. Chỉ mở M7-01 target và M7-02 sau khi M1 cung cấp state/gateway; không mở intent mới để “làm cho đủ bảng”.

## Tiêu chí hoàn tất

- Mỗi dòng có test tự động ở tầng sở hữu và một evidence ID trên Device khi dependency sẵn sàng.
- TTS chỉ nói “Đã…” sau `CommandResult.Applied`; denial/partial result dùng câu riêng.
- M7-01/M7-02 còn nhãn `Kế hoạch` thì M7b **chưa được đánh dấu hoàn tất**.
