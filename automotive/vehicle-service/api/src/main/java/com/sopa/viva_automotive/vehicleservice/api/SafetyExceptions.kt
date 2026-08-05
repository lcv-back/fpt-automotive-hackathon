package com.sopa.viva_automotive.vehicleservice.api

/**
 * Lệnh bị tầng an toàn chặn. Mang theo **mã luật** để log ra
 * `Deny:<RULE_ID>` đúng grammar `03-contracts.md` §1.2 — đó là khoá join của
 * bảng ablation N4b, không phải để cho đẹp.
 *
 * ## Vì sao ở `api` chứ không ở `impl`
 *
 * `GuardedVehicleRepository` (module `impl`) ném exception này, nhưng nơi
 * *phải bắt* nó là `VoiceTurnReport.verdictFor` ở `feature:voice` — module chỉ
 * phụ thuộc `vehicle-service:api`. Để ở `impl` thì đường voice không bắt được
 * theo kiểu, mọi lệnh bị chặn rơi vào nhánh `error != null` và ghi ra
 * `Error:exec_done`. Khi đó A1 đếm `Deny:G1_SPEED_LOCK` bằng 0 ở **cả hai**
 * cột và bảng ablation trở nên vô nghĩa.
 */
class SafetyDeniedException(
    val rule: String,
    val reasonVi: String,
    val suggestion: String? = null,
) : IllegalStateException(reasonVi)

/**
 * Lệnh hợp lệ nhưng phải hỏi lại tài xế trước khi thực hiện.
 *
 * Cùng lý do đặt ở `api` như [SafetyDeniedException]: verdict `Confirm:<rule>`
 * chỉ sinh được nếu đường voice bắt được exception này theo kiểu.
 */
class SafetyConfirmationRequiredException(
    val rule: String,
    val questionVi: String,
) : IllegalStateException(questionVi)
