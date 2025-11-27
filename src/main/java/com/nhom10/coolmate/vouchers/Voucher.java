package com.nhom10.coolmate.vouchers;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Mã voucher không được để trống")
    @Column(unique = true, length = 50, nullable = false)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(name = "discount_type")
    private String discountType = "AMOUNT"; // AMOUNT (Trừ tiền trực tiếp) hoặc PERCENT (% giá trị đơn)

    @NotNull(message = "Giá trị giảm không được để trống")
    @Column(name = "discount_value")
    private BigDecimal discountValue; // Số tiền giảm hoặc Số % giảm

    @Column(name = "min_order_amount")
    private BigDecimal minOrderAmount = BigDecimal.ZERO; // Đơn tối thiểu để dùng

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit = 100; // Tổng số lần có thể dùng

    @Column(name = "used_count")
    private Integer usedCount = 0; // Đã dùng bao nhiêu lần

    private Integer status = 1; // 1: Active, 0: Inactive
}