package com.nhom10.coolmate.sizes;

import com.nhom10.coolmate.product.ProductVariant;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Entity
@Table(name = "sizes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Sizes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "size_name", nullable = false, length = 20, unique = true)
    private String sizeName;

    // Ánh xạ quan hệ 1-n
    @OneToMany(mappedBy = "size", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductVariant> productVariants;
}