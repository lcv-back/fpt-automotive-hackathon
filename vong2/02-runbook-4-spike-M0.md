# RUNBOOK M0 — 4 SPIKE NGÀY 26/07

> **Mục tiêu duy nhất của hôm nay: trả lời được 4 câu hỏi kiến trúc.** Không viết code sản phẩm.
> Mỗi người báo kết quả vào nhóm **trước 21:00**, theo đúng mẫu "KẾT QUẢ" ở cuối mỗi spike.
> Ai xong sớm thì sang giúp người đang bị chặn — không ai tự xoay quá 1 tiếng.

---

## BƯỚC CHUNG (cả 4 người làm trước, ~20 phút)

### 1. Lấy API key CarSky

1. Login `https://carsky.io` (Keycloak SSO)
2. Bấm ⚙️ góc trên phải → **Settings** → **Credentials**
3. **New credential** → đặt tên (`viva-long`, `viva-vi`, …) → **Create**
4. **Copy key ngay** — chuỗi chỉ hiện **một lần duy nhất**

Lưu vào file `.env` ở thư mục làm việc (file này **không bao giờ commit**):

```bash
CARSKY_URL=https://carsky.io
CARSKY_API_KEY=<key vừa copy>
ROOM_ID=<id device của đội>
NODE_KEY=<node key của IVI-FACE>
```

### 2. Lấy `ROOM_ID` và `NODE_KEY`

```bash
# ROOM_ID = ID của Device đội đang Connect (xem cạnh tên device trong panel Devices)
curl -s -H "X-API-Key: $CARSKY_API_KEY" "$CARSKY_URL/api/v1/devices" | jq '.items[] | {id, name}'

# NODE_KEY = id của node Skycraft AAOS trong blueprint
curl -s -H "X-API-Key: $CARSKY_API_KEY" "$CARSKY_URL/api/v1/deployments/$ROOM_ID/nodes" | jq
```

### 3. Mở ADB tunnel

```bash
curl -s -H "X-API-Key: $CARSKY_API_KEY" "$CARSKY_URL/api/v1/deployments/$ROOM_ID/adb-tunnel"
# → trả về hướng dẫn mở tunnel, ví dụ localhost:5038

adb connect localhost:5038
adb devices          # phải thấy device ở trạng thái "device", không phải "offline"
adb shell getprop ro.product.name    # kỳ vọng thấy trout_arm64 hoặc tương tự
```

> **Tunnel là phiên tạm.** Mất kết nối thì mở lại tunnel + `adb connect` lại — **không cần** deploy lại Room.

---

## 🎤 SPIKE S1 — MICROPHONE (Long)

> **Câu hỏi cần trả lời:** app Android chạy trong Skycraft VM có nhận được audio từ microphone của laptop khi bật "Enable microphone" trên widget Screen không?
>
> **Đây là spike quan trọng nhất hôm nay.** Kết quả quyết định toàn bộ kiến trúc voice.

### Bước 1 — Kiểm tra VM có nhìn thấy thiết bị thu âm không (2 phút, làm trước)

```bash
adb shell dumpsys media.audio_flinger | grep -A5 -i "input"
adb shell cmd media_session list-sessions
# Nhanh hơn nữa:
adb shell dumpsys audio | grep -i -E "mic|input|source"
```

Nếu **không có** input device nào → gần như chắc chắn phải đi PA-B. Vẫn làm tiếp bước 2 để chắc chắn.

### Bước 2 — APK ghi âm tối giản

Tạo project Android mới (Empty Activity, minSdk 33, Kotlin). Chỉ cần 1 file.

`AndroidManifest.xml` — thêm quyền:
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
```

`MainActivity.kt`:
```kotlin
package com.viva.micspike

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.io.File
import kotlin.concurrent.thread
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private val TAG = "VIVA_MIC"
    private val SR = 16000
    private val DURATION_S = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            return
        }
        record()
    }

    override fun onRequestPermissionsResult(rc: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(rc, p, r)
        if (r.isNotEmpty() && r[0] == PackageManager.PERMISSION_GRANTED) record()
        else Log.e(TAG, "RECORD_AUDIO bi tu choi")
    }

    private fun record() = thread {
        // Liet ke thiet bi thu am ma VM nhin thay
        val am = getSystemService(AUDIO_SERVICE) as AudioManager
        val inputs = am.getDevices(AudioManager.GET_DEVICES_INPUTS)
        Log.i(TAG, "So input device: ${inputs.size}")
        inputs.forEach { Log.i(TAG, "  input: type=${it.type} name=${it.productName}") }
        if (inputs.isEmpty()) Log.e(TAG, "KHONG CO INPUT DEVICE NAO -> di phuong an B")

        val minBuf = AudioRecord.getMinBufferSize(
            SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        Log.i(TAG, "minBufferSize=$minBuf")

        val rec = AudioRecord(
            MediaRecorder.AudioSource.MIC, SR,
            AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 4)

        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord KHOI TAO THAT BAI -> di phuong an B"); return@thread
        }

        val out = File(getExternalFilesDir(null), "mic_test.pcm")
        rec.startRecording()
        Log.i(TAG, ">>> BAT DAU GHI $DURATION_S giay - NOI VAO MIC NGAY BAY GIO <<<")

        val buf = ShortArray(minBuf)
        var totalSamples = 0
        var peak = 0
        var sumAbs = 0L
        out.outputStream().use { os ->
            val deadline = System.currentTimeMillis() + DURATION_S * 1000
            while (System.currentTimeMillis() < deadline) {
                val n = rec.read(buf, 0, buf.size)
                if (n <= 0) continue
                totalSamples += n
                for (i in 0 until n) {
                    val v = abs(buf[i].toInt())
                    if (v > peak) peak = v
                    sumAbs += v
                    os.write(buf[i].toInt() and 0xFF)
                    os.write((buf[i].toInt() shr 8) and 0xFF)
                }
            }
        }
        rec.stop(); rec.release()

        val avg = if (totalSamples > 0) sumAbs / totalSamples else 0
        Log.i(TAG, "=== KET QUA ===")
        Log.i(TAG, "samples=$totalSamples  peak=$peak  avgAbs=$avg")
        Log.i(TAG, "file=${out.absolutePath}  size=${out.length()} bytes")
        when {
            totalSamples == 0            -> Log.e(TAG, "KET LUAN: KHONG DOC DUOC GI -> PHUONG AN B")
            peak < 200                   -> Log.e(TAG, "KET LUAN: CHI CO IM LANG (peak=$peak) -> PHUONG AN B")
            else                         -> Log.i(TAG, "KET LUAN: MIC HOAT DONG (peak=$peak) -> PHUONG AN A")
        }
    }
}
```

### Bước 3 — Chạy và đọc kết quả

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Terminal riêng: mở log
adb logcat -c && adb logcat -s VIVA_MIC
```

Trên trình duyệt:
1. Mở widget **Screen** của Device
2. Inspector (cột phải) → mục **Microphone** → chọn micro của laptop → bấm **Enable microphone**
3. Mở app `micspike` trên màn hình AAOS → **NÓI TO VÀO MIC 5 GIÂY** ("một hai ba bốn năm, xin chào Vivi")

```bash
# Kéo file về nghe thử
adb pull /sdcard/Android/data/com.viva.micspike/files/mic_test.pcm .
ffplay -f s16le -ar 16000 -ac 1 mic_test.pcm     # hoặc mở bằng Audacity: raw PCM 16-bit LE, 16kHz mono
```

### 📋 KẾT QUẢ S1 — báo vào nhóm theo mẫu này

```
S1 MICROPHONE
- Số input device VM nhìn thấy: ___
- AudioRecord khởi tạo: THÀNH CÔNG / THẤT BẠI
- samples=___  peak=___  avgAbs=___
- Nghe file .pcm có ra tiếng nói không: CÓ / KHÔNG / CHỈ NHIỄU
=> KẾT LUẬN: PHƯƠNG ÁN A (mic vào được VM) / PHƯƠNG ÁN B (bắt audio ở host)
```

### Nếu ra PHƯƠNG ÁN B — thử luôn đường thay thế (30 phút)

```bash
# Kiểm tra app nhận được text đẩy từ ngoài vào không
adb shell am broadcast -a com.viva.INTENT_TEXT --es text "ha dieu hoa xuong 22 do"

# Hoặc qua REST của CarSky (gõ text vào ô đang focus)
curl -X POST -H "X-API-Key: $CARSKY_API_KEY" -H "Content-Type: application/json" \
  "$CARSKY_URL/api/v1/vms/$ROOM_ID/$NODE_KEY/text" \
  -d '{"text":"ha dieu hoa xuong 22 do"}'
```

---

## ⚙️ SPIKE S2 — VHAL PROPERTY (Tùng)

> **Câu hỏi cần trả lời:** property nào đọc/ghi được, property nào trả `null`?
> Kết quả là **bảng property** — đây là đầu vào bắt buộc cho Climate skill, Safety Guard, và mục "danh sách Vehicle Property đã dùng" trong README.

### Bước 1 — Cách nhanh nhất: `adb shell` trước, code sau (10 phút)

```bash
# Xem service xe có sống không
adb shell dumpsys car_service | head -50

# Liệt kê property mà VHAL đang expose
adb shell dumpsys car_service --services CarPropertyService | head -100

# Xem tất cả property được hỗ trợ
adb shell cmd car_service list-properties 2>/dev/null | head -60
```

Đối chiếu song song với widget **Signal Watch** trên trình duyệt.

### Bước 2 — APK dò property

`AndroidManifest.xml`:
```xml
<uses-permission android:name="android.car.permission.CAR_CONTROL_AUDIO_VOLUME"/>
<uses-permission android:name="android.car.permission.CONTROL_CAR_CLIMATE"/>
<uses-permission android:name="android.car.permission.CONTROL_CAR_DOORS"/>
<uses-permission android:name="android.car.permission.CAR_SPEED"/>
<uses-permission android:name="android.car.permission.CAR_POWERTRAIN"/>
<uses-library android:name="android.car" android:required="true"/>
```

`PropProbe.kt`:
```kotlin
package com.viva.vhalspike

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val TAG = "VIVA_VHAL"

    // areaId = 0 nghia la GLOBAL. HVAC thuong dung area theo ghe.
    private val PROPS = listOf(
        Triple("HVAC_POWER_ON",        VehiclePropertyIds.HVAC_POWER_ON,        0x01),
        Triple("HVAC_TEMPERATURE_SET", VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x01),
        Triple("HVAC_FAN_SPEED",       VehiclePropertyIds.HVAC_FAN_SPEED,       0x01),
        Triple("HVAC_AC_ON",           VehiclePropertyIds.HVAC_AC_ON,           0x01),
        Triple("HVAC_FAN_DIRECTION",   VehiclePropertyIds.HVAC_FAN_DIRECTION,   0x01),
        Triple("DOOR_LOCK",            VehiclePropertyIds.DOOR_LOCK,            0x01),
        Triple("PERF_VEHICLE_SPEED",   VehiclePropertyIds.PERF_VEHICLE_SPEED,   0),
        Triple("GEAR_SELECTION",       VehiclePropertyIds.GEAR_SELECTION,       0),
        Triple("NIGHT_MODE",           VehiclePropertyIds.NIGHT_MODE,           0),
        Triple("PARKING_BRAKE_ON",     VehiclePropertyIds.PARKING_BRAKE_ON,     0),
        Triple("FUEL_LEVEL",           VehiclePropertyIds.FUEL_LEVEL,           0),
        Triple("IGNITION_STATE",       VehiclePropertyIds.IGNITION_STATE,       0),
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val car = Car.createCar(this)
        val cpm = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager

        Log.i(TAG, "=== DANH SACH PROPERTY VHAL EXPOSE ===")
        cpm.propertyList.forEach {
            Log.i(TAG, "  id=0x%08X access=%d areas=%s"
                .format(it.propertyId, it.access, it.areaIds.joinToString()))
        }

        Log.i(TAG, "=== DO TUNG PROPERTY ===")
        for ((name, id, area) in PROPS) {
            // READ
            val readRes = try {
                val v = cpm.getProperty(Any::class.java, id, area)
                if (v == null) "NULL" else "OK value=${v.value}"
            } catch (e: Exception) { "LOI ${e.javaClass.simpleName}: ${e.message}" }

            // SUBSCRIBE (callback realtime co hoat dong khong)
            val subRes = try {
                cpm.registerCallback(object : CarPropertyManager.CarPropertyEventCallback {
                    override fun onChangeEvent(v: android.car.hardware.CarPropertyValue<*>) {
                        Log.i(TAG, "CALLBACK $name -> ${v.value}")
                    }
                    override fun onErrorEvent(propId: Int, zone: Int) {
                        Log.w(TAG, "CALLBACK ERROR $name")
                    }
                }, id, CarPropertyManager.SENSOR_RATE_NORMAL)
                "SUBSCRIBED"
            } catch (e: Exception) { "SUB_LOI ${e.message}" }

            Log.i(TAG, "%-24s | READ: %-40s | %s".format(name, readRes, subRes))
        }

        // WRITE thu nghiem: chi thu 2 property an toan
        Log.i(TAG, "=== THU GHI ===")
        tryWrite(cpm, "HVAC_TEMPERATURE_SET", VehiclePropertyIds.HVAC_TEMPERATURE_SET, 0x01, 22.0f)
        tryWrite(cpm, "HVAC_FAN_SPEED",       VehiclePropertyIds.HVAC_FAN_SPEED,       0x01, 2)
    }

    private fun tryWrite(cpm: CarPropertyManager, name: String, id: Int, area: Int, value: Any) {
        try {
            when (value) {
                is Float -> cpm.setFloatProperty(id, area, value)
                is Int   -> cpm.setIntProperty(id, area, value)
                is Boolean -> cpm.setBooleanProperty(id, area, value)
            }
            Thread.sleep(300)
            val back = cpm.getProperty(Any::class.java, id, area)?.value
            Log.i(TAG, "WRITE $name = $value -> doc lai = $back  ${if (back.toString() == value.toString()) "KHOP" else "KHONG KHOP"}")
        } catch (e: Exception) {
            Log.e(TAG, "WRITE $name THAT BAI: ${e.javaClass.simpleName} ${e.message}")
        }
    }
}
```

```bash
adb install -r app-debug.apk
adb logcat -c && adb logcat -s VIVA_VHAL
```

### Bước 3 — Đối chiếu chiều ngược lại

Trên trình duyệt: mở widget **GPIO Panel** hoặc **Signal Watch** → thay đổi giá trị `HvacCommand/Driver_Temperature` → xem log app có nhận `CALLBACK` không. **Đây là bằng chứng "real-time sync" cho đề #2.**

### 📋 KẾT QUẢ S2 — báo vào nhóm

```
S2 VHAL
| Property                | READ        | WRITE   | CALLBACK |
|-------------------------|-------------|---------|----------|
| HVAC_POWER_ON           | OK / NULL   | OK/lỗi  | có/không |
| HVAC_TEMPERATURE_SET    |             |         |          |
| HVAC_FAN_SPEED          |             |         |          |
| HVAC_AC_ON              |             |         |          |
| DOOR_LOCK               |             |         |          |
| PERF_VEHICLE_SPEED      |             |         |          |
| GEAR_SELECTION          |             |         |          |
| NIGHT_MODE              |             |         |          |
| PARKING_BRAKE_ON        |             |         |          |
- Đổi giá trị từ Signal Watch → app có nhận callback: CÓ / KHÔNG
=> DANH SÁCH PROPERTY CẦN XIN TEAM HẠ TẦNG WIRE THÊM: ___
```

⚠️ Property trả `null` → **báo team hạ tầng, KHÔNG debug tiếp phía app** (guideline CDC nói rõ).

---

## 🎵 SPIKE S3 — USB MEDIA (Dương)

> **Câu hỏi cần trả lời:** đóng gói được ảnh USB và app đọc được nhạc + album art từ đó không?

### Bước 1 — Build `usb.img` trên Windows (dùng WSL2 — sạch nhất)

```bash
# Trong WSL2
sudo apt update && sudo apt install -y dosfstools mtools

mkdir -p ~/viva-usb && cd ~/viva-usb
# Chuẩn bị 5 file mp3 CÓ SẴN ID3 tag (title/artist/album) và ảnh bìa nhúng
# Kiểm tra tag trước:
#   sudo apt install -y python3-mutagen && python3 -c "from mutagen.mp3 import MP3; print(MP3('a.mp3').tags)"

truncate -s 256M usb.img
mkfs.vfat -F 32 -n VIVAUSB usb.img

mmd -i usb.img ::/Music
mcopy -s -i usb.img ./mp3/* ::/Music/
mdir -i usb.img ::/Music        # xác nhận file đã vào
```

### Bước 1b — Nếu không có WSL2 (PowerShell Administrator)

```powershell
$vhd = "C:\Temp\usb.vhd"
New-Item -ItemType Directory -Force C:\Temp | Out-Null

@"
create vdisk file="C:\Temp\usb.vhd" maximum=256 type=fixed
select vdisk file="C:\Temp\usb.vhd"
attach vdisk
create partition primary
format fs=fat32 quick label=VIVAUSB
assign letter=Z
exit
"@ | Out-File -Encoding ascii C:\Temp\mk.txt
diskpart /s C:\Temp\mk.txt

New-Item -ItemType Directory -Force Z:\Music | Out-Null
Copy-Item C:\Temp\mp3\* Z:\Music\

@"
select vdisk file="C:\Temp\usb.vhd"
detach vdisk
exit
"@ | Out-File -Encoding ascii C:\Temp\um.txt
diskpart /s C:\Temp\um.txt

Rename-Item $vhd C:\Temp\usb.img
```

### Bước 2 — Upload lên Artifacts

1. Panel **Artifacts** → **New Artifact**
2. Category = **USB**, tên = `viva-usb`
3. **Add Version** → chọn `usb.img` → đánh dấu **latest**

### Bước 3 — Mount vào VM

1. Panel **Devices** → Connect device → **+** → thêm widget **USB Device Proxy**
2. Inspector → chọn node Device Proxy (thường tên "USB Devices")
3. Stage → **Refresh** → dropdown **Image** chọn `viva-usb — usb.img (v0.0.1)` → bấm **Plug**
4. Xác nhận thấy **"Attached (1)"** kèm đường dẫn mount

```bash
# Xác nhận phía VM
adb shell ls -la /sdcard/Music/
adb shell find /sdcard -iname "*.mp3" 2>/dev/null | head
adb shell "content query --uri content://media/external/audio/media --projection title:artist:album" | head
```

### Bước 4 — Kích hoạt MediaStore scan nếu file không hiện

```bash
adb shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE \
  -d file:///sdcard/Music/usb_1
adb shell cmd media_session list-sessions
```

### 📋 KẾT QUẢ S3 — báo vào nhóm

```
S3 USB MEDIA
- Build usb.img: THÀNH CÔNG / LỖI (___)
- Upload Artifact category USB: OK / lỗi
- Plug qua widget: OK, đường dẫn mount = ___
- adb thấy file mp3: CÓ / KHÔNG
- MediaStore index được (content query ra kết quả): CÓ / KHÔNG
- Album art đọc được: CÓ / KHÔNG
=> Media skill dùng nguồn: MediaStore / đọc file trực tiếp
```

---

## 🤖 SPIKE S4 — PLATFORM API & MCP (Vĩ)

> **Câu hỏi cần trả lời:** điều khiển được AAOS từ script/AI agent không? Push image lên registry được không? VM có internet không?

### Bước 1 — Smoke test REST API

```bash
set -a && source .env && set +a

# 1. Health
curl -s -H "X-API-Key: $CARSKY_API_KEY" "$CARSKY_URL/api/v1/healthz" | jq

# 2. Trạng thái deployment
curl -s -H "X-API-Key: $CARSKY_API_KEY" \
  "$CARSKY_URL/api/v1/deployments/$ROOM_ID/status" | jq

# 3. Chụp màn hình AAOS
curl -s -H "X-API-Key: $CARSKY_API_KEY" \
  "$CARSKY_URL/api/v1/vms/$ROOM_ID/$NODE_KEY/screenshot" -o shot.png && file shot.png

# 4. Cây UI (dùng cho regression test sau này)
curl -s -H "X-API-Key: $CARSKY_API_KEY" \
  "$CARSKY_URL/api/v1/vms/$ROOM_ID/$NODE_KEY/accessibility" | jq '.elements | length'

# 5. Chạy shell trong Android
curl -s -X POST -H "X-API-Key: $CARSKY_API_KEY" -H "Content-Type: application/json" \
  "$CARSKY_URL/api/v1/vms/$ROOM_ID/$NODE_KEY/shell" \
  -d '{"command":"getprop ro.build.version.release"}' | jq

# 6. Tap thử (Touch route CHỈ có khi hệ thống bật COOLGATE_URL_SERVER)
curl -s -X POST -H "X-API-Key: $CARSKY_API_KEY" -H "Content-Type: application/json" \
  "$CARSKY_URL/api/v1/vms/$ROOM_ID/$NODE_KEY/tap" -d '{"x":450,"y":300}' | jq
```

### Bước 2 — Tín hiệu (quan trọng cho Safety Guard)

```bash
# Liệt kê node nguồn tín hiệu
curl -s -H "X-API-Key: $CARSKY_API_KEY" "$CARSKY_URL/api/v1/signals/$ROOM_ID" | jq

SIG_NODE=<node key vừa tìm được>

# Liệt kê tín hiệu trên node đó
curl -s -H "X-API-Key: $CARSKY_API_KEY" \
  "$CARSKY_URL/api/v1/signals/$ROOM_ID/$SIG_NODE" | jq

# ĐỌC tốc độ xe
curl -s -X POST -H "X-API-Key: $CARSKY_API_KEY" -H "Content-Type: application/json" \
  "$CARSKY_URL/api/v1/signals/$ROOM_ID/$SIG_NODE/values" \
  -d '{"paths":["Vehicle.Speed"]}' | jq

# GHI tốc độ xe = 60 (chính là cách demo Safety Guard!)
curl -s -X POST -H "X-API-Key: $CARSKY_API_KEY" -H "Content-Type: application/json" \
  "$CARSKY_URL/api/v1/signals/$ROOM_ID/$SIG_NODE/actuate" \
  -d '{"signals":[{"path":"Vehicle.Speed","value":60}]}' | jq
```

> Nếu path `Vehicle.Speed` không có, thử các path đã thấy trong Started pack:
> `HvacCommand/Driver_Temperature`, `HvacCommand/Passenger_Temperature`, `PWT_VehicleSpeed/Speed_kph`

### Bước 3 — Registry

```bash
# Thử cả 2 host, xem host nào đúng
docker login registry.carsky.io -u <username-carsky>
# hoặc
docker login registry.hackathon-1.carsky.io -u <username-carsky>
# Password = Zot API key (tạo tại registry UI → icon user → API Keys → Create new API key, dạng zak_...)

# Push thử image rỗng để xác nhận cluster kéo được
docker pull hello-world
docker tag hello-world <host>/viva/hello:v1
docker push <host>/viva/hello:v1
```

### Bước 4 — VM có internet không

```bash
adb shell ping -c 3 8.8.8.8
adb shell "curl -s -o /dev/null -w '%{http_code}' https://api.anthropic.com" 2>/dev/null
adb shell settings get global captive_portal_server
```

### Bước 5 — Nối MCP vào Claude Code

```json
{
  "mcpServers": {
    "carsky": {
      "command": "node",
      "args": ["<đường dẫn>/mcp/dist/index.js"],
      "env": {
        "A8_URL": "https://carsky.io",
        "A8_API_KEY": "<api key>"
      }
    }
  }
}
```
> Chưa có file MCP server thì hỏi mentor xin — hoặc dùng HTTP transport: đặt `MCP_TRANSPORT=http`, server chạy cổng `3100`, endpoint `http://localhost:3100/mcp`.

Test: yêu cầu Claude *"chụp màn hình AAOS và cho tôi biết đang mở app gì"*.

### Bước 6 — Repo Git

```bash
mkdir viva && cd viva && git init
mkdir -p app docs tools container/asr container/svc
cat > .gitignore <<'EOF'
.env
*.keystore
local.properties
build/
.gradle/
*.apk
*.pcm
*.img
__pycache__/
EOF
cat > README.md <<'EOF'
# VIVA — Vietnamese In-Vehicle Assistant
Team VIVA · FPT Automotive Hackathon 2026 · Digital Cockpit

## Mục tiêu
## Kiến trúc
## Vehicle Property đã sử dụng
## Hướng dẫn build
## Hướng dẫn cài lên CarSky
## Thư viện mã nguồn mở đã dùng
EOF
git add -A && git commit -m "chore: khoi tao repo VIVA"
```

### 📋 KẾT QUẢ S4 — báo vào nhóm

```
S4 PLATFORM
- healthz: OK / lỗi
- screenshot: OK (___ KB) / lỗi
- accessibility (ui_tree): OK, ___ elements / lỗi
- shell qua REST: OK / lỗi
- tap: OK / KHÔNG CÓ ROUTE (chưa bật COOLGATE)
- signals đọc được: ___ (liệt kê path)
- signals ghi được: CÓ / KHÔNG
- docker login: host ĐÚNG là ___
- push image: OK / lỗi
- VM có internet: CÓ / KHÔNG
- MCP nối được: CÓ / KHÔNG
- Repo git: link ___
```

---

## 📊 TỔNG HỢP CUỐI NGÀY (Long làm, trước 22:00)

Gộp 4 bảng kết quả thành 1 trang, gửi mentor kèm câu:
> *"Đội đã chạy 4 spike xác minh môi trường, đây là kết quả. Nhờ các anh xem giúp ở office hours Thứ 3."*

Bảng quyết định kiến trúc:

| Nếu... | Thì chốt... |
|---|---|
| S1 = PA-A **và** S4 cho phép thêm Container Node | ASR trong Room, app gọi qua Ethernet Bridge — **kiến trúc lý tưởng** |
| S1 = PA-A **nhưng** không được thêm node | ASR trên laptop, app gọi qua `adb reverse tcp:8080 tcp:8080` |
| S1 = PA-B | Audio bắt ở host → ASR host → đẩy text vào app. **Ghi rõ ranh giới trong write-up** |
| S2 có ≥3 property HVAC OK | Climate skill đi thẳng CarPropertyManager |
| S2 property HVAC toàn `null` | Climate skill đi qua CAN signal `HvacCommand/*`, đồng thời xin hạ tầng wire property |
| S3 MediaStore index được | Media skill dùng MediaStore + MediaBrowserService chuẩn |
| S3 chỉ đọc được file thô | Media skill tự quét thư mục, tự parse ID3 (dùng `jaudiotagger`) |
