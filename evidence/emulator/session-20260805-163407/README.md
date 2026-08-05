# Phiên 22 câu — sau khi sửa lỗi định tuyến B20

> 05/08/2026 16:34 (+07). Chạy lại sau khi sửa tầng embedding.
> Đọc `evidence/emulator/README.md` trước: **đây là emulator, không phải CarSky.**
> Phiên trước (16:16) giữ lại để đối chiếu.

## Kết quả

```
22 cases · 16 PASS · 6 FAIL · 0 MISSING (6 of them known gaps)
```

Số PASS không đổi so với phiên 16:16, nhưng **B20 đã khác về chất**:

| | Phiên 16:16 | Phiên này |
|---|---|---|
| intent | `vehicle_status_speed` ❌ | `unknown` ✅ |
| verdict | `Allow` ❌ | `Error:nlu_done` ⚠️ |

Trước: một câu đặt bàn ăn tối khiến xe **thực thi** một truy vấn tốc độ.
Sau: nó được nhận ra là không hiểu.

Verdict vẫn lệch kỳ vọng `Deny:G3_UNSUPPORTED` của suite, và điều đó **đúng như
hiện trạng**: `SafetyRules` không có mã `G3_UNSUPPORTED`, và một câu rơi vào
`unknown` thì không bao giờ chạm tới guard để mà bị Deny. Đây là khoảng cách
thiết kế còn mở, không phải lỗi mới — `benchmark_v1.csv` đã ghi sẵn *"hôm nay rơi
vào unknown/Error:nlu_done, đích là Deny có lý do"*.

## Lỗi đã sửa — và vì sao nó đáng sợ hơn một câu sai

Log của matcher lúc chạy phiên trước:

```
D EmbedIntent: Semantic match "đặt bàn ăn tối" → QUERY_SPEED (cos=1.0)
```

**Cosine bằng 1.0** nghĩa là hai vector trùng khít, không phải "gần giống".

Nguyên nhân: vocab của MiniLM là WordPiece **tiếng Anh**. `BertWordPieceTokenizer`
trả `[UNK]` cho từ nào không tách được, nên:

```
"đặt bàn ăn tối"   -> [CLS] [UNK] [UNK] [UNK] [UNK] [SEP]
"tốc độ hiện tại"  -> [CLS] [UNK] [UNK] [UNK] [UNK] [SEP]
```

Cùng chuỗi token → cùng vector → cosine 1.0. Nói cách khác: **tầng embedding
không hề khớp ngữ nghĩa cho tiếng Việt — nó khớp theo số từ.** Ngưỡng
`MIN_COSINE = 0.48` không cứu được, vì 1.0 vượt mọi ngưỡng.

Đo thêm để chắc chắn không phải một câu cá biệt:

```
"hôm nay trời đẹp quá" → QUERY_SPEED (cos=0.5969521)
```

Cũng vượt ngưỡng, cũng là câu ngoài miền.

Cách sửa: encoder **từ chối nhúng** khi mọi token nội dung đều `[UNK]` — vector
đó không mang thông tin từ vựng nào để so khớp. Tầng trên nhận `null` và trả
`unknown`, thay vì nhận một vector trông có vẻ tự tin.

Hệ quả phải khai đúng: 18 exemplar tiếng Việt trong `IntentExemplarCatalog` (trên
tổng 67) giờ bị loại khỏi chỉ mục — chúng vốn vô dụng và có hại theo đúng cơ chế
trên. **Tầng embedding hiện chỉ còn phủ tiếng Anh.** Tiếng Việt do tầng grammar
lo, và 16 câu PASS ở đây đều đi qua grammar.

Cách sửa đúng cho Vòng 3 là đổi sang model đa ngữ (LaBSE hoặc
`paraphrase-multilingual-MiniLM`) rồi đo lại — không kịp và không nên làm trong
ngày freeze.

`BertWordPieceTokenizerTest` khoá cơ chế này lại, gồm một test khẳng định hai câu
tiếng Việt khác nhau cùng độ dài cho ra **cùng** `inputIds`. Nếu ai đó đổi sang
model đa ngữ, test đó sẽ đổ — và đó là tin tốt.

## Giới hạn không đổi

Vẫn là **bơm text**, không qua mic: không đo WER, không đo nhiễu, không đo độ trễ
đầu-cuối. 22/22 lượt đều có dòng `VIVA_BENCH_INJECT` cùng trace id trong
`capture.log`, và cả 22 summary đều `e2e_ms=0`.
