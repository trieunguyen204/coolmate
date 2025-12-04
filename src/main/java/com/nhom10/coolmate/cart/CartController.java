package com.nhom10.coolmate.cart;

import com.nhom10.coolmate.cart.Cart;
import com.nhom10.coolmate.cart.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // --- 1. Xem giỏ hàng ---
    @GetMapping
    public String viewCart(Model model, HttpServletRequest request, HttpServletResponse response) {
        // Lấy giỏ hàng (xử lý tự động User hoặc Khách vãng lai)
        Cart cart = cartService.getCart(request, response);
        model.addAttribute("cart", cart);

        // Tính tổng tiền
        double grandTotal = 0;
        if (cart != null && cart.getCartItems() != null) {
            grandTotal = cart.getCartItems().stream()
                    .mapToDouble(item -> item.getPriceAtTime().doubleValue() * item.getQuantity())
                    .sum();
        }
        model.addAttribute("grandTotal", grandTotal);

        return "user/cart"; // Trả về template: templates/user/cart.html
    }

    // --- 2. Thêm vào giỏ hàng (Xử lý POST từ form) ---
    @PostMapping("/add")
    public String addToCart(@RequestParam Integer productId,
                            @RequestParam Integer quantity,
                            @RequestParam String size,
                            @RequestParam String color,
                            HttpServletRequest request,   // Để lấy link trang trước (Referer)
                            HttpServletResponse response, // Để lưu cookie (cho khách)
                            RedirectAttributes redirectAttributes) { // Để gửi thông báo

        try {
            // Gọi service xử lý logic thêm sản phẩm
            cartService.addToCart(productId, quantity, size, color, request, response);

            // Thông báo thành công (chỉ hiện 1 lần rồi tự mất)
            redirectAttributes.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào giỏ hàng thành công!");

        } catch (Exception e) {
            // Thông báo lỗi (ví dụ: Hết hàng, không tìm thấy size...)
            e.printStackTrace(); // In lỗi ra console để debug
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // --- Logic chuyển hướng thông minh ---
        // Lấy URL của trang trước đó để quay lại đúng chỗ
        String referer = request.getHeader("Referer");

        // Nếu không lấy được Referer (hiếm gặp), quay về trang danh sách sản phẩm
        return "redirect:" + (referer != null ? referer : "/user/product");
    }

    // --- 3. Xóa sản phẩm khỏi giỏ ---
    @GetMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Integer id,
                                 HttpServletRequest request,
                                 HttpServletResponse response,
                                 RedirectAttributes redirectAttributes) {
        try {
            cartService.removeFromCart(id, request, response);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa sản phẩm: " + e.getMessage());
        }

        return "redirect:/user/cart";
    }
}