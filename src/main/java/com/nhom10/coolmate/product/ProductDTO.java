package com.nhom10.coolmate.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDTO {
    private Integer id;

    @NotBlank(message = "Tên sản phẩm không được để trống")
    private String name;

    private String description;

    @NotNull(message = "Giá không được để trống")
    @DecimalMin(value = "0.01", message = "Giá phải lớn hơn 0")
    private BigDecimal price;

    @Min(value = 0, message = "Giảm giá phải lớn hơn hoặc bằng 0")
    private Integer discountPercent = 0;

    @NotBlank(message = "Chất liệu không được để trống")
    private String material;

    @NotNull(message = "Danh mục không được để trống")
    private Integer categoryId;

    // --- SỬA LỖI HTML: Thêm tên danh mục để hiển thị trong bảng ---
    private String categoryName; // <--- TRƯỜNG NÀY CẦN THIẾT CHO products.html

    // --- Dữ liệu Biến thể MỚI ---
    private List<VariantInputDTO> variantInputs = new ArrayList<>();

    // --- Xử lý File Upload ---
    private List<MultipartFile> newImages = new ArrayList<>();

    // --- Dữ liệu hiện tại (Chỉ dùng để hiển thị trong modal) ---
    private List<ProductImage> existingImages = new ArrayList<>();

    // --- Dữ liệu tổng hợp (Chỉ dùng để hiển thị trong bảng list) ---
    private BigDecimal discountPrice;

    // List các biến thể (dùng cho việc hiển thị bảng/tải dữ liệu)
    private List<ProductSizeColorStockDTO> productVariants = new ArrayList<>();


    // --- NESTED DTO CHO MỖI DÒNG INPUT (FORM): VariantInputDTO ---
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VariantInputDTO {
        private Integer variantId;
        @NotNull(message = "Size không được để trống")
        private Integer sizeId;
        @NotBlank(message = "Màu sắc không được để trống")
        private String color;
        @Min(value = 0, message = "Tồn kho không thể âm")
        private Integer stock;
    }

    // Nested DTO cho hiển thị tồn kho trong bảng
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSizeColorStockDTO {
        private Integer variantId;
        private String sizeName;
        private String color;
        private Integer stock;
    }
}