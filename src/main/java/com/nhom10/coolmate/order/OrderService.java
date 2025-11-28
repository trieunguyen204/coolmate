package com.nhom10.coolmate.order;

import com.nhom10.coolmate.exception.AppException;
import com.nhom10.coolmate.payment.Payment;
import com.nhom10.coolmate.product.ProductImage;
import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.user.UserDTO;
import com.nhom10.coolmate.vouchers.VoucherDTO;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    // Yêu cầu: Bạn phải đảm bảo Entity Payment.java có trường 'status' và 'method'
    private OrderDTO.PaymentDTO mapPayment(Order order) {
        if (order.getPayment() != null) {
            Payment payment = order.getPayment();

            String paymentStatusName = payment.getStatus() != null ? payment.getStatus().name() : "N/A";
            String paymentMethod = payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : "N/A";

            return OrderDTO.PaymentDTO.builder()
                    .method(paymentMethod)
                    .status(paymentStatusName)
                    .build();
        }
        return null;
    }

    private OrderDTO.ShippingAddressDTO mapAddress(Order order) {
        // --- LOGIC LẤY ĐỊA CHỈ TỪ ORDER ENTITY ---
        User user = null;
        try {
            user = order.getUser();
            user.getFullName(); // Buộc truy cập
        } catch (EntityNotFoundException | NullPointerException e) {
            user = null;
        }

        return OrderDTO.ShippingAddressDTO.builder()
                .fullName(user != null ? user.getFullName() : "N/A")
                .phone(user != null ? user.getPhone() : "N/A")
                .address(order.getShippingAddress()) // <<< LẤY TRỰC TIẾP TỪ TRƯỜNG SHIPPING_ADDRESS
                .build();
    }

    private OrderDTO mapToDTO(Order order) {
        // Tạm tính total (trước discount)
        BigDecimal total = order.getOrderItems().stream()
                .map(item -> item.getDiscountPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // --- XỬ LÝ USER AN TOÀN TRƯỚC ---
        User user = null;
        try {
            user = order.getUser();
            user.getFullName();
        } catch (EntityNotFoundException | NullPointerException e) {
            user = null;
        }

        OrderDTO dto = OrderDTO.builder()
                .id(order.getId())
                .user(user != null ? UserDTO.builder().fullName(user.getFullName()).email(user.getEmail()).build() : null)
                .voucher(order.getVoucher() != null ? VoucherDTO.builder().code(order.getVoucher().getCode()).build() : null)
                .discountAmount(order.getDiscountAmount())
                .total(total)
                .finalTotal(order.getTotal())
                .status(order.getStatus())
                .shippingAddress(order.getShippingAddress()) // <<< LẤY ĐỊA CHỈ
                .createdAt(order.getCreatedAt())
                .payment(mapPayment(order))
                .address(mapAddress(order))
                .build();

        // --- MAPPING ORDER ITEMS AN TO TOÀN ---
        dto.setOrderItems(order.getOrderItems().stream()
                .map(item -> {
                    String productName = "Sản phẩm bị lỗi";
                    List<OrderDTO.ImageInfoDTO> images = new ArrayList<>();
                    String sizeName = "N/A";

                    try {
                        productName = item.getProductVariant().getProduct().getName();
                        sizeName = item.getProductVariant().getSize().getSizeName();
                        images = item.getProductVariant().getProduct().getImages().stream()
                                .map(img -> OrderDTO.ImageInfoDTO.builder().imageUrl(img.getImageUrl()).build())
                                .collect(Collectors.toList());
                    } catch (EntityNotFoundException | NullPointerException e) {
                        // Bỏ qua lỗi dữ liệu hỏng
                    }

                    return OrderDTO.OrderItemDTO.builder()
                            .id(item.getId())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .discountPrice(item.getDiscountPrice())
                            .size(OrderDTO.SizeInfoDTO.builder().sizeName(sizeName).build())
                            .product(OrderDTO.ProductInfoDTO.builder()
                                    .name(productName)
                                    .images(images)
                                    .build())
                            .build();
                })
                .collect(Collectors.toList()));

        return dto;
    }

    // --- CRUD LOGIC ---

    // 1. READ: Lấy tất cả Đơn hàng (hoặc theo Status)
    public List<OrderDTO> getAllOrders(String status) {
        List<Order> orders;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                OrderStatus os = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatus(os);
            } catch (IllegalArgumentException e) {
                orders = orderRepository.findAll();
            }
        } else {
            orders = orderRepository.findAll();
        }

        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // 2. READ: Lấy chi tiết đơn hàng theo ID
    public OrderDTO getOrderDetail(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException("Đơn hàng không tìm thấy với ID: " + id));

        // Buộc load lazy collections
        order.getOrderItems().size();

        return mapToDTO(order);
    }

    // 3. UPDATE: Cập nhật trạng thái đơn hàng
    @Transactional
    public void updateOrderStatus(Integer id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException("Đơn hàng không tìm thấy với ID: " + id));

        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new AppException("Không thể thay đổi trạng thái của đơn hàng đã Hoàn thành hoặc đã Hủy.");
        }

        // Tùy chọn: Thêm logic kiểm tra quy trình (chưa được yêu cầu chi tiết)

        order.setStatus(newStatus);
        orderRepository.save(order);
    }
}