package com.nhom10.coolmate.controller;

import com.nhom10.coolmate.cart.CartService;
import com.nhom10.coolmate.category.CategoryDTO;
import com.nhom10.coolmate.category.CategoryService;
import com.nhom10.coolmate.product.ProductDTO; // Import ProductDTO
import com.nhom10.coolmate.product.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final CartService cartService;



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
    public String product(Model model,
                          @RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "priceRange", required = false) List<String> priceRanges,
                          // THÊM: Chấp nhận tham số sắp xếp
                          @RequestParam(value = "sortOrder", required = false) String sortOrder) {

        // Cập nhật tên hàm Service để truyền thêm tham số sắp xếp
        List<ProductDTO> products = productService.getFilteredProducts(keyword, priceRanges, sortOrder);

        // Truyền lại các tham số đã chọn để giữ trạng thái trên View
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedPriceRanges", priceRanges);
        model.addAttribute("sortOrder", sortOrder); // TRUYỀN THAM SỐ SẮP XẾP

        return "user/product";
    }


    @GetMapping("/products/by-category/{id}")
    public String productByCategory(@PathVariable Integer id, Model model) {
        model.addAttribute("products", productService.findByCategoryId(id));
        return "user/product";
    }

    @GetMapping("/user/about")
    public String about() { return "user/about"; }

    @GetMapping("/user/contact")
    public String contact() { return "user/contact"; }




    @GetMapping("/product/{id}")
    public String viewProductDetail(@PathVariable Integer id, Model model) {
        // Giả sử ProductService có getProductDTOById
        ProductDTO product = productService.getProductById(id);
        model.addAttribute("product", product);
        return "/user/product_detail"; // Trả về file này
    }

    @ModelAttribute("cartItemCount")
    public int getCartItemCount(HttpServletRequest request, HttpServletResponse response) {
        // Gọi hàm count mới trong Service
        return cartService.countItemsInCart(request, response);
    }
}