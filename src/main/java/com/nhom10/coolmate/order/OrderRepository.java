package com.nhom10.coolmate.order;

import com.nhom10.coolmate.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Tìm kiếm đơn hàng theo trạng thái
    List<Order> findByStatus(OrderStatus status);

    // Cần Repository cho OrderItem để truy cập chi tiết
    List<Order> findByUserOrderByCreatedAtDesc(User user);
}