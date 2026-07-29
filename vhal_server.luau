-- ===================================================================
-- VIVA PROJECT - VEHICLE HARDWARE ABSTRACTION LAYER (VHAL) SERVER SCRIPT
-- Ngôn ngữ: Luau (Dùng cho Script Node trên CarSky Platform)
-- Task T2: Script Node Luau VHAL ↔ CAN hai chiều + AI Safety Guard G1
-- Tác giả: Lê Đức Tùng (VHAL Embedded Engineer - Team VIVA)
-- ===================================================================

-- 1. ĐỊNH NGHĨA CÁC VHAL PROPERTY ID (Theo chuẩn Android Automotive OS & BANG_ANH_XA_VHAL_CAN.md)
local VHAL_PROP = {
    HVAC_TEMPERATURE_SET = "0x11600503", -- Nhiệt độ điều hòa (°C) [Message 512: TargetTemperature]
    HVAC_FAN_SPEED       = "0x11400500", -- Mức quạt gió (1 - 5) [Message 512: FanSpeed]
    DOOR_LOCK            = "0x16400b01", -- Trạng thái cửa xe (0: Locked, 1: Unlocked) [Message 768: DoorLockStatus]
    VEHICLE_SPEED        = "0x11600207", -- Tốc độ xe (km/h) [Message 256: VehicleSpeed]
    DRIVER_SEATBELT      = "0x16400b02", -- Dây an toàn tài xế (0: Chưa đeo, 1: Đã đeo) [Message 768: DriverSeatBelt]
    AUDIO_VOLUME         = "0x11400901"  -- Âm lượng buồng lái (0 - 100)
}

-- 2. TRẠNG THÁI XE THỜI GIAN THỰC (Real-time Vehicle State)
local VehicleState = {
    speed = 0.0,            -- Tốc độ xe hiện tại (km/h)
    hvac_temp = 24.0,       -- Nhiệt độ điều hòa (°C)
    fan_speed = 2,          -- Mức gió điều hòa (1-5)
    door_locked = 0,        -- Trạng thái cửa (0: Khóa, 1: Mở)
    driver_seatbelt = 1,    -- Dây an toàn tài xế (0: Tháo, 1: Đeo)
    volume = 50             -- Âm lượng (0-100)
}

-- 3. TẦNG BẢO VỆ AN TOÀN TẤT ĐỊNH (AI SAFETY GUARD - RULE G1)
-- Kiểm tra xem lệnh có an toàn để thực thi dựa trên trạng thái THẬT của xe hay không
function check_safety_guard(command, target_val)
    print(string.format("[SAFETY GUARD] Kiểm tra lệnh: %s | Giá trị: %s | Tốc độ xe: %.1f km/h", command, tostring(target_val), VehicleState.speed))
    
    -- Quy tắc G1.1: CHẶN MỞ CỬA KHI XE ĐANG CHẠY (Speed > 0)
    if command == "OPEN_DOOR" or (command == "SET_DOOR" and target_val == 1) then
        if VehicleState.speed > 0 then
            return false, string.format("CẢNH BÁO AN TOÀN (G1.1): Xe đang chạy ở tốc độ %.1f km/h. TỪ CHỐI MỞ CỬA!", VehicleState.speed)
        end
    end
    
    -- Quy tắc G1.2: GIỚI HẠN NHIỆT ĐỘ ĐIỀU HÒA AN TOÀN (16.0°C - 30.0°C)
    if command == "SET_HVAC_TEMP" then
        if target_val < 16.0 or target_val > 30.0 then
            return false, string.format("CẢNH BÁO AN TOÀN (G1.2): Nhiệt độ yêu cầu (%.1f°C) nằm ngoài dải giới hạn (16.0°C - 30.0°C).", target_val)
        end
    end
    
    -- Quy tắc G1.3: GIỚI HẠN MỨC GIÓ ĐIỀU HÒA (1 - 5)
    if command == "SET_FAN_SPEED" then
        if target_val < 1 or target_val > 5 then
            return false, string.format("CẢNH BÁO (G1.3): Mức gió (%d) ngoài dải cho phép (1-5).", target_val)
        end
    end

    -- Quy tắc G1.4: GIỚI HẠN ÂM LƯỢNG (0 - 100)
    if command == "SET_VOLUME" then
        if target_val < 0 or target_val > 100 then
            return false, "CẢNH BÁO (G1.4): Mức âm lượng ngoài dải 0 - 100."
        end
    end

    return true, "Hợp lệ"
end

-- 4. HÀM GHI VHAL PROPERTY VÀO PIN OUTPUT (Gửi tới Skycraft AAOS HMI)
function set_vhal_property(prop_id, area_id, value)
    print(string.format("[VHAL DISPATCH] Prop: %s | Area: %d | Value: %s", prop_id, area_id, tostring(value)))
    
    -- Ghi vào pin VHAL của Script Node (hỗ trợ cả pins.vhal và Pin.VHAL_OUT)
    if pins and pins.vhal and pins.vhal.write then
        pins.vhal:write({
            property_id = prop_id,
            area_id = area_id,
            value = value,
            timestamp = os.time()
        })
    elseif Pin and Pin.VHAL_OUT then
        Pin.VHAL_OUT:write({
            property_id = prop_id,
            area_id = area_id,
            value = value,
            timestamp = os.time()
        })
    end
end

-- 5. HÀM PHÁT TÍN HIỆU CAN BUS (Outbound CAN: VHAL -> CAN Bus Node)
function publish_can_signal(signal_name, value)
    print(string.format("[CAN DISPATCH] Publish CAN Signal: %s = %s", signal_name, tostring(value)))
    
    -- Tìm pin CAN (can0 hoặc can)
    local can_pin = (pins and (pins.can0 or pins.can)) or (Pin and Pin.CAN_OUT)
    if can_pin and can_pin.db and can_pin.db[signal_name] then
        can_pin.db[signal_name]:publish(value)
    end
end

-- 6. BỘ ĐIỀU PHỐI Ý ĐỊNH AI (AI INTENT DISPATCHER: Intent -> VHAL & CAN)
function process_ai_intent(intent)
    local name = intent.name
    local params = intent.params or {}
    
    print("[VHAL DISPATCHER] Nhận Intent từ AI Engine: " .. tostring(name))
    
    -- A. Skill #2: Climate Control (Nhiệt độ & Quạt gió)
    if name == "SET_HVAC_TEMP" then
        local target_temp = params.temp or 22.0
        local is_safe, msg = check_safety_guard("SET_HVAC_TEMP", target_temp)
        if is_safe then
            VehicleState.hvac_temp = target_temp
            set_vhal_property(VHAL_PROP.HVAC_TEMPERATURE_SET, 0, target_temp)
            publish_can_signal("TargetTemperature", target_temp)
            return { status = "SUCCESS", message = string.format("Đã hạ điều hòa xuống %.1f°C.", target_temp) }
        else
            return { status = "BLOCKED_BY_SAFETY", message = msg }
        end
        
    elseif name == "SET_FAN_SPEED" then
        local speed = params.speed or 2
        local is_safe, msg = check_safety_guard("SET_FAN_SPEED", speed)
        if is_safe then
            VehicleState.fan_speed = speed
            set_vhal_property(VHAL_PROP.HVAC_FAN_SPEED, 0, speed)
            publish_can_signal("FanSpeed", speed)
            return { status = "SUCCESS", message = string.format("Đã chỉnh quạt gió mức %d.", speed) }
        else
            return { status = "BLOCKED_BY_SAFETY", message = msg }
        end

    -- B. Skill Body: Khóa / Mở cửa xe (Door Lock)
    elseif name == "OPEN_DOOR" or name == "CLOSE_DOOR" then
        local door_val = (name == "OPEN_DOOR") and 1 or 0
        local is_safe, msg = check_safety_guard(name, door_val)
        if is_safe then
            VehicleState.door_locked = door_val
            set_vhal_property(VHAL_PROP.DOOR_LOCK, 0, door_val)
            publish_can_signal("DoorLockStatus", door_val)
            return { status = "SUCCESS", message = (door_val == 1) and "Đã mở cửa xe." or "Đã khóa cửa xe." }
        else
            return { status = "BLOCKED_BY_SAFETY", message = msg }
        end

    -- C. Skill Media / System: Âm lượng (Volume)
    elseif name == "SET_VOLUME" then
        local vol = params.volume or 50
        local is_safe, msg = check_safety_guard("SET_VOLUME", vol)
        if is_safe then
            VehicleState.volume = vol
            set_vhal_property(VHAL_PROP.AUDIO_VOLUME, 0, vol)
            return { status = "SUCCESS", message = string.format("Đã chỉnh âm lượng mức %d.", vol) }
        else
            return { status = "BLOCKED_BY_SAFETY", message = msg }
        end

    else
        return { status = "UNKNOWN_INTENT", message = "Ý định chưa được định nghĩa trong VHAL Server." }
    end
end

-- 7. LẮNG NGHE TÍN HIỆU TỪ MẠNG CAN (Inbound CAN: CAN Bus -> VHAL & VehicleState)
-- Tải các callback tự động cập nhật khi nút bấm phần cứng hoặc ECU phát tín hiệu CAN
function init_can_inbound_listeners()
    local can_pin = pins and (pins.can0 or pins.can)
    if not (can_pin and can_pin.db) then
        print("[CAN INBOUND] Cảnh báo: Chưa kết nối DBC CAN Pin. Chạy chế độ Standalone/Simulator.")
        return
    end

    print("[CAN INBOUND] Đã khởi tạo bộ lắng nghe tín hiệu DBC CAN 2 chiều.")

    -- 1. Nhận VehicleSpeed từ EngineECU (Message 256: EngineData)
    if can_pin.db.VehicleSpeed then
        can_pin.db.VehicleSpeed:on_change(function(val)
            VehicleState.speed = val
            print(string.format("[CAN INBOUND] Tốc độ xe cập nhật: %.1f km/h", val))
            set_vhal_property(VHAL_PROP.VEHICLE_SPEED, 0, val)
        end)
    end

    -- 2. Nhận TargetTemperature từ HVACController (Message 512: HVACStatus)
    if can_pin.db.TargetTemperature then
        can_pin.db.TargetTemperature:on_change(function(val)
            VehicleState.hvac_temp = val
            print(string.format("[CAN INBOUND] Nhiệt độ điều hòa từ nút bấm CAN: %.1f°C", val))
            set_vhal_property(VHAL_PROP.HVAC_TEMPERATURE_SET, 0, val)
        end)
    end

    -- 3. Nhận FanSpeed từ HVACController (Message 512: HVACStatus)
    if can_pin.db.FanSpeed then
        can_pin.db.FanSpeed:on_change(function(val)
            VehicleState.fan_speed = val
            print(string.format("[CAN INBOUND] Mức gió từ nút bấm CAN: %d", val))
            set_vhal_property(VHAL_PROP.HVAC_FAN_SPEED, 0, val)
        end)
    end

    -- 4. Nhận DoorLockStatus từ DoorECU (Message 768: DoorStatus)
    if can_pin.db.DoorLockStatus then
        can_pin.db.DoorLockStatus:on_change(function(val)
            VehicleState.door_locked = val
            print(string.format("[CAN INBOUND] Trạng thái khóa cửa từ CAN: %d", val))
            set_vhal_property(VHAL_PROP.DOOR_LOCK, 0, val)
        end)
    end

    -- 5. Nhận DriverSeatBelt từ DoorECU (Message 768: DoorStatus)
    if can_pin.db.DriverSeatBelt then
        can_pin.db.DriverSeatBelt:on_change(function(val)
            VehicleState.driver_seatbelt = val
            print(string.format("[CAN INBOUND] Trạng thái dây an toàn: %d", val))
            set_vhal_property(VHAL_PROP.DRIVER_SEATBELT, 0, val)
        end)
    end
end

-- 8. THAM CHIẾU HÀM KHỞI TẠO VÀ SIMULATOR TELEMETRY TRỰC TIẾP
function on_telemetry_received(data)
    if data.speed ~= nil then
        VehicleState.speed = data.speed
        set_vhal_property(VHAL_PROP.VEHICLE_SPEED, 0, data.speed)
    end
    if data.driver_seatbelt ~= nil then
        VehicleState.driver_seatbelt = data.driver_seatbelt
        set_vhal_property(VHAL_PROP.DRIVER_SEATBELT, 0, data.driver_seatbelt)
    end
    print(string.format("[TELEMETRY] Cập nhật xe: Speed = %.1f km/h | SeatBelt = %d", VehicleState.speed, VehicleState.driver_seatbelt))
end

-- Tự động kích hoạt bộ lắng nghe CAN khi script nạp vào Script Node
init_can_inbound_listeners()
