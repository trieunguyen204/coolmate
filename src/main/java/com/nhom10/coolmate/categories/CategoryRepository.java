package com.nhom10.coolmate.categories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Tìm kiếm theo tên (dùng để kiểm tra trùng lặp khi thêm/sửa)
    Optional<Category> findByNameIgnoreCase(String name);

    // Lấy tất cả các danh mục đang hoạt động (status = 1)
    List<Category> findByStatus(Integer status);

    // Tìm kiếm theo tên (tìm kiếm trong trang Admin)
    List<Category> findByNameContainingIgnoreCase(String keyword);
}