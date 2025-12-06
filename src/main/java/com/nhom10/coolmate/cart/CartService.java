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
import java.math.RoundingMode;
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
    private static final int COOKIE_MAX_AGE = 7 * 24 * 60 * 60; // 7 ngày

    // --- 1. CORE: LẤY GIỎ HÀNG (XỬ LÝ CẢ USER VÀ GUEST) ---
    public Cart getCart(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // A. Trường hợp: Đã đăng nhập
        if (authentication != null && isAuthenticated(authentication)) {
            String email = authentication.getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new AppException("User not found"));

            // Tìm giỏ hàng theo User, nếu chưa có thì tạo mới
            return cartRepository.findByUser(user).orElseGet(() -> {
                Cart newCart = Cart.builder()
                        .user(user)
                        .cartItems(new ArrayList<>())
                        .build();
                return cartRepository.save(newCart);
            });
        }

        // B. Trường hợp: Khách vãng lai (Guest) - Dùng Cookie
        else {
            // [QUAN TRỌNG - FIX LỖI]
            // Tạo Session ngay lập tức để Spring Security có chỗ lưu CSRF token.
            // Điều này ngăn lỗi "Cannot create a session after the response has been committed"
            // khi Controller thực hiện redirect sau đó.
            request.getSession(true);

            String sessionToken = getSessionTokenFromCookie(request);

            // Nếu chưa có cookie token -> Tạo mới
            if (sessionToken == null) {
                return createGuestCart(request, response);
            } else {
                // Nếu có token -> Tìm giỏ hàng, nếu không thấy (VD DB bị xóa) -> Tạo mới
                return cartRepository.findBySessionToken(sessionToken)
                        .orElseGet(() -> createGuestCart(request, response));
            }
        }
    }

    // --- 2. LOGIC THÊM VÀO GIỎ ---
    @Transactional
    public void addToCart(Integer productId, Integer quantity, String sizeName, String color,
                          HttpServletRequest request, HttpServletResponse response) {

        // Bước 1: Lấy giỏ hàng hiện tại (Guest hoặc User)
        // Lưu ý: getCart() sẽ tự động tạo Session/Cookie nếu chưa có
        Cart cart = getCart(request, response);

        // Bước 2: Validate dữ liệu đầu vào
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException("Sản phẩm không tồn tại"));

        Sizes size = sizesRepository.findBySizeName(sizeName)
                .orElseThrow(() -> new AppException("Size không hợp lệ: " + sizeName));

        // Bước 3: Tìm Product Variant (Sự kết hợp giữa SP + Màu + Size)
        ProductVariant variant = productVariantRepository.findByProductIdAndSizeIdAndColor(productId, size.getId(), color)
                .orElseThrow(() -> new AppException("Sản phẩm màu " + color + " size " + sizeName + " hiện không khả dụng."));

        // Bước 4: Kiểm tra tồn kho
        if (variant.getQuantity() < quantity) {
            throw new AppException("Số lượng tồn kho không đủ (chỉ còn " + variant.getQuantity() + ").");
        }

        // Bước 5: Kiểm tra xem sản phẩm đã có trong giỏ chưa
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProductVariant().getId().equals(variant.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Nếu có rồi -> Cộng dồn số lượng
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            // Có thể check lại tồn kho sau khi cộng dồn ở đây nếu muốn chặt chẽ hơn
            cartItemRepository.save(item);
        } else {
            // Nếu chưa có -> Tạo mục mới
            BigDecimal finalPrice = calculateFinalPrice(product);

            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productVariant(variant)
                    .quantity(quantity)
                    .priceAtTime(finalPrice)
                    .build();

            // Thêm vào list để đồng bộ (nếu dùng cascade) và lưu
            cart.getCartItems().add(newItem);
            cartItemRepository.save(newItem);
        }
    }

    // --- 3. XÓA ITEM KHỎI GIỎ ---
    @Transactional
    public void removeFromCart(Integer cartItemId, HttpServletRequest request, HttpServletResponse response) {
        Cart cart = getCart(request, response);

        // Bảo mật: Chỉ xóa nếu item đó thực sự nằm trong giỏ hàng của người đang request
        boolean removed = cart.getCartItems().removeIf(item -> item.getId().equals(cartItemId));

        if (removed) {
            // Xóa cứng trong DB
            cartItemRepository.deleteById(cartItemId);
        } else {
            throw new AppException("Không tìm thấy sản phẩm trong giỏ hàng để xóa.");
        }
    }

    // --- 4. CẬP NHẬT SỐ LƯỢNG (Tăng/Giảm ở trang giỏ hàng) ---
    @Transactional
    public void updateQuantity(Integer cartItemId, Integer newQuantity, HttpServletRequest request, HttpServletResponse response) {
        if (newQuantity <= 0) {
            removeFromCart(cartItemId, request, response);
            return;
        }

        Cart cart = getCart(request, response);
        CartItem item = cart.getCartItems().stream()
                .filter(i -> i.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new AppException("Sản phẩm không tồn tại trong giỏ."));

        // Check tồn kho
        if (item.getProductVariant().getQuantity() < newQuantity) {
            throw new AppException("Kho không đủ hàng.");
        }

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);
    }

    // --- 5. ĐẾM SỐ LƯỢNG (Hiển thị lên icon giỏ hàng) ---
    public int countItemsInCart(HttpServletRequest request, HttpServletResponse response) {
        Cart cart = getCart(request, response);
        if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            return 0;
        }
        // Tính tổng số lượng sản phẩm (Ví dụ: 2 áo A + 1 áo B = 3)
        return cart.getCartItems().stream().mapToInt(CartItem::getQuantity).sum();
    }

    // ================= HELPER METHODS =================

    private Cart createGuestCart(HttpServletRequest request, HttpServletResponse response) {
        String token = UUID.randomUUID().toString();

        // Tạo Cookie gửi về client
        Cookie cookie = new Cookie(CART_COOKIE_NAME, token);
        cookie.setPath("/");
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setHttpOnly(true); // Bảo mật: JS không đọc được
        response.addCookie(cookie);

        // Lưu Cart Guest vào DB
        Cart newCart = Cart.builder()
                .sessionToken(token)
                .user(null) // Quan trọng: User null
                .cartItems(new ArrayList<>())
                .build();
        return cartRepository.save(newCart);
    }

    private String getSessionTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (CART_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private BigDecimal calculateFinalPrice(Product product) {
        BigDecimal price = product.getPrice();
        if (product.getDiscountPercent() != null && product.getDiscountPercent() > 0) {
            BigDecimal discount = BigDecimal.valueOf(product.getDiscountPercent());
            BigDecimal factor = BigDecimal.valueOf(100).subtract(discount).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            return price.multiply(factor);
        }
        return price;
    }
    // --- 6. XÓA SẠCH GIỎ HÀNG (Dùng sau khi Checkout) ---
    @Transactional
    public void clearCart(HttpServletRequest request, HttpServletResponse response) {
        Cart cart = getCart(request, response);
        if (cart != null && !cart.getCartItems().isEmpty()) {
            // Xóa tất cả item trong DB
            cartItemRepository.deleteAll(cart.getCartItems());
            // Xóa list trong memory để tránh lỗi nếu dùng tiếp object này
            cart.getCartItems().clear();
            cartRepository.save(cart);
        }
    }

    public int getCartItemCountForLoggedInUser(Authentication authentication) {

        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {


            return 0;
        }
        return 0;
    }
}