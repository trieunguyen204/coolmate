package com.nhom10.coolmate.order;

import com.nhom10.coolmate.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class OrderController {

    private final OrderService orderService;

    // --- 1. READ: Hiển thị danh sách Đơn hàng (Đã sửa logic lọc) ---
    @GetMapping
    public String listOrders(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "detailId", required = false) Integer detailId,
            Model model) {

        List<OrderDTO> orders = orderService.getAllOrders(status);
        model.addAttribute("orders", orders);

        // Đảm bảo currentStatus là chuỗi (String) để khớp với Thymeleaf
        model.addAttribute("currentStatus", status != null ? status.toUpperCase() : "ALL");
        model.addAttribute("pageTitle", "Quản lý Đơn hàng");

        // Xử lý hiển thị modal chi tiết (nếu có detailId)
        if (detailId != null) {
            try {
                OrderDTO orderDetail = orderService.getOrderDetail(detailId);
                model.addAttribute("orderDetail", orderDetail);
                model.addAttribute("showModal", true);
            } catch (AppException e) {
                model.addAttribute("errorMessage", "Không tìm thấy chi tiết đơn hàng.");
            }
        }

        return "admin/orders";
    }

    // --- 2. UPDATE: Cập nhật trạng thái đơn hàng (Sử dụng cho form NEXT) ---
    @PostMapping("/update-status")
    public String updateStatus(
            @RequestParam("id") Integer orderId,
            @RequestParam("status") String status,
            RedirectAttributes redirectAttributes) {

        try {
            OrderStatus newStatus = OrderStatus.valueOf(status.toUpperCase());
            orderService.updateOrderStatus(orderId, newStatus);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đơn hàng **#" + orderId + "** thành **" + newStatus.name() + "** thành công.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Trạng thái không hợp lệ: " + status);
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        // Giữ lại bộ lọc status hiện tại (nếu có)
        return "redirect:/admin/orders";
    }
}