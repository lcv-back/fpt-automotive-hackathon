# Quyết định — đường NLU chính là grammar, embedding là dự phòng

Ngày chốt: **01/08/2026** · người quyết: Long · trạng thái: **đã phản ánh đúng trong code**

## Vấn đề

Sau khi ghép `automotive/` của Dương với `android/voice/` của Long, app có **hai bộ nhận intent**:

| | Đường | Chủ | Bản chất |
|---|---|---|---|
| A | `GrammarIntentRouter` (`:voice-core`) | Long | Luật tất định, 10 intent lõi theo `03-contracts.md` §3 |
| B | keyword mapping (Room) → `SemanticIntentMatcher` (MiniLM ONNX) | Dương | Xấp xỉ, cosine similarity trên embedding |

Không chốt thì write-up và slide sẽ mô tả một kiến trúc mà code không có — đúng loại sai
mà ô *Minh bạch phạm vi demo* trừ điểm.

## Quyết định

**Grammar là đường chính và là đường có thẩm quyền. Embedding chỉ chạy khi grammar không nhận ra câu.**

Thứ tự trong [`ProcessVoiceCommandUseCase`](../automotive/feature/voice/src/main/java/com/sopa/viva_automotive/feature/voice/domain/ProcessVoiceCommandUseCase.kt):

1. `GrammarIntentRouter.route(utterance)`
2. `Matched` → `CoreIntentMapper` → thực thi. **Dừng ở đây, không hỏi embedding.**
3. `NeedsClarification` → hỏi lại đúng một câu. **Dừng.**
4. `Unsupported(canFallback = false)` → từ chối có lý do. **Dừng** — đây là chốt chặn cho
   wake-phrase sai và 5 lệnh đã cắt; embedding *không* được phép cứu chúng.
5. Chỉ khi `Unsupported(canFallback = true)` mới xuống keyword → embedding.

## Vì sao grammar thắng

- **Kiểm chứng được.** Một luật tất định thì test được, log được, và trả lời được câu
  *"vì sao máy làm thế"* trước BGK. Ngưỡng cosine không trả lời được câu đó.
- **Ranh giới team-owned rõ.** Grammar + bảng M2 là phần đội tự viết; MiniLM là model tải về.
  Ô *Tách phần team-owned* (5đ) chấm đúng ranh giới này.
- **Từ chối phải chắc chắn.** Bước 4 là lý do quan trọng nhất: nếu embedding được chạy sau một
  `Unsupported` dứt khoát, thì "Siri ơi hạ điều hòa" hoặc "bật điều hòa" (đã cắt) vẫn có thể
  bị suy ra thành một lệnh xe. Một trợ lý đoán bừa khi đã quyết định từ chối là lỗi an toàn,
  không phải tính năng.

## Embedding giữ lại để làm gì

Không bỏ. Nó là đường xử lý **cách nói khác** mà grammar chưa phủ (M7-01 nhóm "nóng quá / lạnh quá"
đã có luật riêng, nhưng các biến thể khác thì chưa). Trong write-up nó được khai đúng vai:
*fallback cho paraphrase*, không phải *bộ hiểu ngôn ngữ chính*.

## Hệ quả phải nhớ khi viết bài

- README và slide vẽ pipeline theo đúng 5 bước trên, **không** vẽ hai nhánh song song ngang hàng.
- Số benchmark của Vĩ (V10/V12) phải tách hai nhóm: câu grammar phủ và câu rơi xuống embedding.
  Gộp chung sẽ làm p95 khó giải thích vì hai đường có chi phí rất khác nhau.
- Ablation: bỏ grammar → mọi lệnh phụ thuộc ngưỡng cosine, và các câu từ chối ở bước 4 hết chốt chặn.
  Đây là một mục ablation rẻ, chạy được ngay, không cần Device.

Liên quan: `03-contracts.md` §3 · `13-M7A-TINH-HUONG-PHUC-TAP.md` · `15-QUYET-DINH-BENCHMARK-ASR.md`
