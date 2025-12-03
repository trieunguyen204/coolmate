package com.nhom10.coolmate.cart;

import com.nhom10.coolmate.exception.AppException;
import com.nhom10.coolmate.product.Product;
import com.nhom10.coolmate.product.ProductRepository;
import com.nhom10.coolmate.product.ProductVariant;
import com.nhom10.coolmate.product.ProductVariantRepository;
import com.nhom10.coolmate.sizes.Sizes;
import com.nhom10.coolmate.sizes.SizesRepository;
import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final SizesRepository sizesRepository;

    // Lấy User hiện tại đang đăng nhập
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Người dùng không tồn tại"));
    }

    // Lấy giỏ hàng của user hiện tại
    public Cart getCartByCurrentUser() {
        User user = getCurrentUser();
        return cartRepository.findByUser(user).orElseGet(() -> {
            Cart newCart = Cart.builder().user(user).cartItems(new ArrayList<>()).build();
            return cartRepository.save(newCart);
        });
    }

    // --- LOGIC THÊM VÀO GIỎ ---
    @Transactional
    public void addToCart(Integer productId, Integer quantity, String sizeName, String color) {
        User user = getCurrentUser();
        Cart cart = getCartByCurrentUser();

        // 1. Tìm Product
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Sản phẩm không tồn tại"));

        // 2. Tìm Size (Giả sử bạn tìm size theo tên string "L", "M")
        Sizes size = sizesRepository.findBySizeName(sizeName)
                .orElseThrow(() -> new AppException("Size không hợp lệ: " + sizeName));

        // 3. Tìm ProductVariant (Sự kết hợp giữa Product + Size + Color)
        ProductVariant variant = productVariantRepository.findByProductIdAndSizeIdAndColor(productId, size.getId(), color)
                .orElseThrow(() -> new AppException("Sản phẩm màu " + color + " size " + sizeName + " hiện không khả dụng."));

        // 4. Kiểm tra tồn kho
        if (variant.getQuantity() < quantity) {
            throw new AppException("Số lượng tồn kho không đủ.");
        }

        // 5. Kiểm tra xem item đã có trong giỏ chưa
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Nếu có rồi -> Tăng số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            // Nếu chưa có -> Tạo mới
            // Tính giá sau giảm (nếu có)
            BigDecimal finalPrice = product.getPrice();
            if(product.getDiscountPercent() > 0) {
                // Logic tính giá giảm (bạn có thể dùng lại logic trong ProductService)
                BigDecimal discountFactor = BigDecimal.valueOf(100 - product.getDiscountPercent()).divide(BigDecimal.valueOf(100));
                finalPrice = product.getPrice().multiply(discountFactor);
            }

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(quantity)
                    .priceAtTime(finalPrice)
                    .build();
            cartItemRepository.save(newItem);
        }
    }

    // Xóa item khỏi giỏ
    @Transactional
    public void removeFromCart(Integer cartItemId) {
        Cart cart = getCartByCurrentUser();
        cart.getCartItems().removeIf(item -> item.getId().equals(cartItemId));
        cartItemRepository.deleteById(cartItemId);
    }

    // Lấy tổng số lượng item để hiển thị lên badge menu
    public int countItemsInCart() {
        try {
            Cart cart = getCartByCurrentUser();
            return cart.getCartItems().stream().mapToInt(CartItem::getQuantity).sum();
        } catch (Exception e) {
            return 0; // Chưa đăng nhập
        }
    }
}