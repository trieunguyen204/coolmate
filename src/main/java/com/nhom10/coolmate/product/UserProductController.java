package com.nhom10.coolmate.product;

import com.nhom10.coolmate.cart.CartService;
import com.nhom10.coolmate.category.CategoryDTO;
import com.nhom10.coolmate.category.CategoryService;
import com.nhom10.coolmate.comment.Comment;
import com.nhom10.coolmate.comment.CommentService;
import com.nhom10.coolmate.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final CommentService commentService;
    private final CartService cartService;

    // --- BỔ SUNG: LOGIC TẢI DỮ LIỆU CHUNG (Navbar/Header) ---
    // (Phải khai báo lại ở đây vì Controller này tách biệt với HomeController)

    @ModelAttribute("categories")
    public List<CategoryDTO> getCategories() {
        return categoryService.getAllCategories();
    }

    @ModelAttribute("cartItemCount")
    public int getCartItemCount(HttpServletRequest request, HttpServletResponse response) {
        return cartService.countItemsInCart(request, response);
    }

    // --- 1. Trang chi tiết sản phẩm ---
    // Mapping: /product/{id}
    @GetMapping("/product/{id}")
    public String viewProductDetail(
            @PathVariable Integer id,
            Model model) {
        try {
            ProductDTO product = productService.getProductById(id);
            List<Comment> productComments = commentService.getCommentsByProductId(id);
            Double averageRating = commentService.getAverageRatingByProductId(id);

            model.addAttribute("product", product);
            model.addAttribute("productComments", productComments);
            model.addAttribute("averageRating", averageRating);
            model.addAttribute("pageTitle", product.getName());

            // Trả về view trong thư mục templates/user/
            return "user/product_detail";

        } catch (AppException e) {
            model.addAttribute("errorMessage", e.getMessage());
            return "redirect:/";
        }
    }

    // --- 2. Trang danh sách sản phẩm (Tìm kiếm / Lọc) ---
    // Mapping: /user/product
    @GetMapping("/user/product")
    public String listUserProducts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "priceRanges", required = false) List<String> priceRanges,
            @RequestParam(value = "minPriceInput", required = false) String minPriceInput, // <<<< THÊM
            @RequestParam(value = "maxPriceInput", required = false) String maxPriceInput, // <<<< THÊM
            @RequestParam(value = "sortOrder", required = false, defaultValue = "createdAt_desc") String sortOrder, // <<<< ĐỔI TÊN THAM SỐ
            Model model) {

        List<ProductDTO> products = productService.getFilteredProducts(keyword, priceRanges, minPriceInput, maxPriceInput, sortOrder);

        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedPriceRanges", priceRanges);
        model.addAttribute("minPriceInput", minPriceInput); // <<<< GÁN VÀO MODEL
        model.addAttribute("maxPriceInput", maxPriceInput); // <<<< GÁN VÀO MODEL
        model.addAttribute("sortOrder", sortOrder); // <<<< GÁN VÀO MODEL

        model.addAttribute("pageTitle", "Danh sách Sản phẩm");

        return "user/product";
    }
}