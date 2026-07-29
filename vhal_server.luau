-- ===================================================================
-- VIVA PROJECT - VEHICLE HARDWARE ABSTRACTION LAYER (VHAL) SERVER SCRIPT
-- Ngôn ngữ: Luau (Dùng cho Script Node trên CarSky Platform)
-- Tác giả: Embedded / System Engineer (Team VIVA)
-- ===================================================================

-- 1. ĐỊNH NGHĨA CÁC VHAL PROPERTY ID (Theo chuẩn Android Automotive OS)
local VHAL_PROP = {
    HVAC_TEMPERATURE_SET = "0x11600503", -- Nhiệt độ điều hòa (°C)
    HVAC_FAN_SPEED       = "0x11400500", -- Mức quạt gió (1 - 5)
    DOOR_LOCK            = "0x16400b01", -- Trạng thái cửa xe (0: Khóa, 1: Mở)
    VEHICLE_SPEED        = "0x11600207", -- Tốc độ xe (km/h)
    GEAR_SELECTION       = "0x11400400", -- Số xe (1: P, 2: R, 3: N, 4: D)
    AUDIO_VOLUME         = "0x11400901"  -- Âm lượng buồng lái (0 - 100)
}

-- 2. TRẠNG THÁI XE THỜI GIAN THỰC (Real-time Vehicle State)
local VehicleState = {
    speed = 0.0,         -- Tốc độ hiện tại (km/h)
    gear = "P",          -- Số hiện tại (P, R, N, D)
    hvac_temp = 24.0,    -- Nhiệt độ hiện tại (°C)
    fan_speed = 2,       -- Tốc độ quạt
    door_open = false,   -- Cửa xe
    volume = 50          -- Âm lượng
}

-- 3. TẦNG BẢO VỆ AN TOÀN TẤT ĐỊNH (AI SAFETY GUARD - RULE G1)
-- Kiểm tra xem lệnh có an toàn để thực thi dựa trên trạng thái THẬT của xe hay không
function check_safety_guard(command, target_val)
    print("[SAFETY GUARD] Kiểm tra lệnh: " .. command .. " với giá trị: " .. tostring(target_val))
    
    -- Quy tắc G1.1: Không cho phép MỞ CỬA khi xe đang chạy (Speed > 0)
    if command == "OPEN_DOOR" or (command == "SET_DOOR" and target_val == 1) then
        if VehicleState.speed > 0 then
            return false, "CẢNH BÁO AN TOÀN: Xe đang chạy (" .. tostring(VehicleState.speed) .. " km/h). Từ chối mở cửa xe!"
        end
    end
    
    -- Quy tắc G1.2: Giới hạn nhiệt độ điều hòa an toàn (16°C - 30°C)
    if command == "SET_HVAC_TEMP" then
        if target_val < 16 or target_val > 30 then
            return false, "Nhiệt độ yêu cầu (" .. tostring(target_val) .. "°C) nằm ngoài dải an toàn (16°C - 30°C)."
        end
    end
    
    -- Quy tắc G1.3: Giới hạn âm lượng tối đa tránh làm tài xế mất tập trung (0 - 100)
    if command == "SET_VOLUME" then
        if target_val < 0 or target_val > 100 then
            return false, "Mức âm lượng không hợp lệ."
        end
    end

    return true, "Hợp lệ"
end

-- 4. HÀM THỰC THI GHI VHAL PROPERTY (Gửi tín hiệu xuống VHAL Pin)
function set_vhal_property(prop_id, area_id, value)
    print(string.format("[VHAL DISPATCH] Setting Prop %s (Area %d) = %s", prop_id, area_id, tostring(value)))
    
    -- Kết nối tới Pin output trên CarSky Platform
    if Pin and Pin.VHAL_OUT then
        Pin.VHAL_OUT:write({
            property_id = prop_id,
            area_id = area_id,
            value = value,
            timestamp = os.time()
        })
    end
end

-- 5. BỘ ĐIỀU PHỐI Ý ĐỊNH (INTENT DISPATCHER)
-- Nhận Intent parsed từ AI Container Node và xử lý
function process_ai_intent(intent)
    -- intent cấu trúc dạng: { name = "SET_HVAC_TEMP", params = { temp = 22 } }
    local name = intent.name
    local params = intent.params or {}
    
    print("[VHAL DISPATCHER] Nhận Intent: " .. tostring(name))
    
    -- A. Skill #2: Climate Control (HVAC)
    if name == "SET_HVAC_TEMP" then
        local target_temp = params.temp or 22.0
        local is_safe, msg = check_safety_guard("SET_HVAC_TEMP", target_temp)
        if is_safe then
            VehicleState.hvac_temp = target_temp
            set_vhal_property(VHAL_PROP.HVAC_TEMPERATURE_SET, 0, target_temp)
            return { status = "SUCCESS", message = "Đã chỉnh điều hòa xuống " .. target_temp .. " độ." }
        else
            return { status = "BLOCKED_BY_SAFETY", message = msg }
        end
        
    elseif name == "SET_FAN_SPEED" then
        local speed = params.speed or 2
        VehicleState.fan_speed = speed
        set_vhal_property(VHAL_PROP.HVAC_FAN_SPEED, 0, speed)
        return { status = "SUCCESS", message = "Đã chỉnh quạt gió mức " .. speed }

    -- B. Skill Mở/Khóa cửa (Door Lock)
    elseif name == "OPEN_DOOR" or name == "CLOSE_DOOR" then
        local door_val = (name == "OPEN_DOOR") and 1 or 0
        local is_safe, msg = check_safety_guard(name, door_val)
        if is_safe then
            VehicleState.door_open = (door_val == 1)
            set_vhal_property(VHAL_PROP.DOOR_LOCK, 0, door_val)
            return { status = "SUCCESS", message = (door_val == 1) ? "Đã mở cửa." : "Đã khóa cửa." }
        else
            return { status = "BLOCKED_BY_SAFETY", message = msg }
        end

    -- C. Điều chỉnh âm lượng (Volume)
    elseif name == "SET_VOLUME" then
        local vol = params.volume or 50
        local is_safe, msg = check_safety_guard("SET_VOLUME", vol)
        if is_safe then
            VehicleState.volume = vol
            set_vhal_property(VHAL_PROP.AUDIO_VOLUME, 0, vol)
            return { status = "SUCCESS", message = "Đã chỉnh âm lượng mức " .. vol }
        else
            return { status = "BLOCKED_BY_SAFETY", message = msg }
        end

    else
        return { status = "UNKNOWN_INTENT", message = "Ý định chưa được định nghĩa trong VHAL." }
    end
end

-- 6. HÀM CẬP NHẬT TỐC ĐỘ VÀ TRẠNG THÁI XE TỪ SIMULATOR / CAN BUS
function on_telemetry_received(data)
    if data.speed ~= nil then
        VehicleState.speed = data.speed
    end
    if data.gear ~= nil then
        VehicleState.gear = data.gear
    end
    print(string.format("[TELEMETRY] Cập nhật xe: Speed = %.1f km/h | Gear = %s", VehicleState.speed, VehicleState.gear))
end
