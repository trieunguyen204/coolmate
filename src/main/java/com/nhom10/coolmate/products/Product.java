package com.nhom10.coolmate.products;

import com.nhom10.coolmate.categories.Category;
import com.nhom10.coolmate.comment.Comment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;

    @Column(name = "discount_percent")
    private Integer discountPercent = 0;

    @Column(name = "discount_price")
    private BigDecimal discountPrice;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    // BỔ SUNG TRƯỜNG STATUS CHO LOGIC SOFT DELETE
    @Column(name = "status", columnDefinition = "tinyint default 1")
    private Integer status = 1; // 1: Active, 0: Inactive/Deleted

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Quan hệ 1-N với Ảnh
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();

    // Quan hệ 1-N với Size (Kho hàng)
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSize> productSizes = new ArrayList<>();

    // QUAN HỆ: 1 Product có nhiều Comment
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Comment> comments = new ArrayList<>();
}