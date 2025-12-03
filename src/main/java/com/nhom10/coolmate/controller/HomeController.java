package com.nhom10.coolmate.controller;

import com.nhom10.coolmate.category.CategoryDTO;
import com.nhom10.coolmate.category.CategoryService;
import com.nhom10.coolmate.product.ProductDTO; // Import ProductDTO
import com.nhom10.coolmate.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam; // Import RequestParam

import java.util.List;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;


    @ModelAttribute("categories")
    public List<CategoryDTO> getCategories() {
        return categoryService.getAllCategories();
    }

    @ModelAttribute("cartItemCount")
    public int getCartItemCount() {
        return 0; // TODO: Cập nhật sau khi làm giỏ hàng
    }



    @GetMapping({"/", "/user/home", "/user/"})
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.getFeaturedProducts());
        return "user/home";
    }


    @GetMapping("/user/product")
    public String product(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        // 1. Gọi service lấy danh sách sản phẩm (có hỗ trợ tìm kiếm theo tên)
        List<ProductDTO> products = productService.getAllProducts(keyword);

        // 2. Đưa dữ liệu vào model
        model.addAttribute("products", products);

        // 3. Giữ lại từ khóa tìm kiếm để hiển thị trên ô input (nếu cần)
        model.addAttribute("keyword", keyword);

        return "user/product";
    }
    // --------------------------

    @GetMapping("/products/by-category/{id}")
    public String productByCategory(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findByCategoryId(id));
        return "user/product";
    }

    @GetMapping("/user/about")
    public String about() { return "user/about"; }

    @GetMapping("/user/contact")
    public String contact() { return "user/contact"; }

    @GetMapping("/user/profile")
    public String profile() { return "/user/profile"; }

    @GetMapping("/user/my_orders")
    public String myOrders(Model model) {
        return "/user/my_orders";
    }

    @GetMapping("/user/cart")
    public String cart(Model model) {
        return "/user/cart";
    }

    @GetMapping("/product/{id}")
    public String viewProductDetail(@PathVariable Integer id, Model model) {
        // Giả sử ProductService có getProductDTOById
        ProductDTO product = productService.getProductById(id);
        model.addAttribute("product", product);
        return "/user/product_detail"; // Trả về file này
    }
}