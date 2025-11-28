package com.nhom10.coolmate.product;

import com.nhom10.coolmate.category.CategoryDTO;
import com.nhom10.coolmate.category.CategoryService;
import com.nhom10.coolmate.sizes.SizeDTO;
import com.nhom10.coolmate.sizes.SizesService;
import com.nhom10.coolmate.exception.AppException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/products")
public class ProductController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final SizesService sizesService;

    // Helper: Load dữ liệu cần thiết cho Form (Categories, Sizes)
    private void loadFormData(Model model) {
        List<CategoryDTO> categories = categoryService.getAllCategories();
        List<SizeDTO> allSizes = sizesService.getAllSizes();

        // Thêm option rỗng cho Category
        if (categories.isEmpty()) {
            categories = Collections.singletonList(CategoryDTO.builder().id(null).name("Chưa có danh mục").build());
        }

        model.addAttribute("categories", categories);
        model.addAttribute("allSizes", allSizes);
    }


    // --- 1. READ: Hiển thị danh sách sản phẩm ---
    @GetMapping
    public String listProducts(@RequestParam(value = "keyword", required = false) String keyword, Model model) {

        List<ProductDTO> products = productService.getAllProducts(keyword);
        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        model.addAttribute("pageTitle", "Quản lý Sản phẩm");

        // Khởi tạo DTO rỗng cho modal thêm mới
        if (!model.containsAttribute("productDTO")) {
            model.addAttribute("productDTO", ProductDTO.builder().build());
        }

        // Load data cho Form (dù là list hay form edit)
        loadFormData(model);

        return "admin/products";
    }

    @PostMapping("/save")
    public String saveProduct(
            @Valid @ModelAttribute("productDTO") ProductDTO productDTO,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi validation. Vui lòng kiểm tra lại các trường.");
            redirectAttributes.addFlashAttribute("productDTO", productDTO);
            redirectAttributes.addFlashAttribute("editMode", true);
            return "redirect:/admin/products";
        }

        try {
            productService.saveProduct(productDTO);
            String action = (productDTO.getId() == null) ? "Thêm mới" : "Cập nhật";
            redirectAttributes.addFlashAttribute("successMessage", action + " sản phẩm **" + productDTO.getName() + "** thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("productDTO", productDTO);
            redirectAttributes.addFlashAttribute("editMode", true);

            return "redirect:/admin/products";
        }

        return "redirect:/admin/products";
    }

    @GetMapping("/edit/{id}")
    public String editProduct(@PathVariable Integer id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ProductDTO productDTO = productService.getProductById(id);

            loadFormData(model);

            // Lấy tất cả Sizes để map ID
            List<SizeDTO> allSizes = (List<SizeDTO>) model.getAttribute("allSizes");

            // CHUYỂN ĐỔI VARIANTS HIỆN CÓ SANG VARIANTINPUTS CHO FORM MỚI
            List<ProductDTO.VariantInputDTO> inputs = productDTO.getProductVariants().stream()
                    .map(v -> ProductDTO.VariantInputDTO.builder()
                            .variantId(v.getVariantId())
                            .sizeId(allSizes.stream()
                                    .filter(s -> s.getSizeName().equals(v.getSizeName()))
                                    .findFirst()
                                    .map(SizeDTO::getId) // Đã sửa để tránh NullPointerException nếu Size Name không khớp
                                    .orElseThrow(() -> new AppException("Lỗi mapping Size ID: " + v.getSizeName())))
                            .color(v.getColor())
                            .stock(v.getStock())
                            .build())
                    .collect(Collectors.toList());

            productDTO.setVariantInputs(inputs);

            model.addAttribute("productDTO", productDTO);
            model.addAttribute("editMode", true);
            model.addAttribute("products", productService.getAllProducts(null));

            return "admin/products";

        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/admin/products";
        }
    }

    // --- 4. DELETE: Xóa sản phẩm ---
    @GetMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            productService.deleteProduct(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa sản phẩm thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/products";
    }
}