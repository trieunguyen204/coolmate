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

import jakarta.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
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

    // Tính giá sau khi giảm giá
    private BigDecimal calculateDiscountPrice(BigDecimal price, Integer discountPercent) {
        if (discountPercent == null || discountPercent <= 0) {
            return price;
        }
        // Rounding Half Up cho tính toán giá tiền
        BigDecimal discountFactor = BigDecimal.valueOf(100 - discountPercent.doubleValue()).divide(BigDecimal.valueOf(100));
        return price.multiply(discountFactor).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    private ProductDTO mapToDTO(Product product) {
        // Tính giá giảm
        BigDecimal discountPrice = calculateDiscountPrice(product.getPrice(), product.getDiscountPercent());

        // Lấy thông tin biến thể cho hiển thị (ProductSizeColorStockDTO)
        List<ProductDTO.ProductSizeColorStockDTO> variantsDto = product.getVariants().stream()
                .filter(v -> v.getSize() != null)
                .map(v -> ProductDTO.ProductSizeColorStockDTO.builder()
                        .variantId(v.getId())
                        .sizeName(v.getSize().getSizeName())
                        .color(v.getColor())
                        .stock(v.getQuantity())
                        .build())
                .collect(Collectors.toList());

        // Lấy tên danh mục
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : "-";


        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPercent(product.getDiscountPercent())
                .material(product.getMaterial())
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(categoryName) // <<< ĐÃ THÊM MAP TRƯỜNG NÀY
                .discountPrice(discountPrice) // Giá đã giảm
                .productVariants(variantsDto) // Biến thể cho bảng
                .existingImages(product.getImages()) // Ảnh hiện tại
                .build();
    }

    // ==========================================================
    // READ: getAllProducts
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
    // READ: getProductById
    // ==========================================================
    public ProductDTO getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException("Sản phẩm không tìm thấy: " + id));

        return mapToDTO(product);
    }


    // --- CRUD Logic ---

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
}