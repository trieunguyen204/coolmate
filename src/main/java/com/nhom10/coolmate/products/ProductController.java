package com.nhom10.coolmate.products;

import com.nhom10.coolmate.categories.CategoryService;
import com.nhom10.coolmate.sizes.SizesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/admin/products")
public class ProductController {

    @Autowired private ProductService productService;
    @Autowired private CategoryService categoryService;
    @Autowired private SizesService sizesService;

    @GetMapping
    public String listProducts(Model model, @RequestParam(value = "keyword", required = false) String keyword) {
        List<Product> products = (keyword != null) ? productService.searchProducts(keyword) : productService.getAllProducts();
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);

        // Chuẩn bị dữ liệu cho Modal Thêm mới (DTO rỗng)
        if (!model.containsAttribute("productDTO")) {
            model.addAttribute("productDTO", new ProductDTO());
            model.addAttribute("editMode", false); // Cờ báo hiệu đây là chế độ thêm
        }

        // Dữ liệu chung cho Dropdown/Bảng Stock
        model.addAttribute("categories", categoryService.getAllCategories());
        model.addAttribute("allSizes", sizesService.getAllSizes());
        model.addAttribute("pageTitle", "Quản lý Sản phẩm");

        return "admin/products";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute ProductDTO productDTO, RedirectAttributes redirectAttributes) {
        try {
            productService.saveProduct(productDTO);
            redirectAttributes.addFlashAttribute("successMessage", productDTO.getId() == null ? "Thêm mới thành công!" : "Cập nhật thành công!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi upload ảnh.");
        }
        return "redirect:/admin/products";
    }

    // --- LOGIC SỬA: Lấy dữ liệu cũ và mở Modal ---
    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            ProductDTO dto = productService.getProductDTOById(id);
            redirectAttributes.addFlashAttribute("productDTO", dto);
            redirectAttributes.addFlashAttribute("editMode", true); // Bật cờ editMode để View tự mở Modal
            return "redirect:/admin/products";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy sản phẩm.");
            return "redirect:/admin/products";
        }
    }

    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        productService.deleteProduct(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa sản phẩm.");
        return "redirect:/admin/products";
    }
}