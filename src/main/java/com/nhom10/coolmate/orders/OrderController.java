package com.nhom10.coolmate.orders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/orders")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Hiển thị danh sách & Modal chi tiết
    @GetMapping
    public String listOrders(Model model,
                             @RequestParam(value = "status", required = false) String status,
                             @RequestParam(value = "detailId", required = false) Integer detailId) {

        // Lấy danh sách đơn hàng (đã load sẵn User/Address để hiển thị lên bảng)
        List<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderService.getOrdersByStatus(status);
        } else {
            orders = orderService.getAllOrders();
        }
        model.addAttribute("orders", orders);
        model.addAttribute("currentStatus", status);

        // Nếu có detailId -> Lấy chi tiết để mở Modal
        if (detailId != null) {
            try {
                Order orderDetail = orderService.getOrderById(detailId);
                model.addAttribute("orderDetail", orderDetail);
                model.addAttribute("showModal", true);
            } catch (Exception e) {
                model.addAttribute("errorMessage", "Không tìm thấy đơn hàng #" + detailId);
            }
        }

        model.addAttribute("pageTitle", "Quản lý Đơn hàng");
        return "admin/orders";
    }

    @PostMapping("/update-status")
    public String updateStatus(@RequestParam Integer id,
                               @RequestParam String status,
                               RedirectAttributes redirectAttributes) {
        try {
            orderService.updateOrderStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật đơn hàng #" + id + " thành công!");
            return "redirect:/admin/orders?detailId=" + id; // Mở lại modal để xem kết quả
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/orders";
        }
    }
}