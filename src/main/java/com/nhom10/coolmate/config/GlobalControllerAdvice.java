package com.nhom10.coolmate.config;

import com.nhom10.coolmate.cart.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final CartService cartService;


    @ModelAttribute("cartItemCount")
    public int populateCartItemCount(HttpServletRequest request, HttpServletResponse response) {

        return cartService.countItemsInCart(request, response);
    }
}