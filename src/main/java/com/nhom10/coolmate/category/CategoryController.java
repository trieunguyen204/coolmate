package com.nhom10.coolmate.category;

import com.nhom10.coolmate.exception.AppException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // --- HIỂN THỊ TRANG CHỦ QUẢN LÝ DANH MỤC ---
    @GetMapping
    public String listCategories(Model model) {
        List<CategoryDTO> categories = categoryService.getAllCategories();
        model.addAttribute("categories", categories);
        model.addAttribute("categoryDTO", new CategoryDTO()); // DTO cho modal Thêm mới
        model.addAttribute("pageTitle", "Quản lý Danh mục");
        return "admin/categories"; // Trả về templates/admin/categories.html
    }

    // --- LẤY CHI TIẾT DANH MỤC (DÙNG CHO MODAL SỬA/CHI TIẾT AJAX) ---
    @GetMapping("/detail/{id}")
    @ResponseBody
    public ResponseEntity<CategoryDTO> getCategoryDetail(@PathVariable Integer id) {
        try {
            CategoryDTO categoryDTO = categoryService.getCategoryById(id);
            return ResponseEntity.ok(categoryDTO);
        } catch (AppException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // --- THÊM MỚI DANH MỤC ---
    @PostMapping("/add")
    public String addCategory(
            @Valid @ModelAttribute("categoryDTO") CategoryDTO categoryDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.categoryDTO", bindingResult);
            redirectAttributes.addFlashAttribute("categoryDTO", categoryDTO);
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại thông tin danh mục.");
            return "redirect:/admin/categories";
        }

        try {
            categoryService.createCategory(categoryDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục **" + categoryDTO.getName() + "** thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/categories";
    }

    // --- CẬP NHẬT DANH MỤC ---
    @PostMapping("/update/{id}")
    public String updateCategory(
            @PathVariable Integer id,
            @Valid @ModelAttribute("categoryDTO") CategoryDTO categoryDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {

            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin/categories";
        }

        try {
            categoryService.updateCategory(id, categoryDTO);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật danh mục **" + categoryDTO.getName() + "** thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/admin/categories";
    }

    // --- XÓA DANH MỤC ---
    @GetMapping("/delete/{id}")
    public String deleteCategory(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryService.deleteCategory(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa danh mục thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/categories";
    }
}