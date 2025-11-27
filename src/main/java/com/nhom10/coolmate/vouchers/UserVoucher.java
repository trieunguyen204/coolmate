package com.nhom10.coolmate.vouchers;

import com.nhom10.coolmate.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_vouchers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    @Column(name = "is_used")
    private Boolean isUsed = false; // Đánh dấu đã dùng hay chưa
}