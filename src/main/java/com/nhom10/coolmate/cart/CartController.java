package com.nhom10.coolmate.controller;

import com.nhom10.coolmate.cart.Cart;
import com.nhom10.coolmate.cart.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/user/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // Xem giỏ hàng
    @GetMapping
    public String viewCart(Model model, HttpServletRequest request, HttpServletResponse response) {
        // Lấy giỏ hàng (User hoặc Guest)
        Cart cart = cartService.getCart(request, response);
        model.addAttribute("cart", cart);

        // Tính tổng tiền
        double grandTotal = cart.getCartItems().stream()
                .mapToDouble(item -> item.getPriceAtTime().doubleValue() * item.getQuantity())
                .sum();
        model.addAttribute("grandTotal", grandTotal);

        return "user/cart";
    }

    // Thêm vào giỏ hàng
    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam Integer quantity,
                            @RequestParam String size,
                            @RequestParam String color,
                            HttpServletRequest request,
                            HttpServletResponse response) {

        // Truyền request, response xuống service để xử lý Cookie
        cartService.addToCart(productId, quantity, size, color, request, response);
        return "redirect:/user/cart";
    }

    // Xóa sản phẩm khỏi giỏ
    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Integer id,
                                 HttpServletRequest request,
                                 HttpServletResponse response) {
        cartService.removeFromCart(id, request, response);
        return "redirect:/user/cart";
    }
}