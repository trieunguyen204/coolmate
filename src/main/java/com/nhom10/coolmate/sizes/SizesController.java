package com.nhom10.coolmate.sizes;

import com.nhom10.coolmate.exception.AppException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/sizes")
public class SizesController {

    private final SizesService sizesService;

    // --- 1. READ: Hiển thị trang quản lý (Form Thêm mới/List) ---
    @GetMapping
    public String listSizes(Model model) {
        List<SizeDTO> sizes = sizesService.getAllSizes();
        model.addAttribute("sizes", sizes);

        // Khởi tạo DTO cho Form thêm mới
        if (!model.containsAttribute("size")) {
            model.addAttribute("size", new SizeDTO());
        }
        model.addAttribute("pageTitle", "Quản lý Kích thước");
        return "admin/sizes";
    }

    // --- 2. EDIT: Tải Size cần sửa vào Form ---
    @GetMapping("/edit/{id}")
    public String editSize(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            SizeDTO size = sizesService.getSizeById(id);
            model.addAttribute("size", size);
            model.addAttribute("pageTitle", "Cập nhật Kích thước");
            model.addAttribute("sizes", sizesService.getAllSizes()); // Vẫn hiển thị danh sách
            return "admin/sizes";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/sizes";
        }
    }


    // --- 3. SAVE: Xử lý Thêm mới và Cập nhật ---
    @PostMapping("/save")
    public String saveSize(
            @Valid @ModelAttribute("size") SizeDTO sizeDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {

        if (bindingResult.hasErrors()) {
            // Nếu có lỗi validation, flash attribute lỗi và redirect về trang list
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.size", bindingResult);
            redirectAttributes.addFlashAttribute("size", sizeDTO);

            // Nếu là lỗi validation, cần redirect lại để hiển thị form cũ (dùng flash attribute)
            return "redirect:/admin/sizes";
        }

        try {
            SizeDTO savedSize = sizesService.saveSize(sizeDTO);

            String action = (sizeDTO.getId() == null) ? "Thêm mới" : "Cập nhật";
            redirectAttributes.addFlashAttribute("successMessage", action + " Size **" + savedSize.getSizeName() + "** thành công!");
        } catch (AppException e) {
            // Lỗi nghiệp vụ (ví dụ: trùng tên)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            // Flash attribute dữ liệu đã nhập để giữ trên form
            redirectAttributes.addFlashAttribute("size", sizeDTO);
        }

        return "redirect:/admin/sizes";
    }

    // --- 4. DELETE: Xóa Size ---
    @GetMapping("/delete/{id}")
    public String deleteSize(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            sizesService.deleteSize(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa Size thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/sizes";
    }
}