package com.nhom10.coolmate.products;

import com.nhom10.coolmate.categories.CategoryRepository;
import com.nhom10.coolmate.exception.ResourceNotFoundException;
import com.nhom10.coolmate.sizes.Sizes;
import com.nhom10.coolmate.sizes.SizesRepository;
import org.hibernate.Hibernate; // <-- Bổ sung import này
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.ArrayList; // <-- Bổ sung
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    @Autowired private ProductRepository productRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private SizesRepository sizesRepository;

    // --- 1. LẤY DANH SÁCH (Đã sửa lỗi Lazy) ---
    public List<Product> getAllProducts() {
        List<Product> products = productRepository.findAll();
        // Ép tải dữ liệu Images và ProductSizes trước khi đóng session
        products.forEach(p -> {
            Hibernate.initialize(p.getImages());
            Hibernate.initialize(p.getProductSizes());
        });
        return products;
    }

    public List<Product> searchProducts(String keyword) {
        List<Product> products = productRepository.findByNameContainingIgnoreCase(keyword);
        // Ép tải dữ liệu tương tự như trên
        products.forEach(p -> {
            Hibernate.initialize(p.getImages());
            Hibernate.initialize(p.getProductSizes());
        });
        return products;
    }

    // --- 2. LẤY DỮ LIỆU ĐỂ SỬA (Đã sửa lỗi Lazy) ---
    public ProductDTO getProductDTOById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "ID", id));

        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setDiscountPercent(product.getDiscountPercent());
        if (product.getCategory() != null) {
            dto.setCategoryId(product.getCategory().getId());
        }

        // --- SỬA QUAN TRỌNG TẠI ĐÂY ---
        // Ép tải ảnh và copy sang ArrayList mới để ngắt kết nối với Hibernate Proxy
        Hibernate.initialize(product.getImages());
        dto.setExistingImages(new ArrayList<>(product.getImages()));

        // Load Stock
        List<Sizes> allSizes = sizesRepository.findAll();
        for (Sizes size : allSizes) {
            dto.getSizeIds().add(size.getId());

            // Ép tải productSizes
            Hibernate.initialize(product.getProductSizes());

            Optional<ProductSize> psOpt = product.getProductSizes().stream()
                    .filter(ps -> ps.getSize().getId().equals(size.getId()))
                    .findFirst();
            dto.getStocks().add(psOpt.map(ProductSize::getStock).orElse(0));
        }

        return dto;
    }

    // --- 3. LOGIC LƯU (Giữ nguyên, chỉ đảm bảo có Transactional) ---
    public void saveProduct(ProductDTO dto) throws IOException {
        Product product = new Product();
        if (dto.getId() != null) {
            product = productRepository.findById(dto.getId()).orElse(new Product());
        } else {
            product.setCreatedAt(LocalDateTime.now());
        }

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setDiscountPercent(dto.getDiscountPercent());

        if (dto.getDiscountPercent() != null && dto.getDiscountPercent() > 0) {
            BigDecimal discount = dto.getPrice().multiply(BigDecimal.valueOf(dto.getDiscountPercent())).divide(BigDecimal.valueOf(100));
            product.setDiscountPrice(dto.getPrice().subtract(discount));
        } else {
            product.setDiscountPrice(dto.getPrice());
        }

        product.setCategory(categoryRepository.findById(dto.getCategoryId()).orElse(null));
        Product savedProduct = productRepository.save(product);

        // Lưu ảnh mới
        if (dto.getNewImages() != null) {
            String uploadDir = "src/main/resources/static/uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            for (MultipartFile file : dto.getNewImages()) {
                if (!file.isEmpty()) {
                    String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                    try (InputStream inputStream = file.getInputStream()) {
                        Files.copy(inputStream, uploadPath.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
                        ProductImage img = new ProductImage();
                        img.setProduct(savedProduct);
                        img.setImageUrl("/uploads/" + fileName);
                        savedProduct.getImages().add(img);
                    }
                }
            }
        }

        // Lưu Stock
        if (dto.getSizeIds() != null && dto.getStocks() != null) {
            savedProduct.getProductSizes().clear();
            for (int i = 0; i < dto.getSizeIds().size(); i++) {
                Integer sizeId = dto.getSizeIds().get(i);
                Integer stock = dto.getStocks().get(i);

                if (stock != null) {
                    Sizes size = sizesRepository.findById(sizeId).orElse(null);
                    if (size != null) {
                        ProductSize ps = new ProductSize();
                        ps.setId(new ProductSize.ProductSizeKey(savedProduct.getId(), size.getId()));
                        ps.setProduct(savedProduct);
                        ps.setSize(size);
                        ps.setStock(stock);
                        savedProduct.getProductSizes().add(ps);
                    }
                }
            }
        }
        productRepository.save(savedProduct);
    }


    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Sản phẩm", "ID", id));

        // Thiết lập trạng thái thành 0 (Ngừng hoạt động)
        if (product.getStatus() == 1) {
            product.setStatus(0);
            productRepository.save(product);
        }
        // Nếu muốn XÓA CỨNG:
        // try { productRepository.deleteById(id); } catch (Exception e) { throw new IllegalStateException("Sản phẩm đang có đơn hàng liên quan."); }
    }
}