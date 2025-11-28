package com.nhom10.coolmate.vouchers;

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
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    // --- 1. READ: Hiển thị trang quản lý (Form Thêm mới/List) ---
    @GetMapping
    public String listVouchers(Model model) {
        List<VoucherDTO> vouchers = voucherService.getAllVouchers();
        model.addAttribute("vouchers", vouchers);

        // Khởi tạo DTO cho Form thêm mới
        if (!model.containsAttribute("voucher")) {
            model.addAttribute("voucher", VoucherDTO.builder().build());
        }
        model.addAttribute("pageTitle", "Quản lý Mã Giảm Giá");
        return "admin/vouchers";
    }

    // --- 2. EDIT: Tải Voucher cần sửa vào Form ---
    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            VoucherDTO voucher = voucherService.getVoucherById(id);
            model.addAttribute("voucher", voucher);
            model.addAttribute("pageTitle", "Cập nhật Mã Giảm Giá");
            model.addAttribute("vouchers", voucherService.getAllVouchers());
            return "admin/vouchers";
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/vouchers";
        }
    }


    // --- 3. SAVE: Xử lý Thêm mới và Cập nhật ---
    @PostMapping("/save")
    public String saveVoucher(
            @Valid @ModelAttribute("voucher") VoucherDTO voucherDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.voucher", bindingResult);
            redirectAttributes.addFlashAttribute("voucher", voucherDTO);
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi validation. Vui lòng kiểm tra lại form.");
            return "redirect:/admin/vouchers";
        }

        try {
            VoucherDTO savedVoucher = voucherService.saveVoucher(voucherDTO);

            String action = (voucherDTO.getId() == null) ? "Thêm mới" : "Cập nhật";
            redirectAttributes.addFlashAttribute("successMessage", action + " Voucher **" + savedVoucher.getCode() + "** thành công!");
        } catch (AppException e) {
            // Lỗi nghiệp vụ (ví dụ: trùng CODE)
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("voucher", voucherDTO);
        }

        return "redirect:/admin/vouchers";
    }

    // --- 4. DELETE: Xóa Voucher ---
    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucher(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa Voucher thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }
}