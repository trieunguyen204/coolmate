package com.nhom10.coolmate.vouchers;

import com.nhom10.coolmate.order.Order;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "vouchers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "code", nullable = false, length = 50, unique = true)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", columnDefinition = "ENUM('PERCENT','AMOUNT') DEFAULT 'AMOUNT'")
    private DiscountType discountType;

    // SỬA: Đổi tên từ discountValue -> discountAmount để khớp với logic tính toán
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "min_order", columnDefinition = "DECIMAL(10,2) DEFAULT 0")
    private BigDecimal minOrder;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "usage_limit", columnDefinition = "INT DEFAULT 1")
    private Integer usageLimit;

    @Column(name = "used_count", columnDefinition = "INT DEFAULT 0")
    private Integer usedCount;

    @Column(name = "status", columnDefinition = "TINYINT DEFAULT 1")
    private Integer status; // 1: Active, 0: Inactive

    // Ánh xạ quan hệ 1-n
    @OneToMany(mappedBy = "voucher", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Order> orders;
}