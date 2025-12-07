package com.nhom10.coolmate.comment;

import com.nhom10.coolmate.user.User;
import com.nhom10.coolmate.product.Product;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList; // Thêm import này
import java.util.List;

@Entity
@Table(name = "comments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Bình luận thuộc sản phẩm nào
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Người dùng nào bình luận
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Nội dung bình luận
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // Số sao (1–5) (Admin trả lời có thể để null hoặc không set)
    private Integer rate;

    // Bình luận cha (nếu đây là reply)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Comment parent;

    // Danh sách các reply của bình luận
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default // Quan trọng để Builder không set list này thành null
    private List<Comment> replies = new ArrayList<>();

    // Đánh dấu reply này là của admin
    @Column(name = "is_admin_reply")
    @Builder.Default
    private Boolean isAdminReply = false;

    // Thời gian tạo
    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    // Tự động set thời gian tạo
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Timestamp.valueOf(LocalDateTime.now());
        }
    }
}