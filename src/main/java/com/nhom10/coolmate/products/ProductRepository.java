package com.nhom10.coolmate.products;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Tìm kiếm sản phẩm theo tên (để làm chức năng Search)
    List<Product> findByNameContainingIgnoreCase(String name);

    // Lọc theo danh mục (nếu cần sau này)
    List<Product> findByCategoryId(Integer categoryId);
}