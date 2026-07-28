# Tin nhắn gửi chị Linh + anh Thủy + anh Đức

> Gửi vào đúng thread chị LinhNT169 đã brief. Gửi **trước 12:00 ngày 26/07** để kịp có câu trả lời trước office hours Thứ 3 (28/07).
> Kiểm tra trước khi gửi: link Google Drive proposal đang ở chế độ "Anyone with the link can view".

---

```
Em chào chị Linh, chào anh Thủy và anh Đức ạ!

Em là Ngô Văn Long, đội trưởng team VIVA. Cảm ơn chị Linh đã kết nối,
rất mong được các anh đồng hành cùng đội trong 2 tuần tới ạ.

Em xin brief nhanh định hướng sản phẩm và nhờ các anh xác nhận giúp
đội một vài điểm để tránh đi lệch hướng ạ.

── ĐỊNH HƯỚNG SẢN PHẨM ──
VIVA là trợ lý giọng nói tiếng Việt trên AAOS, phục vụ persona tài xế
giao hàng chặng cuối. Đội chọn Voice-Controlled Assistant làm lớp điều
phối trung tâm; Media Player, Climate/VHAL và DTC Monitor được tích hợp
thành skill điều khiển bằng giọng nói, chạy trên tín hiệu xe thật
(VHAL / MediaSession / UDS) chứ không mock UI. Toàn bộ lệnh đi qua một
tầng Safety Guard tất định, đối chiếu trạng thái xe thật trước khi thực thi.

→ Nhờ anh xác nhận giúp: BGK sẽ chấm đội theo MỘT sản phẩm tích hợp,
  hay theo 4 bộ tiêu chí riêng của 4 challenge ạ? Việc này ảnh hưởng
  trực tiếp tới cách đội phân bổ thời gian trong 2 tuần còn lại.

→ Tiện thể em xin xác nhận lại danh sách đề đã đăng ký của đội đúng là
  4 đề: Media Player, Climate Control VHAL, Voice-Controlled Assistant,
  DTC Monitor ạ (bản brief bị lặp tên đề Voice nên em muốn chắc chắn).

── 8 CÂU HỎI CẦN GỠ SỚM ──
1. Tài liệu tiêu chí: Guideline CDC có nhắc file Hackathon_IVI_CDC_2026.md
   chứa bảng tiêu chí chấm và danh sách API/property bắt buộc theo từng đề.
   Đội xin file này được không ạ? (Guideline đánh số Đề 1–10, webinar đánh
   số 4 challenge — đội chưa rõ mình đang ở hệ đánh số nào.)

2. Vehicle Property: Xin danh sách property đã được wire trong blueprint/Room
   của đội — đặc biệt HVAC_POWER_ON, HVAC_TEMPERATURE_SET, HVAC_FAN_SPEED,
   DOOR_LOCK, PERF_VEHICLE_SPEED, GEAR_SELECTION, NIGHT_MODE. Nếu thiếu
   property nào, đội xin được bổ sung sớm vì đây là đường găng của cả
   Climate skill lẫn Safety Guard ạ.

3. Quyền Nydus: Đội có quyền editor để THÊM một Container Node vào Room
   (chạy service ASR tiếng Việt của đội) không, hay việc này do team hạ
   tầng thực hiện ạ? Nếu được, đội dự kiến nối qua pin ethernet vào
   Ethernet Bridge để app trong VM gọi service qua HTTP.

4. Đường vào microphone: App Android chạy trong Skycraft VM có đọc được
   audio qua AudioRecord khi bật Microphone trên widget Screen không ạ?
   Nếu không, BTC khuyến nghị đường nào cho đề Voice-Controlled Assistant?
   (Đội thấy widget Text-to-Speech chỉ có giọng zh-TW và en-US, chưa có
   tiếng Việt, nên phần TTS đội sẽ tự lo.)
   Đội cũng thấy CarSky SDK có probe `wavelink` (ALSA → Opus) và Outpost
   hỗ trợ pin kind `audio` — hướng này có khả dụng cho đội thi không ạ?

5. Kết nối ra ngoài: VM có outbound internet không ạ? Đội cần biết để
   quyết định giữ hay bỏ tầng cloud LLM fallback.

6. Nguồn DTC: Blueprint của đội có Script Node nào chạy nydus.uds.server
   (có sẵn danh sách DTC) không ạ? Và pin CAN của đội tên là gì để đội
   biết đường socket ISO-TP /run/nydus/uds-<pin>.sock cần kết nối?

7. Registry: Host chính xác để docker push của đội là registry.carsky.io
   hay registry.hackathon-1.carsky.io/<team> ạ? (Hai tài liệu ghi khác nhau.)

8. Source mẫu: Đội xin bộ source của tutorial "Test Bench AEB
   Device-in-the-Loop" (Dockerfile + blueprint JSON + DBC/VSS) để học
   pattern Container Node, và repo cdc-starter / repo mẫu CDC nếu có ạ.

Đội đang chạy 4 spike xác minh trong hôm nay (micro, VHAL property,
USB image cho media, REST/MCP API) và sẽ mang kết quả tới office hours
Thứ 3 (28/07) để xin ý kiến các anh.

Em cảm ơn các anh ạ!
Ngô Văn Long — Team VIVA
```

---

## Ghi chú nội bộ (không gửi)

| Câu hỏi | Ảnh hưởng nếu câu trả lời là "không" |
|---|---|
| **#3 quyền Container Node** | ASR phải chạy trên laptop đội qua `adb reverse` — mất điểm "tận dụng nền tảng", demo phụ thuộc máy cá nhân |
| **#4 microphone** | Chuyển sang PA-B: audio bắt ở host, đẩy text vào app. **Phải ghi rõ trong write-up**, không giấu |
| **#2 property** | Climate skill đi vòng qua CAN signal `HvacCommand/Driver_Temperature` đã có sẵn trong Started pack |
| **#6 DTC/UDS** | Tự viết UDS server bằng Lua nếu được cấp Script Node, hoặc mô phỏng in-app (mất điểm "simulation fidelity") |

Câu **#1** và **#3** là hai câu quan trọng nhất — đừng để trôi qua office hours 28/07 mà chưa có câu trả lời.
