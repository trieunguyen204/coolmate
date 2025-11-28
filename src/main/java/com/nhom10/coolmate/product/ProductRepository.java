package com.nhom10.coolmate.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Tìm kiếm theo tên sản phẩm (không phân biệt hoa thường và tìm kiếm một phần)
    List<Product> findByNameContainingIgnoreCase(String keyword);
}