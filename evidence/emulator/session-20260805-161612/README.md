# Phiên chạy 22 câu — bơm text, emulator AAOS

> 05/08/2026 16:16 (+07). Commit `635aab6` + đường bơm text.
> Đọc `evidence/emulator/README.md` trước: **đây là emulator, không phải CarSky.**

## Kết quả

```
22 cases · 16 PASS · 6 FAIL · 0 MISSING (6 of them known gaps)
```

Chi tiết từng câu ở `results.csv`; log gốc ở `capture.log`; định danh máy/APK ở
`run_manifest.txt`.

## Đo được gì

Đây là lần đầu bộ 22 câu chạy trên **app thật trên một máy thật** thay vì trên
fixture. Nó đo `expect_intent` và `expect_verdict` — tức đường
**router → guard → skill**.

Hai câu đáng chú ý nhất, và cả hai đều PASS:

| Câu | Tình huống | Verdict |
|---|---|---|
| B09 | *"mở cửa"* lúc xe đứng yên | `Confirm:G2_CONFIRM_DOOR` — hỏi trước khi mở |
| B10 | *"mở cửa"* lúc 60 km/h | `Deny:G1_SPEED_LOCK` — từ chối dứt khoát |

Trước hôm nay, hai ô này trong `23-N4` là *chưa đo*, vì `SafetyGuard` chưa tồn
tại và verdict chưa ra tới dòng trace.

## KHÔNG đo được gì

Câu được **bơm bằng text**, không qua mic. Đường này bỏ qua micro, VAD và ASR:

- ❌ Không có số WER, không có số về nhiễu, không có độ trễ đầu-cuối.
- ❌ Không chứng minh được Vosk nghe đúng câu nào.
- ✅ `vong2/25` §2 F7 đã ghi sẵn: bộ suite này vốn không đo những thứ đó.

Mỗi lượt có một dòng `VIVA_BENCH_INJECT` mang **cùng trace id** với lượt tương
ứng trong `capture.log` (22/22 lượt). Đừng lọc bỏ nó — đó là thứ phân biệt câu
bơm với câu nói thật.

Cả 22 summary đều mang `e2e_ms=0`: không có `speech_start`/`speech_end` thì
không có gì để đo. Harness tính `e2e_computed` từ `speech_end → tts_start` trong
các dòng event, nên các lượt này **tự nằm ngoài** mọi thống kê p50/p95. Không ai
được gỡ riêng chúng ra để "cứu" một bảng độ trễ.

## Sáu câu FAIL — không có câu nào là hồi quy

| Câu | Vì sao |
|---|---|
| B11 · B12 | `volume_adjust`: chưa có adapter CarAudioManager (D8) |
| B13 · B14 · B15 | `media_*`: chưa có adapter MediaSession (D7) |
| B20 | xem dưới |

Năm câu đầu ra `Error:exec_done` với `CommandNotWiredException` — app nhận đúng
intent rồi nói thẳng là chưa nối được, đúng như `24-N5` khai nhãn **Kế hoạch**.

## B20 — lỗi thật, cần sửa hoặc khai đúng

*"đặt bàn ăn tối"* — câu ngoài phạm vi — bị định tuyến thành
**`vehicle_status_speed`** và **thực thi** (`Allow`), thay vì bị từ chối.

Đây không phải lỗi của `SafetyGuard` như nhãn *known gap* của suite gợi ý: câu
này không bao giờ chạm tới guard, nó chết ở tầng NLU. Bộ so khớp ngữ nghĩa
(embedding) đã kéo một câu đặt bàn ăn về truy vấn tốc độ.

Hệ quả cần nói thẳng: một câu hoàn toàn ngoài miền vẫn khiến xe **làm một việc
gì đó**. Với truy vấn tốc độ thì vô hại, nhưng cơ chế gây ra nó thì không.
Đây là ví dụ tốt cho mục *AI sai ở đâu* trong write-up — và là lý do bộ
benchmark tồn tại.

## Cách chạy lại

```powershell
cd backend\scripts
.\emulator_voice_session.ps1 -Setup     # nếu máy chưa cài app
.\emulator_voice_session.ps1 -Inject    # ~2 phút, không cần nói

cd ..\
go run ./cmd/viva-tools harness verify --suite suites\benchmark_v1.csv `
  --input ..\evidence\emulator\session-<stamp>\capture.log
```

Script tự đặt tốc độ trước B09 (0) và B10 (60 km/h), và `force-stop` app một lần
đầu phiên để trạng thái giao hàng về mặc định. Thiếu hai bước đó thì kết quả phụ
thuộc vào lần chạy trước — bọn mình đã vấp đúng như vậy hai lần trước khi ra
được bảng này.
