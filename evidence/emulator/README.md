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
