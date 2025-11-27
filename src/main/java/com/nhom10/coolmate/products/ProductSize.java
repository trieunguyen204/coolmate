package com.nhom10.coolmate.products;

import com.nhom10.coolmate.sizes.Sizes;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "product_sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSize {

    @EmbeddedId
    private ProductSizeKey id;

    @ManyToOne
    @MapsId("productId") // Map với field productId trong Key
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @MapsId("sizeId") // Map với field sizeId trong Key
    @JoinColumn(name = "size_id")
    private Sizes size;

    private Integer stock = 0;

    // Class định nghĩa Composite Key
    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSizeKey implements Serializable {
        @Column(name = "product_id")
        private Integer productId;

        @Column(name = "size_id")
        private Integer sizeId;
    }
}