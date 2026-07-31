# Đường dẫn và tài khoản dùng chung

`docs/link.md` là bản có thật của file này. Nó **không** nằm trong Git (xem `.gitignore`)
và không được commit lại: repo này đi kèm bài nộp, và thể lệ 3.6 chỉ cho công khai
giải pháp của đội, không cho công khai chi tiết nội bộ nền tảng.

Chép file này thành `docs/link.md` rồi điền giá trị thật lấy từ kênh nội bộ của đội.

```
# Cổng thông tin BTC
https://fptautomotive-hackathon.com/

# CarSky
<url room của đội>
- tài khoản: <lấy trong kênh nội bộ>
- mật khẩu:  <lấy trong kênh nội bộ>
```

## Nếu bạn vừa clone và không có `docs/link.md`

Hỏi đội trưởng. Đừng dựng lại file này từ lịch sử Git — mật khẩu trong các commit
trước ngày 01/08 **đã bị coi là lộ và phải được đổi**.

## Biến môi trường

Token gọi REST API CarSky đọc từ `.env` (mẫu ở `backend/.env.example`), không đặt ở đây.
