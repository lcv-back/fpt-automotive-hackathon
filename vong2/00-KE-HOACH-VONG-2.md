# KẾ HOẠCH VÒNG 2 — TEAM VIVA · FPT AUTOMOTIVE HACKATHON 2026

## Context

Team VIVA đã qua Vòng 1 với proposal "Vietnamese In-Vehicle Assistant" — trợ lý giọng nói tiếng Việt trên AAOS, vertical Digital Cockpit. Đội được BTC đăng ký **4 đề**: Media Player · Climate Control VHAL · Voice-Controlled Assistant · DTC Monitor. Supporter LinhNT169 đã kết nối đội với mentor (anh Thủy, anh Đức).

Vòng 2 chạy 21/07 → **23:59 ngày 10/08/2026**. Tính từ 26/07 còn **15 ngày**. Năng lực đội: part-time ~3–5h/ngày + full 2 ngày cuối tuần (~280 person-hour hiệu dụng). Đội **đã có Device Running trên CarSky**.

Vấn đề cần giải: proposal Vòng 1 được viết dựa trên mô tả starter pack ở webinar, nhưng tài liệu vận hành CarSky cho thấy nền tảng thật khác ở nhiều điểm cốt lõi (VHAL do Script Node Luau cấp, không có ASR sẵn, TTS không có tiếng Việt, USB phải tự đóng gói). Kế hoạch này ánh xạ lại toàn bộ sản phẩm lên **đúng nguyên liệu CarSky thật**, chia việc theo 4 thành viên, và đặt mốc để kịp deadline.

Chiến lược đã chốt: **Voice làm xương sống + mỗi đề còn lại làm sâu đúng MỘT tiêu chí nặng nhất của nó** — bảo hiểm cho cả hai khả năng BGK chấm theo barem tích hợp hoặc theo 4 barem riêng.

---

## PHẦN A — BTC ĐÃ CUNG CẤP SẴN NHỮNG GÌ

### A1. Nền tảng CarSky (đã xác nhận trong tài liệu)

| Hạng mục | Chi tiết | Đội dùng để làm gì |
|---|---|---|
| **CarSky Rework UI** | `carsky.io`, Keycloak SSO, workbench 3 cột (Panel · Stage · Inspector), 7 mục: Devices → Videos → Artifacts → Nydus → Hubs → Registry → Dashboard | Toàn bộ môi trường dev + demo |
| **Blueprint "Started pack"** | 22 node, 4 zone: **CDC Zonal**, BCM Zonal, PWT Zonal, TCU Zonal. Node chính: **IVI-FACE (Android)**, Cluster-FACE (AGL), BCM/TCU/PWT Gateway, Central Broker (VSS) | Room có sẵn — đội không phải dựng |
| **Tín hiệu đã wire sẵn** | Walkthrough cho thấy BCM CAN có `HvacCommand/Driver_Temperature`, `Passenger_Temperature`; PWT có `PWT_VehicleSpeed/Speed_kph` | **Climate skill + Safety Guard có nguồn tín hiệu thật ngay** |
| **Artifact AAOS** | artifact `aaos` (Android Image) v0.0.1: `image` 731.9 MB + `host_package` 463.6 MB. Ngoài ra: `agl`, `usb`, `pwt`, `bcm`, `vss` | Không phải build image AAOS |
| **12 widget** | Screen (stream + **Microphone** + Recorder) · ADB · Shell · Signal Watch · GPIO Panel · **USB Device Proxy** · CAN Panel · CAPL TestScript · **Road Simulator** · Simulator Box · **Text-to-speech** · Log | Dev, test, quay video demo |
| **REST API** | `/api/v1/...` — blueprints, devices, artifacts, deployments, **vms** (screenshot/tap/swipe/text/key/shell/accessibility), **signals** (values/actuate/subscribe/periodic), credentials. Swagger tại `/api/v1/docs` | Tự động hoá test + benchmark |
| **MCP Server** | **42 tool** / 10 nhóm: `screenshot`, `tap`, `swipe`, `input_text`, `press_key`, `ui_tree`, `find_text`, `adb_shell`, `wait_boot`, `send_signals`, `get_signal_values`, `subscribe_signals`, `pod_logs`, `search_logs`, `vm_tunnel_open`, `deploy`, `wait_ready`, `container_shell`… | **Câu chuyện AI** + regression tự động |
| **Container Registry (Zot)** | `registry.carsky.io` (guideline SDV ghi `registry.hackathon-1.carsky.io/<team>` → **phải xác nhận host thật với BTC**). API key dạng `zak_...` | Push image ASR/DTC/delivery của đội |
| **CarSky SDK** | `scout` (daemon hub) · `tether` · `lens` · `probe` với backend **`wavelink` = capture ALSA audio → Opus**; Outpost pin hỗ trợ kind **`audio`** | Đường dự phòng đưa mic thật vào Room |
| **ADB Tunnel** | `vm_tunnel_open` → `adb connect localhost:5038` → Android Studio debug bình thường | Vòng lặp dev |
| **Conduit** | `exposedPorts` trên Container Node → URL `https://<host>/conduit/http/<room-ns>/<node>/<port>/` | Dashboard/API của đội truy cập từ browser |

### A2. Code / tài nguyên mẫu BTC nêu là có cấp

| Nguồn | Nội dung | Trạng thái |
|---|---|---|
| `github.com/fpt-automotive-hackathon/cdc-starter` | Repo starter pack CDC (webinar slide 40) | **Chưa xác nhận đội đã clone được** — hỏi mentor |
| `Hackathon_IVI_CDC_2026.md` | **Bảng tiêu chí chấm + API/property bắt buộc theo từng đề** (guideline CDC trỏ tới) | **Chưa có — phải xin** |
| Repo mẫu CDC | "Snippet đọc/ghi property, media app cơ bản — team hạ tầng/BTC cung cấp trước ngày thi" | **Chưa xác nhận** |
| Workshop "AAOS for hackers" | Public từ 25/06 | Xem lại nếu chưa |
| Tutorial AEB Device-in-the-Loop | Blueprint 4 node + 2 Dockerfile + DBC + VSS + scenario + thuật toán mẫu + dashboard + code-server | **Mẫu tốt nhất để học pattern Container Node** — xin bộ source |
| Mentor | ≥1 mentor/đội; office hours **Thứ 3 & 5, 19:00–20:00** | Còn 4 buổi: 28/07, 30/07, 04/08, 06/08 |

### A3. BTC KHÔNG cung cấp — đội phải tự làm

| Thiếu | Hệ quả | Cách xử lý trong kế hoạch |
|---|---|---|
| **ASR tiếng Việt** | Không có "minimal pipeline audio→text" như webinar mô tả | Container Node `viva-asr` (PhoWhisper/whisper-small INT8) |
| **TTS tiếng Việt** | Widget TTS chỉ có `zh-TW`, `en-US` | Android `TextToSpeech` nếu image có `vi-VN`; fallback pre-render audio |
| **Nội dung media** | Không có nhạc sẵn | Tự build `usb.img` FAT32, upload Artifact category USB |
| **Delivery/đơn hàng** | Không có | Container `viva-delivery` hoặc module in-app |
| **Wake word** | Không có | Push-to-talk là must; wake word là nice-to-have |

---

## PHẦN B — KIẾN TRÚC CHỐT (ánh xạ lên nguyên liệu CarSky thật)

```
        ┌──────── Room CarSky (Kubernetes) ────────────────────────────────┐
        │                                                                  │
 Mic  ──┼─► Widget Screen (WebRTC mic) ──► Skycraft Node "IVI-FACE" (AAOS) │
 (browser)                                  ┌────────────────────────────┐  │
        │                                   │  VIVA App (Kotlin)         │  │
        │                                   │  ① AudioRecord + VAD       │  │
        │                                   │  ② → ASR qua Ethernet      │  │
        │  ┌───────────────────────────┐    │  ③ Intent Router T0/T1     │  │
        │  │ Container "viva-asr"      │◄───┤  ④ SAFETY GUARD (tất định) │  │
        │  │ PhoWhisper INT8 / HTTP    │    │  ⑤ Skills ×4               │  │
        │  └───────────────────────────┘    │  ⑥ TTS + HMI Compose       │  │
        │  ┌───────────────────────────┐    └──────┬──────┬──────┬───────┘  │
        │  │ Container "viva-svc"      │◄──────────┘      │      │          │
        │  │  · DTC reader (ISO-TP)    │            CarPropertyManager      │
        │  │  · Delivery simulator     │                  │      │          │
        │  │  · Dashboard :8080        │                  ▼      ▼          │
        │  └────────┬──────────────────┘         Script Node   MediaSession │
        │           │ /run/nydus/uds-<pin>.sock  (VehicleServer  ← USB img  │
        │           ▼                             gRPC Luau)     Device     │
        │      CAN Bus Node ◄──────────────────────────┘         Proxy Node │
        └──────────────────────────────────────────────────────────────────┘
```

**Kênh nối app ↔ container** (theo thứ tự ưu tiên, chốt sau Spike):
1. **Ethernet Bridge** — Skycraft có pin `ethernet` với IP tĩnh (bridge chạy DHCP gán MAC deterministic). App gọi `http://10.99.0.2:8080`. *Sạch nhất.*
2. **AF_VSOCK** — Container Node (chạy trên host K8s) ↔ guest VM qua CID, không cần cấu hình mạng.
3. **`adb reverse tcp:8080 tcp:8080`** — service chạy trên laptop đội. *Dự phòng khi không được thêm node.*

**Nguồn DTC** (phát hiện mới, thay hoàn toàn phương án "tự mock"):
- Script Node có `nydus.uds.server({ dtcs = {{id=0x010420, status=0x09}}, dids={...}, routines={...} })` — **mô phỏng ECU thật**.
- Mỗi CAN Bus pin được runtime mở sẵn **socket ISO-TP** tại `/run/nydus/uds-<pin_name>.sock`. Container Node kết nối, gửi PDU thô — runtime tự lo ISO-TP segmentation.
- Request chuẩn: `19 02 FF` (ReadDTC by status) → `59 02 FF ...`; `22 F1 90` (VIN); `10 03` (extended session).
- → **DTC Monitor của VIVA là tester UDS thật, không phải mock.** Đây là điểm ăn "Simulation fidelity" + "Extensibility sang ECU thật".

---

## PHẦN C — PHÂN VAI

| Người | Vai trò | Sở hữu (owner) | Đường găng? |
|---|---|---|---|
| **Ngô Văn Long** | Lead · Voice AI & Kiến trúc | Voice pipeline (VAD/ASR/TTS), Intent Router T0/T1, hợp đồng interface giữa các module, phương pháp benchmark, write-up câu chuyện AI, điều phối demo | ✅ **CÓ** |
| **Lê Công Vĩ** | Senior Backend · Agent & DevOps | Container Node + Docker image + push Zot, `viva-asr`, `viva-svc` (DTC reader + delivery + dashboard conduit), **harness tự động qua CarSky REST/MCP** | ✅ **CÓ** |
| **Lê Đức Tùng** | Embedded · VHAL & DTC | Lớp `CarPropertyManager`, **Safety Guard policy engine**, DTC analyzer (tần suất/xu hướng/tương quan), bảng property đã dùng cho README | ✅ **CÓ** |
| **Việt Dương** | Android · HMI & Media | App shell AAOS, HMI Compose + car-ui-lib, **MediaBrowserService/MediaSession + plugin interface + album-art cache**, `usb.img`, đóng gói APK | ⬜ Song song |

**Quy tắc phối hợp:**
- Mọi interface giữa module do Long chốt bằng file `docs/contracts.md` **trước 28/07** — sau đó ai làm phần nấy, không sửa chéo.
- Daily 15 phút 21:30 trên nhóm chat: 3 câu — hôm qua xong gì / hôm nay làm gì / đang bị chặn bởi ai.
- Mọi thứ chặn >4h → đưa lên nhóm ngay, không tự xoay qua đêm.

---

## PHẦN D — LỊCH THỰC THI THEO MỐC

### 🔴 M0 — HÔM NAY 26/07 (Chủ nhật, full day): 4 SPIKE + email mentor

Mục tiêu: **gỡ hết ẩn số kiến trúc trong 1 ngày**. Không viết code sản phẩm.

| # | Người | Việc | Định nghĩa "xong" |
|---|---|---|---|
| S0 | Long | Gửi email/tin nhắn cho chị Linh + anh Thủy + anh Đức (bản nháp đã có) | Đã gửi trước 12:00 |
| S1 | Long | **Spike MIC**: APK tối giản `AudioRecord` ghi 5s → `adb pull` → nghe. Bật Microphone trên widget Screen | Kết luận rõ: mic vào được VM / không |
| S2 | Tùng | **Spike VHAL**: `CarPropertyManager.get/setProperty` cho HVAC_POWER_ON, HVAC_TEMPERATURE_SET, HVAC_FAN_SPEED, DOOR_LOCK, PERF_VEHICLE_SPEED, GEAR_SELECTION, NIGHT_MODE. Đối chiếu widget Signal Watch | **Bảng property: cái nào OK, cái nào trả `null`** |
| S3 | Dương | **Spike MEDIA**: build `usb.img` FAT32 (5 mp3 + album art) → Artifact category USB → widget USB Device Proxy → Plug | Thấy file trong `/sdcard/Music/usb_1` |
| S4 | Vĩ | **Spike PLATFORM**: tạo API key; `curl /api/v1/healthz`; gọi `screenshot` + `send_signals`; `docker login` registry; nối MCP vào Claude Code; kiểm tra VM có internet không | Chạy được 1 lệnh MCP điều khiển màn hình AAOS |
| S5 | Vĩ | Tạo repo Git + khung README + `.gitignore` (không commit API key) | Repo có commit đầu |

**Deliverable cuối ngày:** 1 trang tóm tắt kết quả 4 spike, gửi mentor trước office hours.

### 🟠 M1 — 27–28/07: Chốt kiến trúc (office hours 28/07 19:00)

| Người | Việc | Deadline |
|---|---|---|
| Long | `docs/contracts.md`: schema Intent, schema Safety Guard verdict, API ASR (`POST /asr` → `{text, conf, ms}`), API DTC (`GET /dtc`), event bus in-app | 28/07 tối |
| Long | Chốt **phương án voice A hoặc B** dựa trên S1 | 28/07 tại office hours |
| Vĩ | Dockerfile `viva-asr` + push thử image rỗng lên Zot → xác nhận cluster pull được | 28/07 |
| Tùng | Skeleton Safety Guard: interface `evaluate(intent, vehicleState) → ALLOW / DENY(reason) / CONFIRM` | 28/07 |
| Dương | App shell AAOS chạy trên Device, HMI khung 3 vùng (trạng thái xe · hội thoại · skill hiện hành) | 28/07 |
| Cả đội | **Office hours 28/07**: trình 4 spike, xin `Hackathon_IVI_CDC_2026.md`, xin wire property thiếu, xin quyền thêm Container Node, chốt cách chấm (1 sản phẩm hay 4 đề) | 28/07 20:00 |

### 🟡 M2 — 29–31/07: XƯƠNG SỐNG SỐNG (mốc quan trọng nhất)

> **Định nghĩa M2 đạt:** nói *"Hạ điều hòa xuống 22 độ"* bằng tiếng Việt → nhiệt độ đổi thật qua VHAL → HMI cập nhật → log in ra timestamp đủ 6 chặng. **Một câu, một luồng, chạy được.**

| Người | Việc | Deadline |
|---|---|---|
| Long | VAD (Silero ONNX) + client gọi ASR + Intent T0 grammar cho 5 lệnh đầu | 30/07 |
| Vĩ | `viva-asr` chạy thật trong Room (hoặc trên laptop qua `adb reverse`), trả text tiếng Việt | 30/07 |
| Vĩ | **Harness v1**: script Python đọc log app qua `adb_shell`, ghi CSV timestamp 6 chặng | 31/07 |
| Tùng | Safety Guard G1 (khoá theo tốc độ/gear) + lớp VHAL wrapper hoàn chỉnh | 31/07 |
| Dương | HMI phản chiếu HVAC + volume real-time; TTS đọc phản hồi (dù chỉ 3 câu mẫu) | 31/07 |
| Long | **Instrument latency ngay trong app**: mọi chặng ghi mốc `SystemClock.elapsedRealtimeNanos` | 30/07 |

**Checkpoint 30/07 office hours:** demo xương sống cho mentor, xin feedback sớm.

⚠️ **Nếu 31/07 chưa đạt M2 → dừng mọi tính năng khác, cả 4 người dồn vào xương sống.** Không có xương sống thì không có sản phẩm.

### 🟢 M3 — 01–02/08 (cuối tuần, full): 4 SKILL + TÍNH NĂNG CHỮ KÝ

| Đề | Tính năng chữ ký (chạm tiêu chí nặng nhất) | Người | Deadline |
|---|---|---|---|
| **#1 Media** | `MediaBrowserService` + `MediaSession` tách lớp; **interface plugin `MediaSourceProvider`** với 2 implementation (USB ảo + local storage); **cache album art** (LruCache + disk), không ANR khi load | Dương | 02/08 |
| **#2 Climate** | Đi **đủ 4 tầng** App → CarPropertyManager → Car Service → VHAL (Script Node); đúng `areaId`; **callback real-time** hai chiều (đổi từ GPIO Panel → HMI tự cập nhật) | Tùng | 02/08 |
| **#3 Voice** | Coverage **cả** car control **lẫn** delivery flow; router T0 + T1; TTS phản hồi | Long | 02/08 |
| **#4 DTC** | **Tester UDS thật** qua `/run/nydus/uds-*.sock` (`19 02 FF`); phân loại nhóm P/C/B/U; trạng thái active/pending/stored + severity; **analyzer: tần suất · xu hướng · tương quan lỗi đồng thời**; export JSON | Tùng + Vĩ | 02/08 |
| Delivery flow | Container `viva-svc`: danh sách đơn, chặng kế tiếp, xác nhận giao; dashboard qua conduit | Vĩ | 02/08 |
| Safety Guard | G2 (xác nhận lệnh nhạy cảm) + G3 (whitelist function cho LLM nếu có T2) | Tùng | 02/08 |

### 🔵 M4 — 03–05/08: TÍCH HỢP & FEATURE FREEZE

| Người | Việc | Deadline |
|---|---|---|
| Cả đội | Tích hợp 4 skill vào một app, một luồng, một HMI | 04/08 |
| Long | Tinh chỉnh ngưỡng VAD/ASR, tối ưu latency về mục tiêu <1,5s | 05/08 |
| Vĩ | **Harness v2**: bộ 20+ câu lệnh chạy tự động, có `send_signals` set trạng thái xe, `screenshot` làm bằng chứng | 04/08 |
| Tùng | **Safety scenario pack**: ≥6 kịch bản pass/fail chạy tự động | 04/08 |
| Cả đội | **Office hours 04/08**: demo bản tích hợp, xin feedback cuối cùng còn kịp sửa | 04/08 |
| — | 🚫 **FEATURE FREEZE 23:59 ngày 05/08** — sau mốc này chỉ sửa lỗi và đánh bóng | 05/08 |

### 🟣 M5 — 06–07/08: ĐO ĐẠC & TÀI LIỆU

| Người | Việc | Deadline |
|---|---|---|
| Vĩ | Chạy benchmark ≥20 câu × 3 mức nhiễu, **edge-only vs hybrid**, xuất **p50/p95** + biểu đồ | 06/08 |
| Tùng | Chạy safety pack, xuất báo cáo pass/fail | 06/08 |
| Long | **Write-up câu chuyện AI**: prompt đã dùng, AI hỗ trợ tốt ở đâu, AI sai ở đâu, MCP-driven testing | 07/08 |
| Vĩ | **README hoàn chỉnh**: mục tiêu · kiến trúc · **danh sách Vehicle Property đã dùng** · hướng dẫn build · hướng dẫn cài lên CarSky · nguồn thư viện open-source | 07/08 |
| Dương | Sơ đồ kiến trúc (voice → ASR → NLU → guard → dispatcher → vehicle) + extension points | 07/08 |
| Cả đội | **Office hours 06/08**: checkpoint cuối với mentor | 06/08 |

### ⚫ M6 — 08–09/08 (cuối tuần, full): VIDEO & TỔNG DUYỆT

| Người | Việc | Deadline |
|---|---|---|
| Long | Kịch bản video 5–7' theo Phụ lục B của proposal, chốt lời thoại | 08/08 sáng |
| Cả đội | **Quay video bằng Recorder Part của widget Screen** — tải `.mp4` về ngay | 08/08 |
| Dương | Dựng video (chèn overlay log VHAL / Signal Watch song song để chứng minh "không mock") | 09/08 |
| Vĩ | **Backup video dự phòng** (guideline CarSky yêu cầu rõ) + smoke test cuối trên Device sạch | 09/08 |
| Cả đội | **Tổng duyệt 2 lần**: cài APK sạch → chạy full kịch bản → không crash trong logcat | 09/08 tối |

### 🏁 M7 — 10/08: NỘP TRƯỚC TRƯA

- 10:00 — push repo cuối, kiểm tra `.env`/API key **không** bị commit
- 11:00 — đối chiếu checklist nộp bài, nộp
- 12:00 — **đã nộp xong**. 12 tiếng buffer còn lại là bảo hiểm, không phải thời gian làm việc

**Checklist nộp bài (guideline CDC):**
- [ ] Build release/debug không lỗi
- [ ] Đã cài và test trên đúng Device của đội trên CarSky
- [ ] Đã smoke test qua Signal Watch / GPIO Panel, không crash
- [ ] Đã quay video demo dự phòng
- [ ] Repo Git đầy đủ README, đã push trước deadline
- [ ] Ghi rõ nguồn thư viện mã nguồn mở
- [ ] Source code · Documentation · Video 5–7' · Write-up (có phần AI)

---

## PHẦN E — VERIFICATION (cách kiểm chứng end-to-end)

**1. Kiểm chứng "điều khiển thật, không mock" (Tính kết dính 20đ)**
```bash
# Terminal 1: theo dõi tín hiệu phía nền tảng
#   widget Signal Watch trỏ vào HvacCommand/Driver_Temperature
# Terminal 2: đọc log app
adb connect localhost:<port> && adb logcat -s VIVA
# Nói: "Hạ điều hòa xuống 22 độ"
# PASS khi: Signal Watch đổi giá trị ĐỒNG THỜI với log app + HMI đổi
```

**2. Kiểm chứng Safety Guard (điểm khác biệt của đội)**
```bash
# Set xe đang chạy qua REST
curl -X POST -H "X-API-Key: $KEY" \
  "$BASE/api/v1/signals/$ROOM/$NODE/actuate" \
  -d '{"signals":[{"path":"Vehicle.Speed","value":60}]}'
# Nói: "Mở cửa"
# PASS khi: hệ thống TỪ CHỐI + giải thích lý do + đề nghị làm khi xe dừng
# Lặp lại với Vehicle.Speed=0 → PASS khi thực hiện được
```

**3. Kiểm chứng latency <1,5s (cam kết trong proposal)**
```bash
python tools/benchmark.py --utterances 20 --noise-levels 3 --mode edge
python tools/benchmark.py --utterances 20 --noise-levels 3 --mode hybrid
# PASS khi: p95 đường edge < 1500ms, báo cáo có breakdown 6 chặng
```

**4. Kiểm chứng DTC là UDS thật, không mock**
```bash
# Trong container viva-svc
python tools/uds_probe.py --sock /run/nydus/uds-can_main.sock --req "19 02 FF"
# PASS khi: nhận về PDU bắt đầu bằng 0x59, parse ra danh sách DTC
```

**5. Kiểm chứng "code chạy lại được" (Chất lượng thực thi 20đ)**
- Người ngoài đội làm theo đúng README trên máy sạch: clone → build → `adb install` → chạy được kịch bản chính. Nhờ **chị Linh hoặc mentor thử** ngày 08/08.

**6. Kiểm chứng regression tự động qua MCP (câu chuyện AI)**
```
screenshot → ui_tree → find_text("22°C") → PASS/FAIL
```
Chạy lại sau mỗi lần build; log kết quả vào write-up.

---

## PHẦN F — RỦI RO & PHƯƠNG ÁN

| Rủi ro | Dấu hiệu sớm | Phương án |
|---|---|---|
| **Mic không vào được VM** | Spike S1 thất bại 26/07 | **PA-B**: audio bắt tại host → ASR trên laptop → đẩy text vào app qua `adb shell am broadcast` / REST `/text`. Giọng nói vẫn thật; **ghi rõ ranh giới hệ thống trong write-up** |
| **Không được thêm Container Node** | Mentor trả lời không có quyền editor | ASR + `viva-svc` chạy trên laptop đội, app gọi qua `adb reverse tcp:8080 tcp:8080` |
| **Property HVAC/DOOR trả `null`** | Spike S2 26/07 | Báo team hạ tầng ngay (guideline nói rõ: **không debug tiếp phía app**). Trong lúc chờ, Climate skill đi qua CAN signal `HvacCommand/Driver_Temperature` đã có sẵn |
| **ASR quá chậm trên pod CPU** | RTF đo được >0.5 | Hạ xuống `whisper-tiny` INT8; chỉ decode chunk cuối (streaming); cân nhắc chạy ASR trên laptop |
| **TTS không có tiếng Việt** | Kiểm tra `TextToSpeech.getAvailableLanguages()` | Pre-render ~30 câu phản hồi cố định thành audio đóng gói trong APK |
| **Room sự cố đúng lúc demo** | — | Video dự phòng quay từ 08/08 (guideline CarSky khuyến nghị) |
| **Không kịp tích hợp** | 05/08 chưa freeze được | Cắt theo thứ tự: cloud LLM T2 → wake word → barge-in → theme ngày/đêm → T1 classifier (giữ T0 grammar) |
| **Registry host sai** | `docker push` lỗi 401/không thấy image | Xác nhận host thật với BTC: `registry.carsky.io` hay `registry.hackathon-1.carsky.io/<team>` |

---

## PHẦN G — NHỮNG THỨ KHÔNG LÀM (cắt có chủ đích)

Ghi ra để cả đội không bị cám dỗ:

- ❌ Wake word "Vivi ơi" — dùng push-to-talk. Chỉ làm nếu dư thời gian sau 05/08
- ❌ Cloud LLM T2 — không nằm trong cam kết 1,5s, phụ thuộc internet của VM
- ❌ Barge-in, noise augmentation, cá nhân hoá theo giọng
- ❌ Theme ngày/đêm (trừ khi `NIGHT_MODE` property đã wire sẵn → khi đó rẻ, làm)
- ❌ Cross-vertical sang SOVD/CCS — hấp dẫn nhưng 15 ngày không đủ
- ❌ Tự dựng lại blueprint (thể lệ 6.8: tự xây lại thứ đã có **không được cộng điểm**)

---

## PHẦN H — VIỆC PHẢI LÀM NGAY TRONG 2 GIỜ TỚI

1. **Long** — gửi tin nhắn cho chị Linh + anh Thủy + anh Đức (bản nháp đã soạn), thêm 2 mục: xin bộ source tutorial AEB và xác nhận host registry
2. **Long** — chia file kế hoạch này vào nhóm chat, mỗi người xác nhận đã đọc phần vai trò của mình
3. **Cả 4 người** — bắt đầu spike của mình, báo kết quả vào nhóm trước 21:00 hôm nay
4. **Vĩ** — tạo repo Git + mời 3 người còn lại + push khung README
