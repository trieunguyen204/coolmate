package com.nhom10.coolmate.order;

import com.nhom10.coolmate.address.Address;
import com.nhom10.coolmate.address.AddressRepository;
import com.nhom10.coolmate.cart.Cart;
import com.nhom10.coolmate.cart.CartService;
import com.nhom10.coolmate.exception.AppException;
import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.user.UserRepository;
import com.nhom10.coolmate.vouchers.Voucher;
import com.nhom10.coolmate.vouchers.VoucherService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CheckoutController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final VoucherService voucherService;

    // =========================================================================
    // 1. HIỂN THỊ TRANG THANH TOÁN
    // =========================================================================
    @GetMapping("/user/checkout")
    public String showCheckoutPage(Model model,
                                   HttpServletRequest request,
                                   HttpServletResponse response,
                                   Principal principal) {
        // 1. Lấy giỏ hàng
        Cart cart = cartService.getCart(request, response);
        if (cart == null || cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
            return "redirect:/user/cart";
        }

        // 2. Tính tổng tiền tạm tính
        BigDecimal total = cart.getCartItems().stream()
                .map(item -> item.getPriceAtTime().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Logic cho User đã đăng nhập
        if (principal != null) {
            User user = userRepository.findByEmail(principal.getName()).orElse(null);
            if (user != null) {
                // Điền thông tin cá nhân
                model.addAttribute("userFullName", user.getFullName());
                model.addAttribute("userPhone", user.getPhone());
                // Email (nếu muốn điền sẵn)
                model.addAttribute("userEmail", user.getEmail());

                // Lấy Sổ địa chỉ
                List<Address> savedAddresses = addressRepository.findByUser(user);
                model.addAttribute("savedAddresses", savedAddresses);

                // Lấy danh sách Voucher khả dụng cho User
                List<Voucher> myVouchers = voucherService.getAvailableVouchers();
                model.addAttribute("myVouchers", myVouchers);
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("total", total);

        return "user/checkout";
    }

    // =========================================================================
    // 2. XỬ LÝ ĐẶT HÀNG - ĐÃ CẬP NHẬT THÊM THAM SỐ discountAmount
    // =========================================================================
    @PostMapping("/place-order")
    public String placeOrder(
            @RequestParam("fullName") String fullName,
            @RequestParam("phone") String phone,
            @RequestParam("addressDetail") String addressDetail,
            @RequestParam(value = "orderNotes", required = false) String orderNotes,
            @RequestParam("paymentMethod") String paymentMethod,
            @RequestParam(value = "voucherCode", required = false) String voucherCode,
            @RequestParam(value = "discountAmount", required = false) BigDecimal discountAmount, // NHẬN THÊM GIÁ TRỊ GIẢM GIÁ
            HttpServletRequest request,
            HttpServletResponse response,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            // Đảm bảo discountAmount không null
            if (discountAmount == null) {
                discountAmount = BigDecimal.ZERO;
            }

            // Gọi OrderService để tạo đơn hàng
            // TRUYỀN THÊM discountAmount VÀO createOrder
            Order order = orderService.createOrder(
                    fullName,
                    phone,
                    addressDetail,
                    orderNotes,
                    paymentMethod,
                    request,
                    response,
                    principal,
                    voucherCode,
                    discountAmount // TRUYỀN GIÁ TRỊ GIẢM GIÁ
            );

            // Chuyển hướng đến trang thành công
            return "redirect:/user/order_success/" + order.getId();

        } catch (AppException e) {
            // Lỗi nghiệp vụ (ví dụ: Hết hàng, Voucher không hợp lệ...)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/user/checkout";
        } catch (Exception e) {
            // Lỗi hệ thống
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Đã xảy ra lỗi khi đặt hàng. Vui lòng thử lại.");
            return "redirect:/user/checkout";
        }
    }

    // =========================================================================
    // 3. API KIỂM TRA VOUCHER (Cho Javascript gọi AJAX)
    // =========================================================================
    @GetMapping("/api/voucher/check")
    @ResponseBody
    public ResponseEntity<?> checkVoucher(@RequestParam String code, @RequestParam BigDecimal total) {
        try {
            // 1. Kiểm tra tính hợp lệ của Voucher
            Voucher voucher = voucherService.validateVoucher(code, total);

            // 2. TÍNH TOÁN SỐ TIỀN GIẢM THỰC TẾ
            BigDecimal discount = voucherService.calculateDiscount(voucher, total);

            // Trả về JSON thành công
            return ResponseEntity.ok(new VoucherResponse(
                    true,
                    voucher.getCode(),
                    discount, // TRẢ VỀ SỐ TIỀN GIẢM ĐÃ TÍNH TOÁN
                    "Áp dụng mã giảm giá thành công!"
            ));
        } catch (Exception e) {
            // Trả về JSON lỗi
            return ResponseEntity.ok(new VoucherResponse(
                    false,
                    null,
                    BigDecimal.ZERO,
                    e.getMessage()
            ));
        }
    }

    // DTO nội bộ dùng để trả về JSON cho API Voucher
    @Data
    @AllArgsConstructor
    static class VoucherResponse {
        private boolean success;
        private String code;
        private BigDecimal discountAmount;
        private String message;
    }

    // =========================================================================
    // 4. TRANG THÔNG BÁO THÀNH CÔNG
    // =========================================================================
    @GetMapping("/user/order_success/{id}")
    public String orderSuccess(@PathVariable Integer id, Model model) {
        try {
            OrderDTO orderDTO = orderService.getOrderDetail(id);
            model.addAttribute("order", orderDTO);
            model.addAttribute("orderDetails", orderDTO.getOrderItems());
            return "user/order_success";
        } catch (Exception e) {
            return "redirect:/";
        }
    }
}