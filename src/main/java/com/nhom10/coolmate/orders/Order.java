package com.nhom10.coolmate.orders;

import com.nhom10.coolmate.user.User; // Import User Entity
import com.nhom10.coolmate.vouchers.Voucher;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // --- 1. LIÊN KẾT VỚI USER (Người đặt hàng) ---
    // Mối quan hệ N-1: Một User có nhiều Order
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- 2. LIÊN KẾT VỚI ADDRESS (Địa chỉ giao hàng) ---
    // Mối quan hệ N-1: Một Address có thể dùng cho nhiều Order (lịch sử)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @Column(name = "discount_amount")
    private BigDecimal discountAmount;

    private BigDecimal total; // Tổng tiền hàng (chưa giảm)

    @Column(name = "final_total")
    private BigDecimal finalTotal; // Tổng tiền thanh toán (đã trừ KM)

    @Column(length = 30)
    private String status; // PENDING, SHIPPING, DELIVERED, CANCELLED

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // --- 3. DANH SÁCH SẢN PHẨM TRONG ĐƠN ---
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // --- 4. THÔNG TIN THANH TOÁN ---
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    // --- BỔ SUNG LIÊN KẾT VOUCHER ---
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;


}