package com.nhom10.coolmate.categories;

import com.nhom10.coolmate.exception.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict; // <-- Bổ sung import này
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    // Cache danh sách khi lấy ra
    @Cacheable("categories")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    // 2. THÊM MỚI: Hàm tìm kiếm (Không Cache kết quả tìm kiếm để tránh lãng phí bộ nhớ)
    public List<Category> searchCategories(String keyword) {
        if (keyword != null) {
            return categoryRepository.findByNameContainingIgnoreCase(keyword);
        }
        return categoryRepository.findAll();
    }

    // Khi Thêm mới hoặc Cập nhật -> Xóa Cache cũ để load lại list mới
    @CacheEvict(value = "categories", allEntries = true) // <-- QUAN TRỌNG: Thêm dòng này
    public Category saveCategory(Category category) {
        // 1. Kiểm tra trùng tên (Logic giữ nguyên như cũ)
        Optional<Category> existingCategory = categoryRepository.findByNameIgnoreCase(category.getName());

        if (existingCategory.isPresent()) {
            Category foundCategory = existingCategory.get();
            if (category.getId() == null || !foundCategory.getId().equals(category.getId())) {
                throw new IllegalArgumentException("Tên danh mục đã tồn tại: " + category.getName());
            }
        }

        if (category.getId() == null && category.getStatus() == null) {
            category.setStatus(1);
        }

        return categoryRepository.save(category);
    }

    // Khi Xóa -> Xóa Cache cũ
    @CacheEvict(value = "categories", allEntries = true) // <-- QUAN TRỌNG: Thêm dòng này
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "ID", id));

        if (category.getStatus() != 0) {
            category.setStatus(0);
            categoryRepository.save(category);
        }
    }

    public Category getCategoryById(Integer id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "ID", id));
    }
}