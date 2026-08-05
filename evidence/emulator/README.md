# `evidence/emulator/` — chạy trên emulator AAOS, KHÔNG phải CarSky

> Lập 05/08/2026. Thư mục này **cố ý tách khỏi `evidence/c2/`**.

## Đọc một dòng

Toàn bộ file ở đây được thu trên **Android Automotive emulator chạy trên máy dev**, không
phải trên Device `VIVA` của CarSky. Chúng **không** đóng được E01–E11 trong
`vong2/18-CLAIM-EVIDENCE-MAP.md` — các ID đó đòi đúng nền tảng CarSky.

Vì sao vẫn thu: tính tới 05/08, APK **chưa từng được cài hay mở trên bất kỳ máy nào**;
mọi số liệu đều từ unit test và fixture tự sinh. Đường tới Device CarSky đang đứt
(`docs/backend-docs/carsky-api.md` §4 — Conduit trả 502 cho `adb-exec`/`shell`, và chưa ai
chạy được `nydus-reach`). Emulator là cách duy nhất hiện có để biết app **có chạy hay
không** trước freeze.

## Môi trường

| Mục | Giá trị |
|---|---|
| Emulator | `emulator -avd viva_aaos34 -no-snapshot -no-boot-anim -gpu auto -allow-host-audio` |
| System image | `system-images;android-34-ext9;android-automotive;x86_64` (Google APIs) |
| Fingerprint | `google/sdk_gcar_x86_64/emulator_car64_x86_64:14/UAA1.250512.001/13479943:userdebug/dev-keys` |
| Serial | `EMULATOR37X1X11X0` |
| Biến thể APK | **`mock`** — `MockVehicleRepository`, không đụng `android.car`, không có CarService/VHAL thật |
| Commit | `635aab695360e96f55398fcca0436ace401ec043` |

## Dựng lại môi trường này — ba bước, bước 2 hay bị quên

```powershell
# 1. Cai SDK va tao AVD (mot lan)
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\cmdline-tools\latest\bin\sdkmanager.bat" "emulator" "system-images;android-34-ext9;android-automotive;x86_64"
& "$sdk\cmdline-tools\latest\bin\avdmanager.bat" create avd -n viva_aaos34 `
    -k "system-images;android-34-ext9;android-automotive;x86_64" -d "automotive_1080p_landscape"

# 2. Khoi dong KEM CO AUDIO
& "$sdk\emulator\emulator.exe" -avd viva_aaos34 -no-snapshot -no-boot-anim -gpu auto -allow-host-audio

# 3. Cai app, cap quyen, chon tieng Viet (giong noi + giao dien)
cd backend\scripts
.\emulator_voice_session.ps1 -Setup
```

> 🔴 **Bước còn thiếu mà không lệnh nào làm thay được.** Trong cửa sổ emulator:
> nút **`...`** (Extended Controls) → mục **Microphone** → bật
> **"Virtual microphone uses host audio input"**. Toggle này **mặc định tắt**,
> và cờ `-allow-host-audio` chỉ *cho phép* chứ không bật nó.
>
> Thiếu bước này, log ghi `peak=2/32767` suốt cả lượt — mic ảo đẩy im lặng vào
> máy ảo, mọi lượt ra `Error:speech_end`, và không có gì trong app hay trong
> log gợi ý nguyên nhân. Ngày 05/08 mất vài giờ mới lần ra.

## Mức tín hiệu cần đạt

Đo bằng `adb logcat -s VoskEngine:D` trong lúc nói:

| `peak` | Nghĩa là |
|---|---|
| `2` | mic ảo chưa nối — xem toggle ở trên |
| `~1700` | có tiếng nhưng quá nhỏ, Vosk không đọc ra chữ nào |
| `> 5000` | đủ để nhận dạng |
| `20000+` | mức đo được khi nói to và ghé gần máy |

Mic array của laptop thu rất yếu ở khoảng cách thường. Kéo **Input volume** trong
Windows Sound lên 100 và nói gần.

## Được phép khai gì

✅ *"App build, cài, khởi động và chạy ổn định trên emulator AAOS 14; sinh `VIVA_TRACE`
đúng format; model Vosk tiếng Việt nạp thật; `SafetyGuard` chặn lệnh mở khoá cửa ở
60 km/h trước khi chạm setter."*

❌ Không được khai bất kỳ câu nào có chữ *"trên CarSky"*, *"trên Device"*, *"tới VHAL"*,
*"tới CAN"* hay *"full-stack"* dựa trên các file ở đây. Biến thể `mock` nghĩa là đầu kia
là bộ mô phỏng trong app — theo `24-N5` đây vẫn là nhãn **Mô phỏng**, chỉ là mô phỏng
*đã thật sự chạy* thay vì mới chỉ có unit test.

## Danh sách file

| File | Nội dung |
|---|---|
| `artifact-identity.txt` | commit, tên APK, SHA-256, flavor, giờ build/cài, định danh máy |
| `install-launch-crash.log` | kết quả cài, khởi động, quét crash/ANR |
| `viva-trace-first-turn.log` | một lượt voice trọn vẹn, có `VIVA_TRACE_SUMMARY` |
| `safety-speed60.log` | mở khoá cửa ở 60 km/h bị `G1_SPEED_LOCK` chặn |
| `safety-speed60-hmi.png` | ảnh màn hình lúc bị chặn |
| `vosk-vi-loaded.log` | model tiếng Việt nạp và lắng nghe |
