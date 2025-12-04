package com.nhom10.coolmate.config;

import com.nhom10.coolmate.cart.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice // Annotation này giúp áp dụng cho toàn bộ Controller
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CartService cartService;

    // Trong GlobalControllerAdvice.java
    @ModelAttribute("cartItemCount")
    public int populateCartItemCount(HttpServletRequest request, HttpServletResponse response) {
        // Phải truyền request và response vào đây
        return cartService.countItemsInCart(request, response);
    }
}