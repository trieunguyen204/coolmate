package com.nhom10.coolmate.controller;

import com.nhom10.coolmate.cart.Cart;
import com.nhom10.coolmate.cart.CartService;
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
    public String viewCart(Model model) {
        try {
            Cart cart = cartService.getCartByCurrentUser();
            model.addAttribute("cart", cart);

            // Tính tổng tiền
            double grandTotal = cart.getCartItems().stream()
                    .mapToDouble(item -> item.getPriceAtTime().doubleValue() * item.getQuantity())
                    .sum();
            model.addAttribute("grandTotal", grandTotal);

        } catch (Exception e) {
            // Nếu chưa đăng nhập hoặc lỗi, trả về giỏ rỗng
            return "redirect:/login";
        }
        return "user/cart"; // Trả về file cart.html
    }

    // Thêm vào giỏ hàng (Xử lý POST từ Product Detail)
    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam Integer quantity,
                            @RequestParam String size,
                            @RequestParam String color) {

        cartService.addToCart(productId, quantity, size, color);
        return "redirect:/user/cart"; // Thêm xong chuyển hướng đến trang giỏ hàng
    }

    // Xóa sản phẩm khỏi giỏ
    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Integer id) {
        cartService.removeFromCart(id);
        return "redirect:/user/cart";
    }
}