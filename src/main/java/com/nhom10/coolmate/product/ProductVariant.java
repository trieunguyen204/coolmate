package com.nhom10.coolmate.product;

import com.nhom10.coolmate.cart.CartItem;
import com.nhom10.coolmate.order.OrderItem;
import com.nhom10.coolmate.sizes.Sizes;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "product_variants", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"product_id", "size_id", "color"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product; // Ánh xạ quan hệ n-1

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "size_id", nullable = false)
    private Sizes size; // Ánh xạ quan hệ n-1

    @Column(name = "color", nullable = false, length = 20)
    private String color;

    @Column(name = "quantity", columnDefinition = "INT DEFAULT 0")
    private Integer quantity;

    @Column(name = "sku", length = 50)
    private String sku;

    @CreationTimestamp
    @Column(name = "created_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private Timestamp createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private Timestamp updatedAt;

    // Ánh xạ quan hệ 1-n
    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "productVariant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems;
}