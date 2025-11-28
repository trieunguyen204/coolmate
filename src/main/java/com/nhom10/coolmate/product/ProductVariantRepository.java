package com.nhom10.coolmate.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

    // Tìm kiếm biến thể theo Product ID, Size ID và Color
    Optional<ProductVariant> findByProductIdAndSizeIdAndColor(Integer productId, Integer sizeId, String color);
}