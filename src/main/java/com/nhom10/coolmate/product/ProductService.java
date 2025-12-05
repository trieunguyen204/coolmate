package com.nhom10.coolmate.product;

import com.nhom10.coolmate.category.Category;
import com.nhom10.coolmate.category.CategoryRepository;
import com.nhom10.coolmate.comment.CommentService; // THÊM: Import CommentService
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
import java.math.RoundingMode;
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
    private final CommentService commentService; // THÊM: Inject CommentService

    // --- Mapper & Calculation ---

    private BigDecimal calculateDiscountPrice(BigDecimal price, Integer discountPercent) {
        if (discountPercent == null || discountPercent <= 0) {
            return price;
        }
        BigDecimal discountFactor = BigDecimal.valueOf(100 - discountPercent.doubleValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        return price.multiply(discountFactor).setScale(0, RoundingMode.HALF_UP); // Set Scale 0 để làm tròn giá bán
    }

    private ProductDTO mapToDTO(Product product) {
        BigDecimal discountPrice = calculateDiscountPrice(product.getPrice(), product.getDiscountPercent());
        BigDecimal oldPrice = (product.getDiscountPercent() != null && product.getDiscountPercent() > 0) ? product.getPrice() : null;

        String mainImageUrl = product.getImages() != null && !product.getImages().isEmpty()
                ? product.getImages().get(0).getImageUrl()
                : "/images/placeholder_product.jpg";

        String categoryName = "-";
        Integer categoryId = null;

        try {
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

        // THÊM: Lấy điểm đánh giá trung bình
        Double averageRating = commentService.getAverageRatingByProductId(product.getId());


        return ProductDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .discountPercent(product.getDiscountPercent())
                .material(product.getMaterial())
                .categoryId(categoryId)
                .categoryName(categoryName)
                .discountPrice(discountPrice)
                .currentPrice(discountPrice)
                .oldPrice(oldPrice)
                .imageUrl(mainImageUrl)
                .productVariants(variantsDto)
                .existingImages(product.getImages())
                .averageRating(averageRating) // Gán giá trị đánh giá
                .build();
    }

    // ==========================================================
    // CLIENT: getFilteredProducts (CHỨC NĂNG LỌC VÀ SẮP XẾP)
    // ==========================================================
    /**
     * Lọc và sắp xếp sản phẩm dựa trên keyword, khoảng giá và thứ tự sắp xếp.
     * @param keyword Từ khóa tìm kiếm
     * @param priceRanges Danh sách các chuỗi khoảng giá (ví dụ: ["0-200000"])
     * @param sortOrder Thứ tự sắp xếp (ví dụ: "price_asc", "createdAt_desc")
     * @return List<ProductDTO>
     */
    public List<ProductDTO> getFilteredProducts(String keyword, List<String> priceRanges, String sortOrder) {
        // 1. Phân tích khoảng giá (Giữ nguyên logic từ bước trước)
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;

        if (priceRanges != null && !priceRanges.isEmpty()) {
            for (String range : priceRanges) {
                String[] parts = range.split("-");

                if (!parts[0].isEmpty()) {
                    BigDecimal currentMin = new BigDecimal(parts[0]);
                    if (minPrice == null || currentMin.compareTo(minPrice) < 0) {
                        minPrice = currentMin;
                    }
                }

                if (parts.length > 1 && !parts[1].isEmpty()) {
                    BigDecimal currentMax = new BigDecimal(parts[1]);
                    if (maxPrice == null || currentMax.compareTo(maxPrice) > 0) {
                        maxPrice = currentMax;
                    }
                }
            }
        }

        // 2. Gọi Repository với logic phức tạp (Filter, Sắp xếp)
        List<Product> products = productRepository.findByKeywordAndPriceRange(keyword, minPrice, maxPrice, sortOrder);


        return products.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // ==========================================================
    // CLIENT: getFeaturedProducts
    // ==========================================================
    public List<ProductDTO> getFeaturedProducts() {
        List<Product> products = productRepository.findTop8ByOrderByCreatedAtDesc();

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
        // ... (Logic save product giữ nguyên) ...
        Category category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new AppException("Danh mục không tồn tại."));

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

        Product savedProduct = productRepository.save(product);

        if (dto.getVariantInputs() != null && !dto.getVariantInputs().isEmpty()) {

            java.util.Set<String> uniqueVariants = new java.util.HashSet<>();

            for (ProductDTO.VariantInputDTO input : dto.getVariantInputs()) {

                String uniqueKey = input.getSizeId() + "_" + input.getColor().trim().toLowerCase();
                if (!uniqueVariants.add(uniqueKey)) {
                    throw new AppException("Biến thể trùng lặp được phát hiện trong dữ liệu nhập: Size ID " + input.getSizeId() + " và Màu " + input.getColor());
                }

                Sizes size = sizesRepository.findById(input.getSizeId())
                        .orElseThrow(() -> new AppException("Size không tồn tại: " + input.getSizeId()));

                Optional<ProductVariant> existingVariant = Optional.empty();
                if (input.getVariantId() != null) {
                    existingVariant = variantRepository.findById(input.getVariantId());
                } else {
                    existingVariant = variantRepository
                            .findByProductIdAndSizeIdAndColor(savedProduct.getId(), size.getId(), input.getColor().trim());
                }

                ProductVariant variant = existingVariant.orElseGet(ProductVariant::new);

                variant.setProduct(savedProduct);
                variant.setSize(size);
                variant.setColor(input.getColor().trim());
                variant.setQuantity(input.getStock() != null ? input.getStock() : 0);

                variantRepository.save(variant);
            }
        }

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