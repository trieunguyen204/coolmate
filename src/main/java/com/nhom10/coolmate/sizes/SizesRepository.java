package com.nhom10.coolmate.sizes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SizesRepository extends JpaRepository<Sizes, Integer> {
    // Tìm kiếm theo tên (dùng class Sizes)
    Optional<Sizes> findBySizeNameIgnoreCase(String sizeName);
}