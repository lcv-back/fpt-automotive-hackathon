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

**`POST /blueprints/{id}/clone` — đã chạy 03/08.** Kết quả ở
`backend/carsky/blueprint-clone-response.json`: blueprint mới
`6deadb05-c856-4dab-976b-432b0fac0658`, tên `VIVA-deploy-clone-0803`,
`parentBlueprintId` trỏ đúng bản gốc.

⚠️ **`name` là field bắt buộc**, và client cũ POST body rỗng nên lệnh clone
chưa từng chạy được — xem PR `fix/carsky-clone-name`. Bài học chung: mọi
endpoint POST trong client này đều viết từ tài liệu HTML chứ chưa gọi thật,
nên **đối chiếu lại với `openapi.json` trước khi tin**.

**Chưa gọi:** `POST /deployments` (deploy bản clone, phần cuối của V2) — xem
mục 8.

## 4. 🚫 Cả họ endpoint điều khiển VM đang chết

`screenshot` · `accessibility` · `shell` · `tap` · `text` · `key` · `swipe` ·
`adb-exec` · `container-exec` — tất cả trả:

```
502 {"error":"SERVICE_UNAVAILABLE","message":"Conduit service not configured"}
```

Bốn phép thử loại trừ nguyên nhân từ phía đội:

1. Mọi endpoint **không** cần conduit đều 200 với cùng API key → không phải quyền.
2. Lỗi y hệt trên **script-node** (`container-exec` ở IVI Gateway), không riêng node
   Android → không phải do node skycraft.
3. `/account-limits/effective/…` **không có cờ nào** bật/tắt conduit → không phải quota.
4. 🆕 **05/08 21:35 — thử lại với room vừa deploy sạch, 22/22 node `Running`:** vẫn
   `502`. Xem mục 4b.

→ Thiếu cấu hình phía nền tảng. **Câu hỏi cho BTC/mentor**, kèm nguyên văn lỗi trên.

### 4b. Đường dẫn đúng của họ endpoint này — bản trước ghi thiếu

Mục 4 liệt kê tên endpoint nhưng không ghi route, và đoán route theo kiểu
`/deployments/{room}/nodes/{node}/shell` sẽ nhận `404 Route not found` — **404 đó
không phải Conduit chết**, chỉ là gõ sai địa chỉ. Route thật, lấy từ
`/api/v1/openapi.json` (73 route):

```
POST /api/v1/vms/{roomId}/{nodeKey}/adb-shell     ← cai APK, chay lenh
POST /api/v1/vms/{roomId}/{nodeKey}/shell
POST /api/v1/vms/{roomId}/{nodeKey}/screenshot · /tap · /text · /key · /swipe
POST /api/v1/deployments/{roomId}/adb-exec/{nodeKey}
POST /api/v1/deployments/{roomId}/container-exec/{nodeKey}
GET  /api/v1/signals/{roomId}/{nodeKey}/values    ← doc tin hieu VSS
POST /api/v1/signals/{roomId}/{nodeKey}/actuate   ← dat tin hieu (A1 dat toc do)
```

Gọi đúng `POST /vms/{room}/{node}/adb-shell` với node `IVI - Android` lúc room
đang chạy đủ 22 node vẫn trả:

```
502 {"error":"SERVICE_UNAVAILABLE","message":"Conduit service not configured"}
```

**Đây là bằng chứng mạnh nhất để leo thang.** Trước đây còn có thể nghi "tại room
không chạy"; nay room chạy đủ, `adb-tunnel` trả cấu hình hợp lệ, mọi endpoint khác
200, riêng họ Conduit 502. Không còn giả thuyết nào từ phía đội.

### 4c. Room từng biến mất — 05/08

Chiều 05/08, giao diện web không kết nối được: `Data WebSocket closed (code 4000)`
và mọi part (`Device Proxy`, `face-screen`, `face-audio`, `face-touch-panel`) đều
hết giờ. Nguyên nhân **không phải Conduit**:

```
GET /deployments/v37aa3knc6t1embelr5yi/nodes
404 {"error":"NOT_FOUND","message":"No deployment found for this room in current profile"}
```

Cả 4 device (`Gemini`, `Gemini 2`, `VIVA`, `VIVA (Copy)`) đều `operational: IDLE`,
`lastSeenAt: null` — **không có deployment nào tồn tại**. Room `og4erd2wzaxe5xod8otuj`
từng chạy 22/22 node ở V7 cũng không còn trong danh sách device: device đó đã bị xoá.

Khắc phục: deploy lại blueprint `6deadb05-…` (chính bản đã dựng ra room V7) lên
device `VIVA`:

```
POST /deployments {blueprintId, roomId: v37aa3knc6t1embelr5yi, name: "VIVA-demo-0805"}
→ id 933cf72a-0a81-4b48-a401-725850eca3d8, namespace room-5s98rj6x, status PENDING
→ sau ~3 phut: 22/22 node Running, device VIVA chuyen IDLE → BUSY
```

**Bài học vận hành:** deployment CarSky không sống mãi. Trước mỗi buổi làm việc
hoặc trước khi quay demo, kiểm một dòng:

```powershell
go run ./cmd/viva-tools carsky nodes --room $env:CARSKY_ROOM_ID
```

404 nghĩa là phải deploy lại, không phải nền tảng hỏng.

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

## 8. Deploy bản clone — lệnh cụ thể, và vì sao chưa bấm

Phần cuối của V2 (*"Clone Running"*) là một lệnh:

```powershell
$k = ((Get-Content backend\.env | Select-String '^CARSKY_API_KEY=') -split '=',2)[1]
$body = @{
  blueprintId = "6deadb05-c856-4dab-976b-432b0fac0658"   # bản clone
  roomId      = "og4erd2wzaxe5xod8otuj"                  # device VIVA 2, dang khong co deployment
  name        = "VIVA-deploy-clone-0803"
} | ConvertTo-Json -Compress

Invoke-WebRequest -Uri "https://hackathon-2.carsky.io/api/v1/deployments" -Method POST `
  -Headers @{ "x-api-key" = $k; "Content-Type" = "application/json" } -Body $body
```

Hai điều phải biết trước khi bấm:

- **Quota `MAX_CONCURRENT_DEPLOYMENTS_PER_ACCOUNT = 2`**, đội đang dùng 1 (room VIVA
  đang chạy). Deploy bản clone là lấy nốt slot thứ hai — sau đó không deploy thêm
  được gì cho tới khi xoá một cái.
- Deploy vào **VIVA 2** thì không đụng gì tới room demo. Đừng deploy đè lên
  `v37aa3knc6t1embelr5yi`.

Gỡ khi cần: `DELETE /api/v1/deployments/{roomId}`.

## 9. Tự động deploy khi merge vào `main` — làm được tới đâu

Làm được, nhưng **không phải cho thứ quan trọng nhất**:

| Việc | Tự động được? | Vì sao |
|---|---|---|
| Deploy/redeploy blueprint | ✅ | `POST /deployments` + `DELETE /deployments/{roomId}`, chỉ cần API key trong GitHub Secrets |
| Cập nhật container `viva-asr` | ⚠️ một phần | Build + push + restart ✅ tự động; **đổi image phải làm tay trong web UI** vì `PATCH /nodes/{id}` trả 404 (xem 9c) |
| **Cài APK lên node Android** | ❌ | Cần `adb`, mà `adb-exec`/`shell` đang chết vì Conduit (mục 4). Không có đường HTTPS nào thay thế |

### 9b. Bổ sung 06/08 — hai endpoint bảng trên chưa tính tới

Đọc lại `openapi.json` (73 endpoint) thì có hai cái đổi kết luận:

```
PATCH  /api/v1/nodes/{nodeId}                       Update node, `config` là object tự do
POST   /api/v1/deployments/{roomId}/restart/{node}  Restart specific node
```

Cái thứ hai gỡ phản đối số 2 bên dưới: **không phải dựng lại cả room**, bán kính
ảnh hưởng còn đúng một container trong 22 node.

Nhưng có một cái bẫy: node `VIVA ASR` (`b8eada00-d137-4fdc-a131-2197b1d1356b`)
ghim image bằng **digest**, nên push đè lên một tag rồi restart sẽ kéo lại đúng
image cũ. Chuỗi bắt buộc là bốn bước:

```
build image → push registry → PATCH /nodes/{id} đổi digest → restart/{node}
```

Và `config` của node còn chứa ba biến `ASR_DEVICE` / `ASR_MODEL_NAME` /
`ASR_COMPUTE_TYPE`. PATCH mà chỉ gửi `image` rất có thể ghi đè cả object và xoá
sạch chúng — ASR khởi động với model mặc định mà không báo lỗi. Phải đọc config
hiện tại, trộn, rồi mới ghi.

**Chặn ở đâu:** registry đòi credential riêng, API key CarSky không dùng được.
Thử 06/08 trên `GET /v2/viva/viva-asr/tags/list` — `Basic viva:<key>`,
`Bearer <key>` và ẩn danh đều **401**. Image hiện tại đã nằm trên registry nên
có người push được rồi; cần xin lại đúng cặp credential đó.

> ✅ **Đã gỡ 07/08.** `backend/.env` giờ có `CARSKY_REGISTRY_USER` +
> `CARSKY_REGISTRY_TOKEN`, và cặp đó chạy thật: `GET /v2/viva/viva-asr/tags/list`
> → **200**. Đã push thành công tag `0.2.0`
> (`sha256:da1ea85e…`) qua workflow. Ba secret cũng đã đặt trên repo:
> `CARSKY_API_KEY`, `CARSKY_REGISTRY_USERNAME`, `CARSKY_REGISTRY_PASSWORD`.

**Đã có sẵn:** `.github/workflows/carsky-deploy-asr.yml` và
`carsky-push-asr-image.yml`. Cả hai là `workflow_dispatch`.

### 9c. Đính chính 07/08 — `PATCH /nodes/{id}` KHÔNG tồn tại

Mục 9b ở trên lấy `PATCH /api/v1/nodes/{nodeId}` **từ `openapi.json` mà chưa gọi
thử**. Gọi thật ngày 07/08:

```
PATCH /api/v1/nodes/b8eada00-d137-4fdc-a131-2197b1d1356b   →  404
```

Cả lần PATCH chính lẫn lần lùi lại đều 404 trên cùng endpoint đó. Đây đúng lớp
lỗi mục 10 đã ghi cho `POST /nodes/{nodeId}/pins`: **spec khai nhiều hơn thứ
server chạy được.** Bài học: đừng kết luận "đường đi đã rõ" từ `openapi.json`
khi chưa gọi một lần.

**Ba đường thay thế đều tắc**, kiểm bằng chính `openapi.json`:

| Đường | Vì sao không dùng được |
|---|---|
| `PATCH /blueprints/{id}` | Schema chỉ nhận `name` + `description` |
| `POST /blueprints/{id}/batch` | Chỉ có 3 op: `addNode`, `addPin`, `addEdge` — toàn thao tác THÊM, không có update |
| `POST /blueprints/import` | 400, enum `pinType` không nhận `ETHERNET` (mục 10) |

🔴 **Đừng `DELETE /nodes/{nodeId}` rồi tạo lại.** Node cần một pin `ETHERNET` nối
vào `IVI Switch`, mà mục 10 đã chứng minh pin `ETHERNET` **chỉ tạo được trong web
UI**. Xoá node là đường một chiều làm hỏng room demo.

**Chuỗi đúng — bốn bước, bước 3 làm tay:**

```
build image → push registry → đổi image trong WEB UI → restart/{node}
```

`POST /deployments/{roomId}/restart/{node}` là endpoint **có thật**, đã chạy
được, nên bước 4 vẫn tự động. Chỉ bước 3 kẹt cho tới khi CarSky bật route PATCH.

**Hệ quả cho `carsky-deploy-asr.yml`:** workflow đã bỏ bước PATCH, thay bằng bước
đối chiếu image trên node với ô `image` và từ chối chạy nếu chưa khớp — không có
bước đó thì nó sẽ restart node với image **cũ** rồi báo xanh. Bước lùi tự động
cũng bỏ, vì nó lùi bằng chính route 404 đó.

**Đã xác nhận đúng, không phải nghi ngờ:** `GET /deployments/{roomId}/nodes` trả
node id vào trường **`name`** (không phải `id`), nên bước verify của workflow so
`.name == node_id` là đúng.

⚠️ **`CARSKY_DEVICE_ID` trong `backend/.env` đang sai**: giá trị
`og4erd2wzaxe5xod8otuj` là **VIVA 2**, không phải room demo, nên
`GET /deployments/find?device=$CARSKY_DEVICE_ID` trả `[]`. Dùng
`CARSKY_ROOM_ID` (`v37aa3knc6t1embelr5yi`) làm tham số `device` thì ra đúng
deployment `VIVA-demo-0805`, blueprint `6deadb05-…`.

Nghĩa là "CD lên CarSky" **không** giao được APK — tức là không giao được thứ cả
demo phụ thuộc vào. Và ba lý do nữa để **không** bật auto-deploy theo push `main`:

1. **Quota 2 deployment.** Một workflow chạy mỗi lần merge sẽ đụng trần rất nhanh.
2. **Redeploy là phá room đang chạy.** Merge một PR docs lúc đang tổng duyệt mà
   room bị dựng lại thì hỏng buổi duyệt.
3. **Freeze 05/08 rồi demo.** Đúng giai đoạn cần môi trường đứng yên nhất.

**Đề xuất:** workflow chạy tay (`workflow_dispatch`) có ô nhập `roomId` + `blueprintId`,
chứ không phải `on: push`. Vẫn được tính là tự động hoá, mà không có nguy cơ một
commit docs làm sập room lúc 21:00. Khi Conduit được bật thì thêm bước cài APK và
lúc đó mới đáng bàn tới `on: push` cho nhánh release.

## 10. 🚫 API công khai KHÔNG tạo được pin `ETHERNET` — và hệ quả cho V2

Phát hiện 04/08 khi làm V7 (thêm Container Node `viva-asr`).

**Tạo node thì được.** `POST /blueprints/{id}/nodes` trả **201**, và chấp nhận cả
image dạng **digest**:

```
registry.hackathon-2.carsky.io/viva/viva-asr@sha256:63c2c56a...
```

**Nhưng không nối được vào mạng.** Container node cần một pin `ETHERNET` (đúng như
`TCU-NAD` đang có: `pinType=ETHERNET, direction=OUTPUT, properties.address=10.99.0.22`)
rồi nối vào pin của `IVI Switch`. Cả ba đường đều bị chặn:

| Đường | Kết quả |
|---|---|
| `POST /nodes/{nodeId}/pins` | **404 Route not found** — spec có ghi, server không có route |
| `POST /blueprints/{id}/batch` với `addPin` | **400** `Invalid option: expected one of "VHAL"\|"KUKSA"\|"CAN"\|"LIN"\|"VIDEO"\|"GPIO"\|"GENERIC"` |
| `POST /blueprints/import` | **400**, cùng lỗi enum |

Server **tự** kiểm enum, không phải spec cũ: cả ba nơi đều trả về đúng danh sách 7
loại, và `ETHERNET` không nằm trong đó — dù blueprint do chính nền tảng sinh ra thì
đang dùng nó.

→ **Pin ETHERNET chỉ tạo được trong UI CarSky.** Muốn thêm container node có mạng
thì: tạo node bằng API (hoặc UI), rồi **vào UI thêm pin + nối dây**.

`PATCH /pins/{pinId}` cũng **404** — nên sau khi pin được tạo trong UI, cũng không
sửa được `properties.address` bằng API. Địa chỉ IP tĩnh phải đặt luôn trong UI.

### ✅ Đã chạy được: tên đầy đủ + digest, xác nhận 04/08

Sau khi thêm pin trong UI và deploy bản clone, **22/22 node `Running`**, trong đó
node `VIVA ASR` khai image dạng:

```
registry.hackathon-2.carsky.io/viva/viva-asr@sha256:63c2c56a...
```

Nên câu hỏi *"khai `localhost:5000/...` hay tên đầy đủ"* **đã tự trả lời: tên đầy đủ
pull được**, không cần hỏi BTC. Và nền tảng **chấp nhận dạng digest**, nên khoá
artifact theo digest (thay vì `:latest`) là làm được thật — với `viva-asr` thì digest
khoá luôn cả model đã convert nằm trong image.

Bằng chứng: `evidence/carsky/v7-manifest.txt` + `v7-asr-node-phases.json`.

⚠️ Điều này **không** chứng minh gì về latency: container nằm trên mạng `10.99.0.x`
trong room, và không có đường gửi request vào từ ngoài (Conduit chết, chưa có
`nydus-reach`). Số 439/667 ms vẫn là CPU máy dev.

### ⚠️ Hệ quả cho V2: file backup JSON KHÔNG phải đường khôi phục

Đã kiểm bằng thực nghiệm: lấy **chính** `backend/carsky/blueprint-VIVA-deploy-backup.json`
(do `GET /blueprints/{id}/export` sinh ra) đẩy ngược vào `POST /blueprints/import`
→ **400**, vì chứa pin `ETHERNET` mà import từ chối. Nền tảng **không import lại được
bản export của chính nó**.

`04-KE-HOACH-CAP-NHAT-28-07.md` mô tả quy trình an toàn *"export backup → clone →
chỉ sửa clone"*. Quy trình đó vẫn đúng, nhưng phải hiểu lại vai trò từng thứ:

| Thứ | Vai trò thật |
|---|---|
| `POST /blueprints/{id}/clone` | **Đây mới là đường rollback.** Bản sao server-side, giữ nguyên mọi thứ kể cả pin ETHERNET |
| File JSON export | **Tài liệu và bằng chứng**, không phải ảnh chụp khôi phục được. Vẫn đáng commit (nó ghi lại topology, image, địa chỉ IP), nhưng đừng dựa vào nó để phục hồi lúc gấp |

Nói cách khác: trước khi sửa blueprint, thứ phải làm là **clone**, không phải tải JSON về.

### Câu hỏi gửi BTC

> `POST /blueprints/{id}/batch` (và `/import`) chỉ nhận `pinType` trong
> `VHAL|KUKSA|CAN|LIN|VIDEO|GPIO|GENERIC`, nhưng blueprint do nền tảng sinh ra lại có
> pin `ETHERNET`. Vậy đội thêm Container Node có mạng bằng API kiểu gì, hay bắt buộc
> phải qua UI? Và có chủ đích để `import` không nhận lại được bản `export` không ạ?
