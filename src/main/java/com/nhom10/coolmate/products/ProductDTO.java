package com.nhom10.coolmate.products;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProductDTO {
    private Integer id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer discountPercent;
    private Integer categoryId;

    // --- THAY ĐỔI: Hỗ trợ nhiều ảnh ---
    private List<MultipartFile> newImages; // Ảnh mới upload
    private List<ProductImage> existingImages; // Ảnh cũ (để hiển thị khi sửa)

    // Kho hàng
    private List<Integer> sizeIds = new ArrayList<>();
    private List<Integer> stocks = new ArrayList<>();
}