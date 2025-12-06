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
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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

    // --- DTO RECORDS CHO DASHBOARD ---
    public record ProductSaleDTO(String productName, String sizeName, Long quantitySold) {}
    public record RevenueChartDTO(String label, BigDecimal value) {}


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
            // Kiểm tra tồn kho lần cuối
            if (variant.getQuantity() < ci.getQuantity()) {
                throw new AppException("Sản phẩm '" + variant.getProduct().getName() +
                        "' (Size: " + variant.getSize().getSizeName() +
                        ", Màu: " + variant.getColor() + ") không đủ số lượng.");
            }
            BigDecimal itemTotal = ci.getPriceAtTime().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subTotal = subTotal.add(itemTotal);
        }

        // 4. Xử lý Voucher (Validate lại ở Backend để bảo mật)
        Voucher voucher = null;
        BigDecimal finalDiscountAmount = discountAmount != null ? discountAmount : BigDecimal.ZERO;

        if (voucherCode != null && !voucherCode.trim().isEmpty() && finalDiscountAmount.compareTo(BigDecimal.ZERO) > 0) {
            // Kiểm tra voucher có hợp lệ với tổng tiền này không
            voucher = voucherService.validateVoucher(voucherCode, subTotal);

            // Tính toán lại số tiền giảm giá ở server để tránh client gian lận
            BigDecimal backendCalculatedDiscount = voucherService.calculateDiscount(voucher, subTotal);
            finalDiscountAmount = backendCalculatedDiscount;

            // Tăng số lượt sử dụng voucher
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

        // 7. Lưu Order (Cha)
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
                .total(finalTotal) // Đây là final total
                .discountAmount(finalDiscountAmount)
                .voucher(voucher)
                .build();

        Order savedOrder = orderRepository.save(newOrder);

        // 8. Xử lý Cart Items -> Order Items và Trừ tồn kho
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem ci : cart.getCartItems()) {
            ProductVariant variant = ci.getProductVariant();

            // Trừ tồn kho
            variant.setQuantity(variant.getQuantity() - ci.getQuantity());
            productVariantRepository.save(variant);

            // Tạo OrderItem
            OrderItem orderItem = OrderItem.builder()
                    .order(savedOrder)
                    .productVariant(variant)
                    .quantity(ci.getQuantity())
                    .price(ci.getPriceAtTime())
                    .discountPrice(BigDecimal.ZERO) // Hiện tại chưa lưu giảm giá trên từng item
                    .build();

            orderItems.add(orderItem);
        }

        orderItemRepository.saveAll(orderItems);

        // 9. Cập nhật lại list items vào Order (để return đủ data nếu cần)
        savedOrder.setOrderItems(orderItems);
        orderRepository.save(savedOrder); // Save lại để đồng bộ

        // 10. Xóa sạch giỏ hàng
        cartService.clearCart(request, response);

        return savedOrder;
    }

    private String generateOrderCode() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));
        int randomPart = new Random().nextInt(9000) + 1000;
        return "CM" + datePart + randomPart;
    }

    // =========================================================================
    // 2. LOGIC THỐNG KÊ DASHBOARD (FULL)
    // =========================================================================

    // 2.1. Tổng doanh thu (Chỉ tính đơn đã hoàn thành)
    public BigDecimal getTotalRevenue() {
        BigDecimal revenue = orderRepository.calculateTotalRevenue();
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    // 2.2. Top 10 Sản phẩm bán chạy
    public List<ProductSaleDTO> getTopSellingProducts() {
        List<Object[]> results = orderItemRepository.findTopSellingProductVariants();
        return results.stream()
                .map(result -> new ProductSaleDTO(
                        (String) result[1], // Tên SP
                        (String) result[2], // Size
                        ((Number) result[3]).longValue() // Số lượng bán
                ))
                .limit(10)
                .collect(Collectors.toList());
    }

    // 2.3. Doanh thu 6 tháng gần nhất (Cho biểu đồ)
    public List<RevenueChartDTO> getRevenueLast6Months() {
        List<RevenueChartDTO> chartData = new ArrayList<>();

        // A. Xác định mốc thời gian (Mùng 1 của 5 tháng trước)
        LocalDate now = LocalDate.now();
        LocalDate sixMonthsAgo = now.minusMonths(5).withDayOfMonth(1);
        Timestamp startDate = Timestamp.valueOf(sixMonthsAgo.atStartOfDay());

        // B. Lấy danh sách đơn hàng đã Giao thành công từ mốc thời gian đó
        List<Order> orders = orderRepository.findByStatusAndCreatedAtAfter(OrderStatus.DELIVERED, startDate);

        // C. Khởi tạo Map chứa sẵn 6 tháng (Key="MM/yyyy", Value=0) để đảm bảo đủ cột
        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/yyyy");

        for (int i = 0; i < 6; i++) {
            String key = sixMonthsAgo.plusMonths(i).format(formatter);
            revenueMap.put(key, BigDecimal.ZERO);
        }

        // D. Cộng dồn doanh thu vào Map
        for (Order order : orders) {
            if (order.getCreatedAt() != null) {
                String monthKey = order.getCreatedAt().toLocalDateTime().format(formatter);
                // Chỉ cộng nếu key nằm trong 6 tháng (đề phòng sai lệch nhỏ)
                if (revenueMap.containsKey(monthKey)) {
                    revenueMap.put(monthKey, revenueMap.get(monthKey).add(order.getTotal()));
                }
            }
        }

        // E. Chuyển Map thành List DTO trả về cho Controller
        for (Map.Entry<String, BigDecimal> entry : revenueMap.entrySet()) {
            chartData.add(new RevenueChartDTO(entry.getKey(), entry.getValue()));
        }

        return chartData;
    }

    // =========================================================================
    // 3. CÁC HÀM CRUD & MAPPER
    // =========================================================================

    public OrderDTO getOrderDetail(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException("Đơn hàng không tìm thấy với ID: " + id));

        // Load lazy relationship (nếu cần thiết, dù mapper đã xử lý)
        if(order.getOrderItems() != null) order.getOrderItems().size();

        return mapToDTO(order);
    }

    // Lấy danh sách đơn hàng của User (Lịch sử mua hàng)
    public List<OrderDTO> getMyOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));

        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);

        return orders.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Lấy tất cả đơn hàng (Admin quản lý)
    public List<OrderDTO> getAllOrders(String status) {
        List<Order> orders;
        if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("ALL")) {
            try {
                OrderStatus os = OrderStatus.valueOf(status.toUpperCase());
                orders = orderRepository.findByStatus(os);
            } catch (IllegalArgumentException e) {
                // Nếu status rác -> lấy tất cả
                orders = orderRepository.findAll();
            }
        } else {
            orders = orderRepository.findAll();
        }

        // Sắp xếp đơn mới nhất lên đầu (nếu DB chưa sort)
        // orders.sort(Comparator.comparing(Order::getCreatedAt).reversed());

        return orders.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(Integer id, OrderStatus newStatus) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new AppException("Đơn hàng không tìm thấy với ID: " + id));

        // Logic nghiệp vụ: Không cho sửa đơn đã xong/hủy (tuỳ nhu cầu)
        if (order.getStatus() == OrderStatus.DELIVERED || order.getStatus() == OrderStatus.CANCELLED) {
            // Có thể mở ra nếu muốn cho phép Admin sửa lại đơn đã giao
            throw new AppException("Không thể thay đổi trạng thái đơn hàng đã hoàn tất hoặc đã hủy.");
        }

        order.setStatus(newStatus);
        orderRepository.save(order);
    }

    // --- MAPPER: Chuyển Entity sang DTO ---
    private OrderDTO mapToDTO(Order order) {
        BigDecimal subTotal = order.getSubTotal() != null ? order.getSubTotal() : BigDecimal.ZERO;

        // Xử lý User an toàn (tránh lỗi Lazy Loading hoặc ID rác)
        UserDTO userDTO = null;
        try {
            if (order.getUser() != null) {
                userDTO = UserDTO.builder()
                        .fullName(order.getUser().getFullName())
                        .email(order.getUser().getEmail())
                        .phone(order.getUser().getPhone())
                        .build();
            }
        } catch (EntityNotFoundException | NullPointerException e) {
            // User có thể đã bị xóa hoặc lỗi tham chiếu -> Trả về null
            userDTO = null;
        }

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
                .orderItems(mapOrderItems(order)) // Gọi hàm map chi tiết
                .build();
    }

    private List<OrderDTO.OrderItemDTO> mapOrderItems(Order order) {
        if (order.getOrderItems() == null) return new ArrayList<>();

        return order.getOrderItems().stream()
                .map(item -> {
                    String productName = "Sản phẩm (Đã ẩn)";
                    String sizeName = "N/A";
                    String color = "N/A";
                    List<OrderDTO.ImageInfoDTO> images = new ArrayList<>();

                    try {
                        if (item.getProductVariant() != null) {
                            productName = item.getProductVariant().getProduct().getName();
                            sizeName = item.getProductVariant().getSize().getSizeName();
                            color = item.getProductVariant().getColor();

                            // Lấy ảnh sản phẩm
                            images = item.getProductVariant().getProduct().getImages().stream()
                                    .map(img -> OrderDTO.ImageInfoDTO.builder().imageUrl(img.getImageUrl()).build())
                                    .collect(Collectors.toList());
                        }
                    } catch (Exception e) {
                        // Bỏ qua lỗi nếu sản phẩm gốc bị xóa
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
                            .color(color) // Trả về màu sắc
                            .build();
                })
                .collect(Collectors.toList());
    }
}