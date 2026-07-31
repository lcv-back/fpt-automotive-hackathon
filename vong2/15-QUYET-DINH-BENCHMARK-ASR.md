# L10 — Quyết định thay trục “edge-only vs hybrid”

## Quyết định

Không dựng lại một cloud-LLM path chỉ để giữ nguyên câu chữ trong proposal. Trục so sánh Vòng 2 là:

> **ASR on-device (Vosk) so với ASR container (`viva-asr`)**, cùng tập utterance, cùng mức nhiễu và cùng
> định nghĩa latency `speech_end → asr_done` / `speech_end → tts_start`.

Lý do: đây là hai đường có thật trong kiến trúc hiện tại. So sánh này trả lời trực tiếp lựa chọn deployment
trong xe: on-device giữ privacy/offline và tránh network hop; container có thể tăng chất lượng mô hình nhưng
thêm phụ thuộc mạng/nút triển khai. Cloud LLM T2 đã bị cắt vì không phù hợp budget 1,5 giây và yêu cầu mất mạng.

## Ma trận đo bắt buộc

| Biến giữ cố định | Giá trị |
|---|---|
| Dataset | Cùng 20 utterance × 3 mức nhiễu; công bố synthetic/real |
| Thiết bị đầu vào | Cùng file PCM 16 kHz mono hoặc cùng lượt phát lại |
| Output | WER/intent accuracy, success rate, p50/p95 ASR và end-to-end |
| Warm-up | Tách cold run khỏi steady-state; không trộn để làm đẹp p95 |
| Failure | Timeout/lỗi vẫn nằm trong mẫu với `Error:<stage>`, không bị loại |
| Identity | Ghi model/version/config/commit cho cả hai đường |

## Đoạn dùng nguyên văn trong write-up

“Proposal ban đầu nêu so sánh edge-only và hybrid. Sau spike, đội loại cloud LLM khỏi core flow vì nó tạo
network dependency trái với mục tiêu offline và làm tăng rủi ro vượt ngân sách phản hồi 1,5 giây. Chúng tôi
không che giấu thay đổi này và không dựng một đường giả chỉ để giữ claim cũ. Thay vào đó, ablation so sánh
hai deployment ASR đang tồn tại—Vosk on-device và `viva-asr` container—trên cùng dữ liệu, cùng mức nhiễu và
cùng cách tính p50/p95. Kết quả sẽ quyết định đường mặc định; đường còn lại được giữ làm fallback có điều kiện.”

## Trạng thái

- **Quyết định + wording:** hoàn tất.
- **Số benchmark:** chờ Vĩ chạy harness; chưa được điền trước hoặc suy diễn.
