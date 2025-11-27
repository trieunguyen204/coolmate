package com.nhom10.coolmate.orders;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    // Tối ưu: Lấy đơn hàng KÈM THEO User và Address ngay trong 1 câu lệnh
    @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.address ORDER BY o.createdAt DESC")
    List<Order> findAllOrdersWithDetails();

    // Lọc theo trạng thái (Cũng JOIN FETCH để tối ưu)
    @Query("SELECT o FROM Order o JOIN FETCH o.user JOIN FETCH o.address WHERE o.status = :status ORDER BY o.createdAt DESC")
    List<Order> findByStatusWithDetails(String status);
}