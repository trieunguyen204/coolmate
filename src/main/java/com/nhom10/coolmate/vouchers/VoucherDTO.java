package com.nhom10.coolmate.vouchers;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherDTO {
    private Integer id;

    @NotBlank(message = "Mã Voucher không được để trống")
    private String code;

    private String description;

    @NotNull(message = "Loại giảm giá không được để trống")
    private DiscountType discountType;

    @NotNull(message = "Giá trị giảm giá không được để trống")
    @Min(value = 0, message = "Giá trị giảm không thể âm")
    private BigDecimal discountValue;

    // Giữ nguyên kiểu BigDecimal để khớp với Entity
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate startDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    @FutureOrPresent(message = "Ngày kết thúc không được ở trong quá khứ")
    private LocalDate endDate;

    @Min(value = 1, message = "Số lượng phải lớn hơn hoặc bằng 1")
    private Integer usageLimit;

    private Integer usedCount = 0;

    private Integer status = 1; // 1: Active, 0: Inactive

    // Thêm trường hiển thị
    public boolean isActive() {
        if (this.status == null || this.status == 0) return false;
        if (this.endDate != null && this.endDate.isBefore(LocalDate.now())) return false;
        return true;
    }
}