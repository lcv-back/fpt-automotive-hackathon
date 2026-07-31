# ===================================================================
# VIVA PROJECT - UDS / DTC DIAGNOSTIC SIMULATOR (MÔ PHỎNG CHẨN ĐOÁN LỖI XE)
# Ngôn ngữ: Python (Dùng cho CAN Bus Node / Container Node trên CarSky)
# Tác giả: Embedded / System Engineer (Team VIVA)
# ===================================================================

import json
import time

# 1. TỪ ĐIỂN MÃ LỖI DTC CHUẨN XE HƠI (ISO 15031 / SAE J2012)
DTC_DATABASE = {
    "P0301": {
        "code": "P0301",
        "system": "Động cơ (Powertrain)",
        "description_vn": "Bỏ lửa xy-lanh 1 (Cylinder 1 Misfire Detected)",
        "severity": "CRITICAL",
        "action_vn": "Cần kiểm tra bugi và cuộn đánh lửa xy-lanh 1."
    },
    "P0115": {
        "code": "P0115",
        "system": "Hệ thống làm mát",
        "description_vn": "Lỗi mạch cảm biến nhiệt độ nước làm mát động cơ",
        "severity": "WARNING",
        "action_vn": "Kiểm tra cảm biến ECT và dây dẫn."
    },
    "C0035": {
        "code": "C0035",
        "system": "Khung gầm / Phanh ABS (Chassis)",
        "description_vn": "Lỗi cảm biến tốc độ bánh xe trước bên trái",
        "severity": "WARNING",
        "action_vn": "Kiểm tra cảm biến ABS bánh trước trái."
    },
    "B1200": {
        "code": "B1200",
        "system": "Thân xe / Điện điều hòa (Body)",
        "description_vn": "Lỗi công tắc điều khiển điều hòa không khí",
        "severity": "MINOR",
        "action_vn": "Kiểm tra bảng điều khiển HVAC."
    }
}

class UDSDiagnosticSimulator:
    def __init__(self):
        # Trạng thái mã lỗi hiện tại trên mô phỏng ECU xe (Mặc định xe đang có 1 lỗi P0301)
        self.active_dtcs = ["P0301"]
        print("[UDS SIMULATOR] Khởi tạo mô phỏng chẩn đoán UDS/CAN...")

    def read_dtc_information(self, status_mask=0xFF):
        """
        Giả lập dịch vụ UDS Service 0x19 (ReadDTCInformation)
        """
        print("[UDS SERVICE 0x19] Nhận yêu cầu đọc danh sách mã lỗi DTC...")
        results = []
        for code in self.active_dtcs:
            if code in DTC_DATABASE:
                results.append(DTC_DATABASE[code])
        return results

    def get_dtc_summary_vietnamese(self):
        """
        Tổng hợp danh sách lỗi thành văn bản tiếng Việt tự nhiên cho AI đọc cho tài xế
        (Dùng cho Skill #4: DTC Monitor)
        """
        dtcs = self.read_dtc_information()
        if not dtcs:
            return "Hệ thống xe bình thường, không ghi nhận mã lỗi DTC nào."
        
        summary_lines = [f"Hiện tại xe có {len(dtcs)} mã lỗi cần chú ý:"]
        for idx, dtc in enumerate(dtcs, 1):
            summary_lines.append(f"{idx}. Mã {dtc['code']}: {dtc['description_vn']}. Khuyên dùng: {dtc['action_vn']}")
        
        return " ".join(summary_lines)

    def inject_simulated_fault(self, dtc_code):
        """
        Hàm giả lập inject lỗi vào xe để test demo
        """
        if dtc_code in DTC_DATABASE and dtc_code not in self.active_dtcs:
            self.active_dtcs.append(dtc_code)
            print(f"[TEST BENCH] Đã inject mã lỗi mô phỏng: {dtc_code}")
            return True
        return False

    def clear_simulated_faults(self):
        """
        Giả lập dịch vụ UDS Service 0x14 (ClearDiagnosticInformation)
        """
        self.active_dtcs = []
        print("[UDS SERVICE 0x14] Đã xóa toàn bộ mã lỗi DTC.")
        return "Đã xóa sạch mã lỗi trên ECU."

# --- CHẠY THỬ MÔ PHỎNG (TEST BENCH) ---
if __name__ == "__main__":
    sim = UDSDiagnosticSimulator()
    print("\n--- TEST SKILL #4: ĐỌC LỖI XE KHI TÀI XẾ HỎI 'Xe có lỗi gì không?' ---")
    summary = sim.get_dtc_summary_vietnamese()
    print("AI Phản hồi giọng nói:", summary)
    
    print("\n--- TEST INJECT THÊM LỖI CẢM BIẾN ABS (C0035) ---")
    sim.inject_simulated_fault("C0035")
    summary2 = sim.get_dtc_summary_vietnamese()
    print("AI Phản hồi giọng nói mới:", summary2)
