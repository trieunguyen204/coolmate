package com.nhom10.coolmate.order;

import com.nhom10.coolmate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Tìm kiếm đơn hàng theo trạng thái
    List<Order> findByStatus(OrderStatus status);

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    // Tính tổng doanh thu từ các đơn hàng đã Hoàn thành
    @Query("SELECT SUM(o.total) FROM Order o WHERE o.status = com.nhom10.coolmate.order.OrderStatus.DELIVERED")
    BigDecimal calculateTotalRevenue();

    // [MỚI] Tìm đơn hàng đã hoàn thành sau một thời điểm (Dùng cho biểu đồ doanh thu)
    List<Order> findByStatusAndCreatedAtAfter(OrderStatus status, Timestamp date);
}