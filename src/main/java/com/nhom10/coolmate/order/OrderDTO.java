package com.nhom10.coolmate.order;

import com.nhom10.coolmate.user.UserDTO; // Dùng lại DTO của User
import com.nhom10.coolmate.vouchers.VoucherDTO; // Dùng lại DTO của Voucher
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Integer id;
    private UserDTO user; // Thông tin người dùng đặt
    private VoucherDTO voucher;
    private BigDecimal discountAmount;
    private BigDecimal total; // Tổng tiền sản phẩm (trước khi trừ discount)
    private BigDecimal finalTotal; // Tổng tiền cuối cùng (sau khi trừ discount)
    private OrderStatus status;
    private String shippingAddress;
    private Timestamp createdAt;

    // Chi tiết các sản phẩm trong đơn hàng
    private List<OrderItemDTO> orderItems;

    // Thông tin thanh toán giả định (Payment DTO/Entity chưa được cung cấp)
    private PaymentDTO payment;

    // Giả định có thông tin địa chỉ đầy đủ (address Entity chưa được cung cấp)
    private ShippingAddressDTO address;

    // Tính toán tổng tiền cuối cùng
    public BigDecimal getFinalTotal() {
        if (total == null) return BigDecimal.ZERO;
        if (discountAmount == null) discountAmount = BigDecimal.ZERO;

        BigDecimal calculatedTotal = total.subtract(discountAmount);
        return calculatedTotal.compareTo(BigDecimal.ZERO) > 0 ? calculatedTotal : BigDecimal.ZERO;
    }

    // Nested DTO cho Order Item (tách riêng)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Integer id;
        private Integer quantity;
        private BigDecimal price; // Giá gốc
        private BigDecimal discountPrice; // Giá sau giảm giá (của sản phẩm đó)

        // Thông tin Sản phẩm (Giả định)
        private ProductInfoDTO product;
        private SizeInfoDTO size;
    }

    // Nested DTO cho Thông tin Sản phẩm đơn giản
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductInfoDTO {
        private String name;
        private List<ImageInfoDTO> images;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageInfoDTO {
        private String imageUrl;
    }

    // Nested DTO cho Size đơn giản
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeInfoDTO {
        private String sizeName;
    }

    // Nested DTO cho Payment
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentDTO {
        private String method; // Ví dụ: COD, VNPAY
        private String status; // Ví dụ: PAID, UNPAID (string)
    }

    // Nested DTO cho Shipping Address (Giả định)
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressDTO {
        private String fullName;
        private String phone;
        private String address;
    }
}