# BRIEF NGẮN GỬI MENTOR TRƯỚC KICK-OFF
### Team VIVA · 29/07/2026 · Long gửi tiếp sau tin nhắn của Vĩ trong nhóm

> **Mục đích:** anh Đức đọc 2 phút này trước, để khi mở file `06-PHAN-CONG-4-NGUOI.md`
> đã có sẵn khung trong đầu — biết bỏ qua phần nào, dừng lại ở phần nào.
> Cấu trúc: tổng → cụ thể. Kết bằng 3 câu hỏi để tối mai có cái mà trao đổi.

---

## BẢN CHÍNH (dán vào nhóm)

Em brief nhanh 2 phút để tối mai anh đỡ mất công đọc file dài ạ 🙏

**1. Tụi em đang làm gì**
Digital Cockpit, trục chính là **Challenge #3 — trợ lý giọng nói tiếng Việt ("Vivi ơi") trên AAOS**.
Media, HVAC/VHAL và tầng Safety Guard là các skill gắn quanh trục voice đó — tụi em **không làm 4 đề
song song**. Thứ phải chứng minh được là một luồng chạy thật trên CarSky: nói tiếng Việt → intent →
VHAL → CAN signal → CCU, không mock chặng nào.

**2. File em gửi là file gì**
Không phải proposal ạ — đó là **bảng phân công 13 ngày cuối cho 4 người**: mỗi task có người chịu
trách nhiệm, có định nghĩa "xong" (= commit trên main + bằng chứng log/ảnh), có deadline cứng/mềm.
Nó dài vì liệt kê đủ ~60 task, nhưng **anh không cần đọc PHẦN 3** (bảng task chi tiết từng người) —
đó là phần nội bộ tụi em tự quản.

**3. Nếu anh chỉ có 5 phút, đây là 3 chỗ tụi em cần anh nhất**
- **PHẦN 5 — Ai chờ ai:** sơ đồ phụ thuộc. Đường găng dài nhất là **Script Node Luau nối VHAL ↔ CAN,
  hạn 30/07** — trượt cái này là trượt cả demo.
- **PHẦN 6 — Ba mốc cân bằng** (31/07 · 03/08 sau C2 · 05/08 feature freeze): tụi em chốt trước
  *cắt gì khi trễ* thay vì cố ôm hết.
- **PHẦN 8 — Vì sao có bản v2:** hôm nay BTC đăng **barem Vòng 2 mới thay bảng cũ**. Tụi em rà lại
  trong ngày và **bỏ hẳn nhánh DTC/UDS**, dồn 9h đó sang phần dựng bằng chứng (baseline manifest +
  ablation). **Đây là quyết định em muốn nghe anh phản biện nhất.**

**4. Ba câu em muốn hỏi anh tối mai**
a. Vòng 2 có **phiên demo trực tiếp + Q&A** không ạ (lịch, thời lượng, hình thức)? Thể lệ có mục này
   nhưng ghi "BTC thông báo riêng", tụi em chưa nhận được.
b. **Claim–Evidence Map** và **Product & Integration Card** có template mẫu không ạ? Hai cái này là
   deliverable bắt buộc mới mà thể lệ không kèm mẫu.
c. **"Core flow chạy trên CarSky" được chấp nhận từ mức nào** — chạy trên Device trong Room đã đủ,
   hay cần kèm log/trace từ phía platform ạ?

Tối mai em xin phép hỏi kỹ 3 câu này, và rất mong nghe anh "bắt bẻ" giúp phần scope ở PHẦN 8 ạ.
Cảm ơn anh nhiều!

---

## BẢN RÚT GỌN (nếu muốn ngắn hơn nữa)

Em brief nhanh trước khi anh mở file ạ: tụi em làm **trợ lý giọng nói tiếng Việt trên AAOS**
(Challenge #3 làm trục, media/HVAC/safety là skill gắn quanh, không làm 4 đề song song).
File kia là **bảng phân công 13 ngày cuối của 4 người** — anh bỏ qua PHẦN 3 (task chi tiết) được ạ,
chỉ cần nhìn giúp tụi em **PHẦN 5** (đường găng: Script Node Luau VHAL↔CAN, 30/07), **PHẦN 6**
(mốc cắt phạm vi khi trễ) và **PHẦN 8** (hôm nay barem mới ra → tụi em bỏ hẳn nhánh DTC, dồn giờ
sang phần bằng chứng). Tối mai em xin hỏi anh 3 câu: Vòng 2 có demo live + Q&A không ạ;
Claim–Evidence Map / Product & Integration Card có template không; và "core flow chạy trên CarSky"
được tính từ mức nào. Cảm ơn anh ạ 🙏

---

## ⚠️ KIỂM TRA TRƯỚC KHI GỬI

1. **Link trong tin nhắn của Vĩ đang trỏ tới bản CŨ.** `origin/main` hiện là bản **28/07** —
   chưa có cập nhật barem 29/07: vẫn còn T10 DTC, chưa có N1–N7, PHẦN 8 chưa có mục ⑮ đã sửa.
   → Brief này nói "PHẦN 8: bỏ hẳn DTC" mà mentor mở link ra lại thấy DTC còn nguyên.
2. **`08-BAREM-VONG-2-CHINH-THUC.md` chưa có trên `main`** — file `06` trích dẫn nó 3 chỗ, mentor bấm vào sẽ 404.
3. → **Push `06` + `08` lên `main` trước, rồi mới gửi brief.**
