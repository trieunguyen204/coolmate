package com.nhom10.coolmate.comment;

import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.product.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/user/comments")
public class CommentController {

    private final CommentService commentService;
    private final ProductService productService;

    @Autowired
    public CommentController(CommentService commentService, ProductService productService) {
        this.commentService = commentService;
        this.productService = productService;
    }

    @PostMapping("/add")
    public String addComment(
            @RequestParam("productId") Integer productId,
            @RequestParam("content") String content,
            @RequestParam("rate") Integer rate,
            @AuthenticationPrincipal User principal,
            RedirectAttributes ra) {

        // 1. Kiểm tra đăng nhập
        if (principal == null) {
            ra.addFlashAttribute("errorMessage", "Bạn cần đăng nhập để gửi bình luận.");
            return "redirect:/login";
        }

        // 2. Validate dữ liệu đầu vào
        if (content.trim().isEmpty() || rate == null || rate < 1 || rate > 5) {
            ra.addFlashAttribute("errorMessage", "Vui lòng nhập nội dung và chọn số sao (1-5).");
            // SỬA: Chuyển hướng về đúng URL /product/{id}
            return "redirect:/product/" + productId;
        }

        try {
            // 3. Lưu bình luận
            commentService.saveNewComment(productId, principal.getId(), content, rate);
            ra.addFlashAttribute("successMessage", "Đánh giá của bạn đã được gửi thành công!");
        } catch (RuntimeException e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }


        return "redirect:/product/" + productId;
    }
}