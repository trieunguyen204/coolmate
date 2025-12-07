package com.nhom10.coolmate.comment;

import com.nhom10.coolmate.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    // --- 1. Hiển thị trang danh sách toàn bộ bình luận (GET /admin/comments) ---
    // Phương thức này sử dụng: commentService.getAllComments()
    @GetMapping
    public String listComments(Model model) {
        List<Comment> comments = commentService.getAllComments();
        model.addAttribute("comments", comments);
        // Trả về file giao diện: templates/admin/comments.html
        return "admin/comments";
    }

    // --- 2. Xử lý Admin trả lời bình luận (POST /admin/comments/reply) ---
    // Phương thức này sử dụng: commentService.replyToComment()
    @PostMapping("/reply")
    public String replyToComment(
            @RequestParam("parentId") Integer parentId,
            @RequestParam("content") String content,
            @RequestParam(value = "productId", required = false) Integer productId,
            @RequestParam(value = "redirectUrl", required = false) String redirectUrl, // URL để quay lại (nếu có)
            @AuthenticationPrincipal User adminUser,
            RedirectAttributes ra) {

        // 1. Kiểm tra đăng nhập
        if (adminUser == null) {
            return "redirect:/login";
        }

        // 2. Validate nội dung
        if (content == null || content.trim().isEmpty()) {
            ra.addFlashAttribute("errorMessage", "Nội dung câu trả lời không được để trống!");
            return getRedirectPath(redirectUrl, productId);
        }

        try {
            // 3. Gọi service xử lý trả lời
            // Bạn có thể dùng replyToComment hoặc replyToCommentAdmin (vì logic trong Service giống hệt nhau)
            commentService.replyToComment(parentId, adminUser.getId(), content);

            ra.addFlashAttribute("successMessage", "Đã trả lời bình luận thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }

        // 4. Điều hướng về trang cũ
        return getRedirectPath(redirectUrl, productId);
    }

    // --- Helper: Xác định đường dẫn redirect ---
    private String getRedirectPath(String redirectUrl, Integer productId) {
        // Ưu tiên 1: Nếu có redirectUrl (từ trang admin list gửi lên) -> quay lại đó
        if (redirectUrl != null && !redirectUrl.isEmpty()) {
            return "redirect:" + redirectUrl;
        }
        // Ưu tiên 2: Nếu có productId (từ trang chi tiết sản phẩm gửi lên) -> quay lại trang sản phẩm
        if (productId != null) {
            return "redirect:/product/" + productId;
        }
        // Mặc định: Về trang danh sách comment
        return "redirect:/admin/comments";
    }
}