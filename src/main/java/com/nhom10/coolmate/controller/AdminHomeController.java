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
        // 1. Thống kê tổng quan
        BigDecimal totalRevenue = orderService.getTotalRevenue();
        long totalOrders = orderRepository.count();
        long pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING).size();

        // 2. [CẬP NHẬT] Lấy Top sản phẩm bán chạy (Top 10)
        List<OrderService.ProductSaleDTO> topSellingProducts = orderService.getTopSellingProducts();

        // 3. [MỚI] Lấy dữ liệu biểu đồ doanh thu 6 tháng
        List<OrderService.RevenueChartDTO> revenueData = orderService.getRevenueLast6Months();

        // 4. Add to Model
        model.addAttribute("pageTitle", "Trang Chủ Quản Trị");
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("pendingOrders", pendingOrders);

        // Truyền dữ liệu xuống JavaScript
        model.addAttribute("topSellingProducts", topSellingProducts);
        model.addAttribute("revenueData", revenueData);

        return "admin/home";
    }
}