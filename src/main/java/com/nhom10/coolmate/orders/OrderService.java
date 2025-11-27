package com.nhom10.coolmate.orders;

import com.nhom10.coolmate.exception.ResourceNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    // 1. Lấy danh sách (Đã tối ưu JOIN FETCH User & Address)
    public List<Order> getAllOrders() {
        return orderRepository.findAllOrdersWithDetails();
    }

    public List<Order> getOrdersByStatus(String status) {
        return orderRepository.findByStatusWithDetails(status);
    }

    // 2. Lấy chi tiết 1 đơn (Eager load sâu hơn: OrderItems, Products, Images)
    public Order getOrderById(Integer id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "ID", id));

        // Nạp dữ liệu bảng con để tránh lỗi LazyInit khi mở Modal
        Hibernate.initialize(order.getOrderItems());
        order.getOrderItems().forEach(item -> {
            Hibernate.initialize(item.getProduct());
            if (item.getProduct() != null) {
                Hibernate.initialize(item.getProduct().getImages());
            }
            Hibernate.initialize(item.getSize());
        });

        // Nạp User và Address (dù Repository thường đã nạp, nhưng dòng này đảm bảo 100% cho Modal)
        Hibernate.initialize(order.getUser());
        Hibernate.initialize(order.getAddress());
        Hibernate.initialize(order.getPayment());

        return order;
    }

    // 3. Cập nhật trạng thái (Giữ nguyên logic chặn trạng thái sai)
    public void updateOrderStatus(Integer id, String newStatus) {
        Order order = getOrderById(id);
        String currentStatus = order.getStatus();

        if ("DELIVERED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            throw new IllegalStateException("Đơn hàng đã kết thúc, không thể thay đổi!");
        }

        // Logic kiểm tra luồng trạng thái
        boolean isValid = false;
        if ("PENDING".equals(currentStatus)) {
            if ("SHIPPING".equals(newStatus) || "CANCELLED".equals(newStatus)) isValid = true;
        } else if ("SHIPPING".equals(currentStatus)) {
            if ("DELIVERED".equals(newStatus) || "CANCELLED".equals(newStatus)) isValid = true;
        }

        if (!isValid) {
            throw new IllegalArgumentException("Không thể chuyển từ " + currentStatus + " sang " + newStatus);
        }

        order.setStatus(newStatus);

        // Tự động cập nhật thanh toán nếu là COD và Giao thành công
        if ("DELIVERED".equals(newStatus) && order.getPayment() != null
                && "COD".equals(order.getPayment().getMethod())) {
            order.getPayment().setStatus("PAID");
        }

        orderRepository.save(order);
    }
}