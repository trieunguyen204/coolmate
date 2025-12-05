package com.nhom10.coolmate.controller;

import com.nhom10.coolmate.order.OrderRepository;
import com.nhom10.coolmate.order.OrderService;
import com.nhom10.coolmate.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminHomeController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @GetMapping("/home")
    public String showHomePage(Model model) {
        // 1. Lấy dữ liệu thống kê
        BigDecimal totalRevenue = orderService.getTotalRevenue();
        long totalOrders = orderRepository.count();
        // Giả sử có 1 số lượng nhỏ đơn hàng chờ xử lý
        long pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING).size();

        // 2. Lấy Top sản phẩm bán chạy (chỉ lấy 10 sản phẩm đầu)
        List<OrderService.ProductSaleDTO> topSellingProducts = orderService.getTopSellingProducts();

        // 3. Add to Model
        model.addAttribute("pageTitle", "Trang Chủ Quản Trị");
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);
        model.addAttribute("topSellingProducts", topSellingProducts);

        return "admin/home";
    }
}