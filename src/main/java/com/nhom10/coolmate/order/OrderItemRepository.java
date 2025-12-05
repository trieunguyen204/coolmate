package com.nhom10.coolmate.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

    // [MỚI] Lấy danh sách Top sản phẩm bán chạy (đã giao)
    @Query("SELECT oi.productVariant.id, oi.productVariant.product.name, oi.productVariant.size.sizeName, SUM(oi.quantity) " +
            "FROM OrderItem oi " +
            "WHERE oi.order.status = com.nhom10.coolmate.order.OrderStatus.DELIVERED " +
            "GROUP BY oi.productVariant.id, oi.productVariant.product.name, oi.productVariant.size.sizeName " +
            "ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProductVariants();
}