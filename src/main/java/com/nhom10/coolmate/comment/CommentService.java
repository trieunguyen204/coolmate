package com.nhom10.coolmate.comment;

import com.nhom10.coolmate.product.Product;
import com.nhom10.coolmate.product.ProductRepository;
import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;

@Service
@RequiredArgsConstructor // Sử dụng Lombok để tự động inject dependency (thay cho constructor thủ công)
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // --- 1. Lấy danh sách comment (Chỉ lấy cha, con sẽ được load tự động) ---
    public List<Comment> getCommentsByProductId(Integer productId) {
        // Lưu ý: Repository phải có hàm này
        return commentRepository.findByProductIdAndParentIsNullOrderByCreatedAtDesc(productId);
    }

    // --- 2. Tính điểm đánh giá trung bình ---
    public Double getAverageRatingByProductId(Integer productId) {

        List<Comment> comments = commentRepository.findAllByProductIdOrderByCreatedAtDesc(productId);

        if (comments.isEmpty()) {
            return 0.0;
        }

        double sumOfRatings = comments.stream()
                .filter(c -> c.getRate() != null && c.getRate() > 0) // Chỉ tính comment có rate (bỏ qua admin reply)
                .mapToInt(Comment::getRate)
                .sum();

        long validCount = comments.stream()
                .filter(c -> c.getRate() != null && c.getRate() > 0)
                .count();

        if (validCount == 0) {
            return 0.0;
        }

        double average = sumOfRatings / validCount;
        return Math.round(average * 10.0) / 10.0; // Làm tròn 1 chữ số thập phân
    }

    // --- 3. User thêm bình luận mới ---
    @Transactional
    public Comment saveNewComment(Integer productId, Integer userId, String content, Integer rate) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Comment comment = Comment.builder()
                .product(product)
                .user(user)
                .content(content)
                .rate(rate > 5 || rate < 0 ? null : rate)
                .isAdminReply(false) // Mặc định user thường không phải admin reply
                .parent(null)        // Bình luận gốc không có cha
                // createdAt được xử lý bởi @PrePersist trong Entity
                .build();

        return commentRepository.save(comment);
    }

    // --- 4. Admin trả lời bình luận (ĐÃ SỬA LOGIC LÀM PHẲNG) ---
    @Transactional
    public void replyToComment(Integer parentId, Integer adminUserId, String content) {
        // 1. Tìm comment cha hiện tại (có thể là gốc hoặc là reply)
        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Bình luận gốc không tồn tại hoặc đã bị xóa"));

        // 2. LOGIC QUAN TRỌNG: Tìm Comment Gốc (Root)
        // Nếu parentComment đã có cha (nó là reply), thì lấy cha của nó.
        // Nếu không (nó là gốc), thì lấy chính nó.
        Comment rootComment = (parentComment.getParent() != null) ? parentComment.getParent() : parentComment;

        // 3. Tìm user admin đang đăng nhập
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin User không tồn tại"));

        // 4. Tạo reply gắn vào Root
        Comment reply = Comment.builder()
                .product(rootComment.getProduct())   // Gắn vào cùng sản phẩm
                .user(adminUser)                     // Người trả lời là Admin
                .content(content)
                .parent(rootComment)                 // LUÔN LUÔN GẮN VÀO ROOT
                .isAdminReply(true)                  // Đánh dấu là Admin Reply
                .rate(null)
                .build();

        commentRepository.save(reply);
    }
    // --- 5. User trả lời bình luận (Reply) ---
    @Transactional
    public void userReplyToComment(Integer parentId, Integer userId, String content) {
        // 1. Tìm comment cha (Admin comment hoặc User comment gốc)
        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại"));

        // QUAN TRỌNG: Logic làm phẳng (Flattening)
        // Nếu parentId đang là một câu trả lời (cấp 2), ta sẽ gán cha của nó là cha gốc (cấp 1)
        // Để hiển thị tất cả trong cùng một luồng hội thoại dưới comment gốc.
        Comment rootComment = (parentComment.getParent() != null) ? parentComment.getParent() : parentComment;

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        Comment reply = Comment.builder()
                .product(rootComment.getProduct())
                .user(user)
                .content(content)
                .parent(rootComment) // Gán vào Root để hiển thị trong list replies
                .isAdminReply(false) // Đây là User trả lời
                .rate(null)
                .build();

        commentRepository.save(reply);
    }

    // ==========================================================
    // PHƯƠNG THỨC CHO ADMIN (QUẢN TRỊ VIÊN)
    // ==========================================================

    // 1. Lấy tất cả danh sách bình luận (cho trang Admin/Comments)
    public List<Comment> getAllComments() {
        return commentRepository.findAllByOrderByCreatedAtDesc();
    }

    // 2. Admin trả lời bình luận
    @Transactional
    public void replyToCommentAdmin(Integer parentId, Integer adminUserId, String content) {
        // Tìm comment gốc
        Comment parentComment = commentRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Bình luận gốc không tồn tại"));

        // Tìm tài khoản Admin đang thao tác
        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new RuntimeException("Admin User không tồn tại"));

        // Tạo comment trả lời
        Comment reply = Comment.builder()
                .product(parentComment.getProduct()) // Kế thừa Product từ cha
                .user(adminUser)                     // Người trả lời là Admin
                .content(content)
                .parent(parentComment)               // Gắn quan hệ cha-con
                .isAdminReply(true)                  // Đánh dấu là Admin Reply
                .rate(null)                          // Admin trả lời không cần chấm sao
                .build();

        commentRepository.save(reply);
    }

}