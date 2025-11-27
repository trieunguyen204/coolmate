package com.nhom10.coolmate.vouchers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/vouchers")
public class VoucherController {

    @Autowired
    private VoucherService voucherService;

    @GetMapping
    public String listVouchers(Model model) {
        List<Voucher> vouchers = voucherService.getAllVouchers();
        model.addAttribute("vouchers", vouchers);

        if (!model.containsAttribute("voucher")) {
            model.addAttribute("voucher", new Voucher());
        }

        model.addAttribute("pageTitle", "Quản lý Voucher");
        return "admin/vouchers";
    }

    @PostMapping("/save")
    public String saveVoucher(@Valid @ModelAttribute("voucher") Voucher voucher,
                              BindingResult result,
                              RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.voucher", result);
            redirectAttributes.addFlashAttribute("voucher", voucher);
            redirectAttributes.addFlashAttribute("errorMessage", "Vui lòng kiểm tra lại dữ liệu!");
            return "redirect:/admin/vouchers";
        }

        try {
            voucherService.saveVoucher(voucher);
            redirectAttributes.addFlashAttribute("successMessage", "Lưu Voucher thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("voucher", voucher);
        }
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/edit/{id}")
    public String editVoucher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            Voucher voucher = voucherService.getVoucherById(id);
            redirectAttributes.addFlashAttribute("voucher", voucher);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy voucher.");
        }
        return "redirect:/admin/vouchers";
    }

    @GetMapping("/delete/{id}")
    public String deleteVoucher(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            voucherService.deleteVoucher(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa voucher.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/vouchers";
    }
}