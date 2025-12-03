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
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductRepository productRepository;
    private final SizesRepository sizesRepository;

    private static final String CART_COOKIE_NAME = "CART_SESSION";

    // --- HÀM HỖ TRỢ LẤY GIỎ HÀNG THÔNG MINH (USER HOẶC GUEST) ---
    public Cart getCart(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. TRƯỜNG HỢP ĐÃ ĐĂNG NHẬP
        if (authentication != null && isAuthenticated(authentication)) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException("User not found"));

            // Tìm giỏ hàng của User, nếu chưa có thì tạo mới
            return cartRepository.findByUser(user).orElseGet(() -> {
                Cart newCart = Cart.builder().user(user).cartItems(new ArrayList<>()).build();
                return cartRepository.save(newCart);
            });
        }

        // 2. TRƯỜNG HỢP KHÁCH VÃNG LAI (GUEST)
        else {
            String sessionToken = null;

            // Tìm cookie token
            if (request.getCookies() != null) {
                for (Cookie cookie : request.getCookies()) {
                    if (CART_COOKIE_NAME.equals(cookie.getName())) {
                        sessionToken = cookie.getValue();
                        break;
                    }
                }
            }

            // Nếu chưa có token hoặc tìm không thấy giỏ hàng tương ứng -> Tạo mới
            if (sessionToken == null) {
                return createGuestCart(response);
            } else {
                String finalToken = sessionToken;
                return cartRepository.findBySessionToken(finalToken).orElseGet(() -> createGuestCart(response));
            }
        }
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private Cart createGuestCart(HttpServletResponse response) {
        String token = UUID.randomUUID().toString();

        // Tạo Cookie
        Cookie cookie = new Cookie(CART_COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 ngày
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        // Tạo Giỏ hàng trong DB (User = null)
        Cart newCart = Cart.builder()
                .sessionToken(token)
                .user(null) // Quan trọng: User là null
                .cartItems(new ArrayList<>())
                .build();
        return cartRepository.save(newCart);
    }

    // --- LOGIC THÊM VÀO GIỎ (Đã sửa để nhận Request/Response) ---
    @Transactional
    public void addToCart(Integer productId, Integer quantity, String sizeName, String color,
                          HttpServletRequest request, HttpServletResponse response) {

        // Lấy giỏ hàng (Tự động xử lý Guest hay User)
        Cart cart = getCart(request, response);

        // ... Logic tìm sản phẩm giữ nguyên ...
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Sản phẩm không tồn tại"));

        Sizes size = sizesRepository.findBySizeName(sizeName)
                .orElseThrow(() -> new AppException("Size không hợp lệ: " + sizeName));

        ProductVariant variant = productVariantRepository.findByProductIdAndSizeIdAndColor(productId, size.getId(), color)
                .orElseThrow(() -> new AppException("Sản phẩm màu " + color + " size " + sizeName + " hiện không khả dụng."));

        if (variant.getQuantity() < quantity) {
            throw new AppException("Số lượng tồn kho không đủ.");
        }

        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            BigDecimal finalPrice = product.getPrice();
            if(product.getDiscountPercent() > 0) {
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

    // Xóa item (Cần Request/Response để xác định đúng giỏ)
    @Transactional
    public void removeFromCart(Integer cartItemId, HttpServletRequest request, HttpServletResponse response) {
        Cart cart = getCart(request, response);

        // Chỉ xóa nếu item thuộc về giỏ hàng hiện tại (Bảo mật)
        cart.getCartItems().removeIf(item -> item.getId().equals(cartItemId));
        cartItemRepository.deleteById(cartItemId);
    }

    // Đếm số lượng item cho Badge
    public int countItemsInCart(HttpServletRequest request, HttpServletResponse response) {
        Cart cart = getCart(request, response);
        return cart.getCartItems().stream().mapToInt(CartItem::getQuantity).sum();
    }
}