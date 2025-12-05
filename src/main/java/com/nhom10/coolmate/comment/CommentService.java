package com.nhom10.coolmate.comment;

import com.nhom10.coolmate.product.Product;
import com.nhom10.coolmate.product.ProductRepository;
import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository; // Cần UserRepository để lấy thông tin User

    @Autowired
    public CommentService(CommentRepository commentRepository, ProductRepository productRepository, UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }


    public List<Comment> getCommentsByProductId(Integer productId) {
        return commentRepository.findAllByProductIdOrderByCreatedAtDesc(productId);
    }


    public Double getAverageRatingByProductId(Integer productId) {
        List<Comment> comments = getCommentsByProductId(productId);

        if (comments.isEmpty()) {
            return 0.0;
        }


        double sumOfRatings = comments.stream()
                .filter(c -> c.getRate() != null && c.getRate() > 0)
                .mapToInt(Comment::getRate)
                .sum();

        long validCount = comments.stream()
                .filter(c -> c.getRate() != null && c.getRate() > 0)
                .count();

        if (validCount == 0) {
            return 0.0;
        }

        double average = sumOfRatings / validCount;
        // Làm tròn đến 1 chữ số thập phân
        return Math.round(average * 10.0) / 10.0;
    }


    @Transactional
    public Comment saveNewComment(Integer productId, Integer userId, String content, Integer rate) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found with ID: " + productId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Tạo đối tượng Comment mới
        Comment comment = Comment.builder()
                .product(product)
                .user(user)
                .content(content)
                .rate(rate > 5 || rate < 0 ? null : rate) // Đảm bảo rate hợp lệ (0-5)
                .createdAt(new Timestamp(System.currentTimeMillis()))
                .build();

        return commentRepository.save(comment);
    }
}