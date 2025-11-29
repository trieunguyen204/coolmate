package com.nhom10.coolmate.product;

import com.nhom10.coolmate.category.Category;
import com.nhom10.coolmate.category.CategoryRepository;
import com.nhom10.coolmate.sizes.Sizes;
import com.nhom10.coolmate.sizes.SizesRepository;
import com.nhom10.coolmate.util.FileUploadHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.nhom10.coolmate.exception.AppException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode; // Đã thêm
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final CategoryRepository categoryRepository;
    private final SizesRepository sizesRepository;

    // --- Mapper & Calculation ---

    private BigDecimal calculateDiscountPrice(BigDecimal price, Integer discountPercent) {
        if (discountPercent == null || discountPercent <= 0) {
            return price;
        }
        // Đã sửa: dùng RoundingMode.HALF_UP
        BigDecimal discountFactor = BigDecimal.valueOf(100 - discountPercent.doubleValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return price.multiply(discountFactor).setScale(0, RoundingMode.HALF_UP); // Set Scale 0 để làm tròn giá bán
    }

    private ProductDTO mapToDTO(Product product) {
        BigDecimal discountPrice = calculateDiscountPrice(product.getPrice(), product.getDiscountPercent());
        // Giá gốc được dùng làm oldPrice khi có giảm giá
        BigDecimal oldPrice = (product.getDiscountPercent() != null && product.getDiscountPercent() > 0) ? product.getPrice() : null;

        // Lấy URL ảnh đầu tiên làm ảnh đại diện
        String mainImageUrl = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().get(0).getImageUrl()
                : "/images/placeholder_product.jpg"; // Thêm ảnh placeholder nếu không có

        // --- KHẮC PHỤC LỖI CATEGORY ID 0 ---
        String categoryName = "-";
        Integer categoryId = null;

        try {
            // Sử dụng try-catch khi truy cập Category Entity để xử lý lỗi ID 0
            if (product.getCategory() != null) {
                categoryName = product.getCategory().getName();
                categoryId = product.getCategory().getId();
            }
        } catch (EntityNotFoundException | NullPointerException e) {
            categoryName = "Danh mục bị lỗi";
        }

        List<ProductDTO.ProductSizeColorStockDTO> variantsDto = product.getVariants().stream()
                .filter(v -> v.getSize() != null)
                .map(v -> ProductDTO.ProductSizeColorStockDTO.builder()
                        .variantId(v.getId())
                        .sizeName(v.getSize().getSizeName())
                        .color(v.getColor())
                        .stock(v.getQuantity())
                        .build())
                .collect(Collectors.toList());

        // CHỈNH SỬA DTO: Thêm trường currentPrice và oldPrice
        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice()) // Đây là giá gốc
                .discountPercent(product.getDiscountPercent())
                .material(product.getMaterial())
                .categoryId(categoryId)
                .categoryName(categoryName)
                // CÁC TRƯỜNG MỚI ĐỂ HIỂN THỊ Ở CLIENT
                .discountPrice(discountPrice) // Giá sau chiết khấu (Giá mới)
                .currentPrice(discountPrice) // Gán discountPrice vào currentPrice cho tiện dùng trong home.html
                .oldPrice(oldPrice) // Giá gốc (nếu có chiết khấu)
                .imageUrl(mainImageUrl) // URL ảnh đại diện
                .productVariants(variantsDto)
                .existingImages(product.getImages())
                .build();
    }

    // ==========================================================
    // CLIENT: getFeaturedProducts
    // ==========================================================
    /**
     * Lấy danh sách sản phẩm nổi bật (Top 8 sản phẩm mới nhất).
     * @return List<ProductDTO>
     */
    public List<ProductDTO> getFeaturedProducts() {
        List<Product> products = productRepository.findTop8ByOrderByCreatedAtDesc();

        // Thêm logic lọc: Chỉ hiển thị sản phẩm có ít nhất 1 biến thể có tồn kho > 0
        return products.stream()
                .filter(p -> p.getVariants() != null && p.getVariants().stream().anyMatch(v -> v.getQuantity() > 0))
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ==========================================================
    // READ: getAllProducts (ADMIN)
    // ==========================================================
    public List<ProductDTO> getAllProducts(String keyword) {
        List<Product> products;
        if (keyword != null && !keyword.trim().isEmpty()) {
            products = productRepository.findByNameContainingIgnoreCase(keyword);
        } else {
            products = productRepository.findAll();
        }

        return products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ==========================================================
    // READ: getProductById (ADMIN)
    // ==========================================================
    public ProductDTO getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException("Sản phẩm không tìm thấy: " + id));

        return mapToDTO(product);
    }


    // --- CRUD Logic (ADMIN) ---

    @Transactional
    public void saveProduct(@Valid ProductDTO dto) {
        // 1. Validate Category
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new AppException("Danh mục không tồn tại."));

        // 2. Validation cơ bản cho Material (đã chuyển sang @NotBlank trong DTO)

        // 3. Map DTO to Entity (CREATE / UPDATE)
        Product product;
        if (dto.getId() != null) {
            product = productRepository.findById(dto.getId())
                    .orElseThrow(() -> new AppException("Sản phẩm không tồn tại để cập nhật."));
        } else {
            product = new Product();
            product.setVariants(new ArrayList<>());
            product.setImages(new ArrayList<>());
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPercent(dto.getDiscountPercent());
        product.setMaterial(dto.getMaterial());
        product.setCategory(category);

        // 4. Save Product (Lưu trước để lấy ID)
        Product savedProduct = productRepository.save(product);

        // 5. Handle Variants (Sử dụng VariantInputDTO)
        if (dto.getVariantInputs() != null && !dto.getVariantInputs().isEmpty()) {

            // Dùng Set để kiểm tra trùng lặp (SizeId + Color) ngay trong input
            java.util.Set<String> uniqueVariants = new java.util.HashSet<>();

            for (ProductDTO.VariantInputDTO input : dto.getVariantInputs()) {

                // 5a. Kiểm tra trùng lặp trong input
                String uniqueKey = input.getSizeId() + "_" + input.getColor().trim().toLowerCase();
                if (!uniqueVariants.add(uniqueKey)) {
                    throw new AppException("Biến thể trùng lặp được phát hiện trong dữ liệu nhập: Size ID " + input.getSizeId() + " và Màu " + input.getColor());
                }

                // 5b. Lấy Size Entity
                Sizes size = sizesRepository.findById(input.getSizeId())
                        .orElseThrow(() -> new AppException("Size không tồn tại: " + input.getSizeId()));

                // 5c. Tìm hoặc Tạo ProductVariant
                // Tìm theo variantId (nếu có) hoặc theo ProductId + SizeId + Color
                Optional<ProductVariant> existingVariant = Optional.empty();
                if (input.getVariantId() != null) {
                    existingVariant = variantRepository.findById(input.getVariantId());
                } else {
                    existingVariant = variantRepository
                            .findByProductIdAndSizeIdAndColor(savedProduct.getId(), size.getId(), input.getColor().trim());
                }

                ProductVariant variant = existingVariant.orElseGet(ProductVariant::new);

                // 5d. Cập nhật/Tạo mới biến thể
                variant.setProduct(savedProduct);
                variant.setSize(size);
                variant.setColor(input.getColor().trim());
                variant.setQuantity(input.getStock() != null ? input.getStock() : 0);

                variantRepository.save(variant);
            }
        }

        // 6. Handle Image Upload
        if (dto.getNewImages() != null && !dto.getNewImages().isEmpty()) {
            for (MultipartFile file : dto.getNewImages()) {
                if (!file.isEmpty()) {
                    try {
                        String imageUrl = FileUploadHelper.saveFile(file, "products/");
                        ProductImage image = ProductImage.builder()
                                .product(savedProduct)
                                .imageUrl(imageUrl)
                                .build();
                        imageRepository.save(image);
                    } catch (IOException e) {
                        throw new AppException("Lỗi khi lưu file ảnh: " + e.getMessage());
                    }
                }
            }
        }
    }

    @Transactional
    public void deleteProduct(Integer id) {
        if (!productRepository.existsById(id)) {
            throw new AppException("Sản phẩm không tìm thấy để xóa.");
        }
        productRepository.deleteById(id);
    }

    public List<ProductDTO> findByCategoryId(Integer categoryId) {
        List<Product> products = productRepository.findByCategoryId(categoryId);

        return products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

}