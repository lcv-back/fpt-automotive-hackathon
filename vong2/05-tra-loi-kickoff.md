# Trả lời chị Linh — chốt lịch kick-off

> Gửi trong hôm nay 28/07. Mục tiêu: chốt được ngày, và **đẩy 5 câu hỏi sang trước** để mentor kịp tra
> thay vì nghe "để anh check lại rồi báo".

---

## Tin nhắn gửi vào thread

```
Dạ em chào chị Linh, chào anh Thủy và anh Đức ạ ^^

Đội em rất mong buổi kick-off này ạ! Em là Ngô Văn Long, đội trưởng team VIVA.

Về lịch, đội em xin đề xuất theo thứ tự ưu tiên:
  1. Thứ 5 (30/07), 19:00–20:00 — trùng khung office hours nên chắc mọi
     người tiện nhất ạ
  2. Thứ 4 (29/07), 19:30 — nếu các anh muốn sớm hơn
  3. Thứ 6 (31/07), 19:30

Cuối tuần (01–02/08) thì đội em xin phép né ạ, vì đó là 2 ngày đội dồn
sức code trước checkpoint C2 ngày 03/08. Các anh chọn giúp đội khung nào
tiện nhất là được ạ!

Em gửi trước 2 thứ để buổi kick-off đỡ mất thời gian tra cứu:

── ĐỘI ĐANG Ở ĐÂU ──
VIVA là trợ lý giọng nói tiếng Việt trên AAOS, phục vụ tài xế giao hàng
chặng cuối. Đội lấy Voice làm lớp điều phối, còn Media / Climate-VHAL /
DTC là các skill chạy trên tín hiệu xe thật.

Đội đã chạy 4 spike xác minh môi trường (mic, VHAL property, USB media,
REST/MCP API) và đã nhận được 3 câu trả lời rất giá trị từ các anh —
đặc biệt là việc đội được chủ động sửa design và thêm node. Điều này gỡ
được rủi ro lớn nhất của đội, và đội đã ánh xạ lại toàn bộ kiến trúc
theo luồng chuẩn anh gợi ý: app → car service → VHAL → CAN signal →
KUKSA databroker → CCU.

(Em đính kèm báo cáo 1 trang: đội làm được gì / đang vướng gì / cần gì.)

── 5 CÂU ĐỘI XIN HỎI Ở KICK-OFF ──
1. Quan trọng nhất: BGK chấm đội theo MỘT sản phẩm tích hợp, hay theo
   4 bộ tiêu chí riêng của 4 challenge ạ? Việc này quyết định đội có
   làm sâu đề DTC hay không, nên đội rất mong được rõ sớm.

2. Trong mốc C0 có nhắc "tài nguyên cần (GPU/LLM quota)" — đội có được
   cấp GPU cho container ASR tiếng Việt không ạ? Nếu không có thì đội
   sẽ chọn model nhẹ hơn cho phù hợp.

3. VM có outbound internet không ạ?

4. Đội xin bộ source của tutorial "Test Bench AEB Device-in-the-Loop"
   (Dockerfile + blueprint JSON + DBC/VSS) để học pattern Container
   Node, và repo cdc-starter nếu có ạ.

5. Đội xin xác nhận: khi đội clone blueprint "Started pack" rồi sửa
   trên bản clone và deploy lên Device của đội, có ảnh hưởng gì tới
   các đội khác không ạ? Đội muốn chắc chắn trước khi đụng vào.

Em cảm ơn chị Linh và các anh nhiều ạ!
Ngô Văn Long — Team VIVA
```

---

## Ghi chú nội bộ (không gửi)

**Vì sao đề xuất T5 30/07:**

- Trùng khung office hours mentor đã block sẵn → khả năng đủ người cao nhất
- Đội có 2 ngày để có nội dung thật mang đi: bảng signal đọc từ DBC + blueprint clone đã deploy
- Né cuối tuần 01–02/08 — cửa sổ full-capacity duy nhất trước C2

**Câu #5 là câu mới, không có trong danh sách 8 câu cũ.** Thêm vào vì đội sắp sửa blueprint thật;
hỏi trước rẻ hơn nhiều so với làm hỏng Room của người khác rồi mới biết.

**Đã bỏ khỏi danh sách hỏi** (tự tra được, không làm phiền mentor):

| Câu cũ | Vì sao bỏ |
|---|---|
| Tên node CCU + tên pin CAN | Tự tra: `GET /api/v1/deployments/:roomId/nodes` hoặc mở canvas blueprint |
| Danh sách Vehicle Property | Mentor đã trả lời: đọc DBC trong Artifacts |
| Host registry | Tài liệu walkthrough xác nhận `registry.carsky.io` |
| Mic có vào VM không | Mentor đã trả lời: có |

**Kick-off KHÔNG nằm trên đường găng.** Buổi này rơi vào ngày nào cũng được — đội vẫn chạy xương
sống đúng lịch 29–31/07. Chỉ câu #1 thực sự cần mentor, và nó chỉ ảnh hưởng tầng T3 (DTC) nằm
cuối hàng đợi.
