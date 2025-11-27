package com.nhom10.coolmate.sizes;

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
@RequestMapping("/admin/sizes")
public class SizesController {

    @Autowired
    private SizesService sizesService;

    @GetMapping
    public String listSizes(Model model) {
        // Lấy danh sách
        List<Sizes> listSizes = sizesService.getAllSizes();
        model.addAttribute("sizes", listSizes); // 'sizes' là List

        // Form thêm mới: Dùng 'size' (số ít) để bind vào form
        if (!model.containsAttribute("size")) {
            model.addAttribute("size", new Sizes());
        }

        model.addAttribute("pageTitle", "Quản lý Kích thước");
        return "admin/sizes";
    }

    @PostMapping("/save")
    public String saveSizes(@Valid @ModelAttribute("size") Sizes sizes,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // Lưu lại lỗi và đối tượng để hiển thị lại form
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.size", result);
            redirectAttributes.addFlashAttribute("size", sizes);
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại dữ liệu!");
            return "redirect:/admin/sizes";
        }

        try {
            boolean isNew = (sizes.getId() == null);
            sizesService.saveSizes(sizes);
            redirectAttributes.addFlashAttribute("successMessage", isNew ? "Thêm mới thành công!" : "Cập nhật thành công!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("size", sizes);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi hệ thống: " + e.getMessage());
        }

        return "redirect:/admin/sizes";
    }

    @GetMapping("/edit/{id}")
    public String editSizes(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Sizes sizes = sizesService.getSizesById(id);
            // Đẩy đối tượng cần sửa sang trang list
            redirectAttributes.addFlashAttribute("size", sizes);
        } catch (ResourceNotFoundException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sizes";
    }

    @GetMapping("/delete/{id}")
    public String deleteSizes(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            sizesService.deleteSizes(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa size thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sizes";
    }
}