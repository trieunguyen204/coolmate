package com.nhom10.coolmate.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // Tìm kiếm theo tên sản phẩm (không phân biệt hoa thường và tìm kiếm một phần)
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // Phương thức mới: Lấy top 8 sản phẩm mới nhất
    List<Product> findTop8ByOrderByCreatedAtDesc();

    // Tìm sản phẩm theo Category ID
    List<Product> findByCategoryId(Integer categoryId);

    // Nếu bạn cần tìm theo Category Id và còn hàng:
    List<Product> findByCategoryIdAndVariants_QuantityGreaterThan(Integer categoryId, Integer quantity);



    @Query("SELECT p FROM Product p WHERE " +
            "(:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
            // THÊM: Logic sắp xếp động
            "ORDER BY " +
            "CASE WHEN :sortOrder = 'price_asc' THEN p.price END ASC, " +
            "CASE WHEN :sortOrder = 'price_desc' THEN p.price END DESC, " +
            "CASE WHEN :sortOrder = 'createdAt_desc' OR :sortOrder IS NULL THEN p.createdAt END DESC")
    List<Product> findByKeywordAndPriceRange(
            @Param("keyword") String keyword,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("sortOrder") String sortOrder // THÊM THAM SỐ
    );
}