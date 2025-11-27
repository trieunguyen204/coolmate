package com.nhom10.coolmate.orders;

import com.nhom10.coolmate.products.Product;
import com.nhom10.coolmate.sizes.Sizes;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "size_id")
    private Sizes size;

    private Integer quantity;

    private BigDecimal price; // Giá gốc lúc mua

    @Column(name = "discount_price")
    private BigDecimal discountPrice; // Giá thực tế lúc mua
}