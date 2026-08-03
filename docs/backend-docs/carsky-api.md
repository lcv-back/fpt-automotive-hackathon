# CarSky REST API — những gì đã gọi thật và kết quả

> Ghi ngày 02–03/08/2026. Mọi dòng dưới đây là **phản hồi thật của server**, không
> phải suy đoán từ tài liệu. Cái gì chưa gọi thì ghi rõ là chưa gọi.
>
> Không commit token/API key vào repo. Cấu hình đọc từ `backend/.env`
> (đã gitignore), mẫu ở `backend/.env.example`.

## 1. Xác thực — dùng API key, **không** dùng JWT của phiên web

Đây là chỗ mất nhiều thời gian nhất, nên ghi lại để không ai lặp lại:

| Thứ gửi đi | Kết quả |
|---|---|
| Không có header nào | `401 {"error":"UNAUTHORIZED","message":"Missing credentials"}` |
| JWT copy từ phiên đăng nhập web (`alg:HS256`, payload `sub/isAdmin/email`) | `401 ... "Invalid JWT"` — **kể cả token vừa phát, còn 59 phút mới hết hạn** |
| **API key** (`x-api-key: a8k_…`) | ✅ qua được auth |
| **API key** (`Authorization: Bearer a8k_…`) | ✅ qua được auth |
| API key đặt trong cookie | `401 ... "Missing credentials"` |

Vì sao JWT web không dùng được: gốc `https://hackathon-2.carsky.io/` là trang đăng
nhập **Keycloak** (`/auth/realms/hackathon02`, client `rework`). Token mà REST API
chấp nhận là API key phát riêng, không phải session token của Keycloak.

`viva-tools` đọc biến `CARSKY_API_TOKEN` và gửi dạng `Bearer` — **đặt API key vào
biến đó là chạy được**, không phải sửa code.

## 2. Base URL và spec

- Base: `https://hackathon-2.carsky.io/api/v1`
- Spec đầy đủ: **`GET /api/v1/openapi.json`** (71 endpoint). Lưu ý `/api/v1/openapi`
  **404** — đường dẫn đúng có đuôi `.json`. Swagger UI ở `/api/v1/docs`.
- Prefix khác (`/api/...`, `/v1/...`, `/deployments`) đều trả HTML của SPA → chỉ
  `/api/v1/*` mới được proxy sang API.
- Middleware auth chạy **trước** routing: đường dẫn không tồn tại mà thiếu key vẫn
  trả 401, nên đừng dùng 401 để kết luận "endpoint không có".

Spec 128 KB **không commit vào repo** (nội bộ nền tảng, thể lệ 3.6). Tải lại bằng:

```powershell
$k = ((Get-Content backend\.env | Select-String '^CARSKY_API_KEY=') -split '=',2)[1]
Invoke-WebRequest -Uri "https://hackathon-2.carsky.io/api/v1/openapi.json" `
  -Headers @{ "x-api-key" = $k } -OutFile openapi.json
```

## 3. Endpoint đã gọi thật — đều 200

| Endpoint | Dùng cho | Kết quả thật |
|---|---|---|
| `GET /devices` | tra device của đội | 18 device; **VIVA = `v37aa3knc6t1embelr5yi`** |
| `GET /deployments/find?device=<id>` | tìm room đang chạy | VIVA → room `v37aa3knc6t1embelr5yi`, blueprint `RMbeXxTF5ZvkmqzRK04gf`, namespace `room-lgpuafex`, `RUNNING`. **"VIVA 2" (`og4erd2wzaxe5xod8otuj`) không có deployment nào** |
| `GET /deployments/{room}/status` | trạng thái | `RUNNING` |
| `GET /deployments/{room}/nodes` | **V3** | 21 node, 21/21 `Running` — xem `backend/carsky/nodes.json` |
| `GET /deployments/{room}/adb-tunnel` | **V5** | trả `conduitUrl` + lệnh `nydus-reach tunnel adb …` |
| `GET /blueprints/{id}/export` | **V2** backup | 66 KB JSON, đã lưu `backend/carsky/blueprint-VIVA-deploy-backup.json` |
| `GET /signals/{room}` | nguồn tín hiệu | 7 nguồn: 2 CAN (`bcm-can`, `pwt-can`), 1 KUKSA (`central-broker-vss`), 4 GPIO |
| `GET /deployments/{room}/logs/{node}` | log pod | 200 — nhưng xem cảnh báo mục 5 |
| `GET /config/limits`, `/account-limits/effective/{acc}` | quota | `MAX_DEVICES=5`, `MAX_NODES_PER_BLUEPRINT=30`, `MAX_CONCURRENT_DEPLOYMENTS=2`, `MAX_SKYCRAFT_PER_BLUEPRINT=2` |

**Chưa gọi:** `POST /blueprints/{id}/clone` (V2 phần clone) — lệnh ghi, tạo tài
nguyên thật trên nền tảng dùng chung, chờ chốt trước khi chạy.

## 4. 🚫 Cả họ endpoint điều khiển VM đang chết

`screenshot` · `accessibility` · `shell` · `tap` · `text` · `key` · `swipe` ·
`adb-exec` · `container-exec` — tất cả trả:

```
502 {"error":"SERVICE_UNAVAILABLE","message":"Conduit service not configured"}
```

Ba phép thử loại trừ nguyên nhân từ phía đội:

1. Mọi endpoint **không** cần conduit đều 200 với cùng API key → không phải quyền.
2. Lỗi y hệt trên **script-node** (`container-exec` ở IVI Gateway), không riêng node
   Android → không phải do node skycraft.
3. `/account-limits/effective/…` **không có cờ nào** bật/tắt conduit → không phải quota.

→ Thiếu cấu hình phía nền tảng. **Câu hỏi cho BTC/mentor**, kèm nguyên văn lỗi trên.

**Hệ quả:** kế hoạch V11 (`send_signals → screenshot → find_text`) chưa chạy được
qua HTTPS. Đường thay thế là tunnel ở mục 5.

## 5. Hai cái bẫy khi đọc số

- **`/logs/{node}` không phải logcat.** Nó trả log của pod; với node Android các dòng
  lấy về là log WebRTC (`rtc_source_native … UDP throughput`). `VIVA_TRACE` **không**
  nằm ở đây — muốn có trace phải qua `adb logcat` bằng tunnel.
- **`/devices` liệt kê cả device của đội khác** (18 cái, phần lớn `PUBLISHED`). Chỉ
  thao tác trên `VIVA`; đừng gọi lệnh ghi lên id lạ.

## 6. Dev loop hiện dùng được (V5)

API trả sẵn lệnh mở tunnel cho node Android:

```
nydus-reach tunnel adb --conduit https://hackathon-2.carsky.io \
  --namespace room-lgpuafex --node rmbexxtf5zvkmqzrk04gf-n1
```

Sau đó `adb connect <host:port>` → cài APK, `adb logcat`, `screencap`,
`uiautomator dump` đều chạy bản địa, và harness dùng được `--adb` như thiết kế.
**Còn thiếu:** binary `nydus-reach` (tải trong UI CarSky) — chưa ai trong đội xác
nhận đã chạy được lệnh này.

## 7. Node của room VIVA (V3)

21/21 `Running`. Bản đầy đủ ở `backend/carsky/nodes.json`.

| nodeType | Số lượng | Đáng chú ý |
|---|---|---|
| `script-node` | 8 | **IVI Gateway** (`…-n4`) và **PWT Gateway** (`…-n13`) — đúng hai node mentor bảo đọc trước khi viết Luau (M4). Thêm BCM/Climate/VCU/BMS/TCU Gateway |
| `gpio-panel` | 4 | TirePressure, Drive Controls, SeatBelt, Battery — **Drive Controls (`…-n12`) là chỗ đặt tốc độ cho ablation A1** |
| `can-bus` | 2 | BCM CAN (`…-n7`), PWT CAN (`…-n19`) |
| `kuksa-databroker` | 1 | Central Broker VSS (`…-n18`) |
| `skycraft` | 1 | **IVI - Android (`…-n1`)** — nơi cài APK |
| `container` | 2 | TCU-NAD, SeatBelt ECU |
| `eth-bridge` | 2 | IVI Switch, TCU Switch |
| `device-proxy` | 1 | Device Proxy (`…-n14`) |
