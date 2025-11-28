package com.nhom10.coolmate.category;

import com.nhom10.coolmate.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // --- Mapper ---
    private CategoryDTO mapToDTO(Category category) {
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private Category mapToEntity(CategoryDTO dto) {
        return Category.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
    }

    // --- 1. READ: Lấy tất cả danh mục ---
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // --- 2. READ: Lấy chi tiết danh mục theo ID ---
    public CategoryDTO getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Danh mục không tìm thấy với ID: " + id));
        return mapToDTO(category);
    }

    // --- 3. CREATE: Thêm mới danh mục ---
    @Transactional
    public CategoryDTO createCategory(CategoryDTO dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.getName())) {
            throw new AppException("Tên danh mục đã tồn tại: " + dto.getName());
        }
        Category newCategory = mapToEntity(dto);
        newCategory.setId(null); // Đảm bảo là entity mới
        Category savedCategory = categoryRepository.save(newCategory);
        return mapToDTO(savedCategory);
    }

    // --- 4. UPDATE: Cập nhật danh mục ---
    @Transactional
    public CategoryDTO updateCategory(Integer id, CategoryDTO dto) {
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new AppException("Danh mục không tìm thấy với ID: " + id));

        // Kiểm tra trùng tên (trừ chính nó)
        if (categoryRepository.existsByNameIgnoreCase(dto.getName()) && !existingCategory.getName().equalsIgnoreCase(dto.getName())) {
            throw new AppException("Tên danh mục đã tồn tại: " + dto.getName());
        }

        existingCategory.setName(dto.getName());
        existingCategory.setDescription(dto.getDescription());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return mapToDTO(updatedCategory);
    }

    // --- 5. DELETE: Xóa danh mục ---
    @Transactional
    public void deleteCategory(Integer id) {
        if (!categoryRepository.existsById(id)) {
            throw new AppException("Danh mục không tìm thấy với ID: " + id);
        }
        // TODO: Cần kiểm tra nếu có sản phẩm nào đang dùng danh mục này thì không cho xóa
        categoryRepository.deleteById(id);
    }
}