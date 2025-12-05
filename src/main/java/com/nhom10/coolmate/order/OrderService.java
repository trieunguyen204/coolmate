package com.nhom10.coolmate.order;

import com.nhom10.coolmate.cart.Cart;
import com.nhom10.coolmate.cart.CartItem;
import com.nhom10.coolmate.cart.CartService;
import com.nhom10.coolmate.exception.AppException;
import com.nhom10.coolmate.product.ProductVariant;
import com.nhom10.coolmate.product.ProductVariantRepository;
import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.user.UserDTO;
import com.nhom10.coolmate.user.UserRepository;
import com.nhom10.coolmate.vouchers.Voucher;
import com.nhom10.coolmate.vouchers.VoucherDTO;
import com.nhom10.coolmate.vouchers.VoucherService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final ProductVariantRepository productVariantRepository;
    private final UserRepository userRepository;
    private final VoucherService voucherService;

    // [MỚI] DTO cho thống kê doanh số sản phẩm
    public record ProductSaleDTO(String productName, String sizeName, Long quantitySold) {}


    // =========================================================================
    // 1. LOGIC ĐẶT HÀNG (CHECKOUT)
    // =========================================================================
    @Transactional
    public Order createOrder(
            String fullName,
            String phone,
            String addressDetail,
            String note,
            String paymentMethod,
            HttpServletRequest request,
            HttpServletResponse response,
            Principal principal,
            String voucherCode,
            BigDecimal discountAmount
    ) {
        // 1. Lấy giỏ hàng
        Cart cart = cartService.getCart(request, response);
        if (cart == null || cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            throw new AppException("Giỏ hàng trống, không thể đặt hàng.");
        }

        // 2. Xác định User
        User user = null;
        if (principal != null) {
            user = userRepository.findByEmail(principal.getName()).orElse(null);
        }

        // 3. Tính tổng tiền tạm tính (Sub Total) và kiểm tra tồn kho
        BigDecimal subTotal = BigDecimal.ZERO;
        for (CartItem ci : cart.getCartItems()) {
            ProductVariant variant = ci.getProductVariant();
            if (variant.getQuantity() < ci.getQuantity()) {
                throw new AppException("Sản phẩm '" + variant.getProduct().getName() + "' không đủ số lượng.");
            }
            BigDecimal itemTotal = ci.getPriceAtTime().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subTotal = subTotal.add(itemTotal);
        }

        // 4. Xử lý Voucher
        Voucher voucher = null;
        BigDecimal finalDiscountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.trim().isEmpty() && finalDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            voucher = voucherService.validateVoucher(voucherCode, subTotal);
            BigDecimal backendCalculatedDiscount = voucherService.calculateDiscount(voucher, subTotal);
            finalDiscountAmount = backendCalculatedDiscount;
            voucherService.increaseVoucherUsedCount(voucher);
        } else {
            finalDiscountAmount = BigDecimal.ZERO;
        }

        // 5. Tính tổng tiền cuối cùng
        BigDecimal finalTotal = subTotal.subtract(finalDiscountAmount);
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }

        // 6. Tạo Mã đơn hàng
        String orderCode = generateOrderCode();

        // 7. Tạo Order
        Order newOrder = Order.builder()
                .orderCode(orderCode)
                .user(user)
                .recipientName(fullName)
                .recipientPhone(phone)
                .deliveryAddress(addressDetail)
                .note(note)
                .paymentMethod(paymentMethod)
                .status(OrderStatus.PENDING)
                .subTotal(subTotal)
                .total(finalTotal)
                .discountAmount(finalDiscountAmount)
                .voucher(voucher)
                .build();

        Order savedOrder = orderRepository.save(newOrder);

        // 8. Xử lý Cart Items và Trừ tồn kho
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem ci : cart.getCartItems()) {
            ProductVariant variant = ci.getProductVariant();
            variant.setQuantity(variant.getQuantity() - ci.getQuantity());
            productVariantRepository.save(variant);

            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productVariant(variant)
                    .quantity(ci.getQuantity())
                    .price(ci.getPriceAtTime())
                    .discountPrice(BigDecimal.ZERO)
                    .build();

            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);

        // 9. Cập nhật Order
        savedOrder.setOrderItems(orderItems);
        orderRepository.save(savedOrder);

        // 10. Xóa giỏ hàng
        cartService.clearCart(request, response);

        return savedOrder;
    }

    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        int randomPart = new Random().nextInt(9000) + 1000;
        return "CM" + datePart + randomPart;
    }

    // =========================================================================
    // [MỚI] 2. LOGIC THỐNG KÊ DASHBOARD
    // =========================================================================

    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    public List<ProductSaleDTO> getTopSellingProducts() {
        List<Object[]> results = orderItemRepository.findTopSellingProductVariants();
        return results.stream()
                .map(result -> new ProductSaleDTO(
                        (String) result[1], // Product Name
                        (String) result[2], // Size Name
                        ((Number) result[3]).longValue() // Quantity Sold
                ))
                .limit(10)
                .collect(Collectors.toList());
    }

    // =========================================================================
    // 3. CÁC HÀM CRUD & MAPPER
    // =========================================================================

    public OrderDTO getOrderDetail(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException("Đơn hàng không tìm thấy với ID: " + id));

        // Load lazy data an toàn
        if(order.getOrderItems() != null) order.getOrderItems().size();

        return mapToDTO(order);
    }

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
        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Integer id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException("Đơn hàng không tìm thấy với ID: " + id));
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new AppException("Không thể thay đổi trạng thái đơn hàng đã hoàn tất/hủy.");
        }
        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    // --- MAPPER QUAN TRỌNG: Đổ dữ liệu từ Entity sang DTO ---
    private OrderDTO mapToDTO(Order order) {
        BigDecimal subTotal = order.getSubTotal() != null ? order.getSubTotal() : BigDecimal.ZERO;

        // --- SỬA LỖI TẠI ĐÂY ---
        // Sử dụng try-catch để bắt lỗi EntityNotFoundException khi User ID = 0 hoặc không tồn tại
        UserDTO userDTO = null;
        try {
            if (order.getUser() != null) {
                userDTO = UserDTO.builder()
                        // Các dòng dưới đây sẽ kích hoạt Proxy tải dữ liệu từ DB
                        .fullName(order.getUser().getFullName())
                        .email(order.getUser().getEmail())
                        .phone(order.getUser().getPhone())
                        .build();
            }
        } catch (EntityNotFoundException e) {
            // Nếu lỗi (do ID = 0 hoặc ID rác), ta coi như User null (Khách vãng lai)
            // Có thể log warning nếu cần: System.err.println("Order " + order.getId() + " has invalid User ID");
            userDTO = null;
        }
        // -----------------------

        // Map Voucher
        VoucherDTO voucherDTO = null;
        if (order.getVoucher() != null) {
            voucherDTO = VoucherDTO.builder().code(order.getVoucher().getCode()).build();
        }

        return OrderDTO.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .paymentMethod(order.getPaymentMethod())
                .user(userDTO)
                .voucher(voucherDTO)
                .discountAmount(order.getDiscountAmount())
                .total(subTotal)
                .finalTotal(order.getTotal())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .orderItems(mapOrderItems(order))
                .build();
    }

    private List<OrderDTO.OrderItemDTO> mapOrderItems(Order order) {
        if (order.getOrderItems() == null) return new ArrayList<>();

        return order.getOrderItems().stream()
                .map(item -> {
                    String productName = "Sản phẩm";
                    String sizeName = "N/A";
                    List<OrderDTO.ImageInfoDTO> images = new ArrayList<>();

                    try {
                        if (item.getProductVariant() != null) {
                            productName = item.getProductVariant().getProduct().getName();
                            sizeName = item.getProductVariant().getSize().getSizeName();
                            images = item.getProductVariant().getProduct().getImages().stream()
                                    .map(img -> OrderDTO.ImageInfoDTO.builder().imageUrl(img.getImageUrl()).build())
                                    .collect(Collectors.toList());
                        }
                    } catch (Exception e) {
                        // Ignore mapping error if product/variant deleted
                    }

                    return OrderDTO.OrderItemDTO.builder()
                            .id(item.getId())
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .discountPrice(item.getDiscountPrice())
                            .product(OrderDTO.ProductInfoDTO.builder()
                                    .name(productName)
                                    .images(images)
                                    .build())
                            .size(OrderDTO.SizeInfoDTO.builder().sizeName(sizeName).build())
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<OrderDTO> getMyOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);

        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }
}