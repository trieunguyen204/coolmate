package com.nhom10.coolmate.order;

import com.nhom10.coolmate.user.UserDTO;
import com.nhom10.coolmate.vouchers.VoucherDTO;
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

    // --- CÁC TRƯỜNG MỚI THÊM (Để hiển thị trang thành công) ---
    private String orderCode;       // Mã đơn hàng (VD: CM2025...)
    private String recipientName;   // Tên người nhận
    private String recipientPhone;  // SĐT người nhận
    private String deliveryAddress; // Địa chỉ giao hàng chi tiết
    private String paymentMethod;   // Phương thức thanh toán (COD, VNPAY...)
    // -----------------------------------------------------------

    private UserDTO user;
    private VoucherDTO voucher;
    private BigDecimal discountAmount;

    private BigDecimal total;      // Tổng tiền hàng
    private BigDecimal finalTotal; // Tổng thanh toán (sau khi trừ KM)

    private OrderStatus status;
    private Timestamp createdAt;

    // Chi tiết các sản phẩm trong đơn hàng
    private List<OrderItemDTO> orderItems;

    // --- Nested DTOs ---

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemDTO {
        private Integer id;
        private Integer quantity;
        private BigDecimal price; // Giá tại thời điểm mua
        private BigDecimal discountPrice;

        private ProductInfoDTO product;
        private SizeInfoDTO size;
    }

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SizeInfoDTO {
        private String sizeName;
    }
}