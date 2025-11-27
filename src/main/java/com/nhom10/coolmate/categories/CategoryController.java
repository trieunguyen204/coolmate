package com.nhom10.coolmate.categories;

import com.nhom10.coolmate.exception.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/categories")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // 1. Hiển thị trang Quản lý Danh mục (GET /admin/categories)
    // Đã sửa: Sử dụng model.containsAttribute để giữ lại đối tượng 'category' (khi sửa/lỗi)
    @GetMapping
    public String listCategories(Model model,
                                 @RequestParam(value = "keyword", required = false) String keyword) {

        List<Category> listCategories;

        // Logic: Nếu có từ khóa thì tìm kiếm, ngược lại thì lấy tất cả
        if (keyword != null && !keyword.isEmpty()) {
            listCategories = categoryService.searchCategories(keyword);
        } else {
            listCategories = categoryService.getAllCategories();
        }

        model.addAttribute("categories", listCategories);
        model.addAttribute("keyword", keyword); // Truyền lại keyword ra view để giữ trong ô input

        // Logic giữ form thêm mới (như cũ)
        if (!model.containsAttribute("category")) {
            model.addAttribute("category", new Category());
        }

        model.addAttribute("pageTitle", "Quản lý Danh mục");

        return "admin/categories";
    }

    // 2. Xử lý Thêm mới và Chỉnh sửa (POST /admin/categories/save)
    @PostMapping("/save")
    public String saveCategory(@Valid @ModelAttribute("category") Category category,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            // ⭐ QUAN TRỌNG: Lưu lỗi validation và đối tượng category vào FlashAttributes để GET /admin/categories hiển thị lại form.
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.category", result);
            redirectAttributes.addFlashAttribute("category", category);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Vui lòng kiểm tra lại dữ liệu nhập!");
            return "redirect:/admin/categories";
        }

        try {
            boolean isNew = (category.getId() == null);
            categoryService.saveCategory(category);

            String message = isNew ? "Thêm mới Danh mục thành công!" : "Cập nhật Danh mục thành công!";
            redirectAttributes.addFlashAttribute("successMessage", message);
        } catch (IllegalArgumentException e) {
            // Lỗi trùng tên (từ Service)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Giữ lại đối tượng category để đổ dữ liệu vào form
            redirectAttributes.addFlashAttribute("category", category);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: Đã xảy ra lỗi khi lưu danh mục.");
        }

        return "redirect:/admin/categories";
    }


    @GetMapping("/edit/{id}")
    public String editCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            Category category = categoryService.getCategoryById(id);
            // ⭐ QUAN TRỌNG: Chuyển đối tượng category sang trang list để điền vào form chỉnh sửa
            redirectAttributes.addFlashAttribute("category", category);

            return "redirect:/admin/categories";
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/categories";
        }
    }

    // 4. Xử lý Xóa (GET /admin/categories/delete/{id})
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);

            redirectAttributes.addFlashAttribute("successMessage", "Đã đổi trạng thái danh mục ID " + id + " sang Ngừng hoạt động!");
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/categories";
    }
}