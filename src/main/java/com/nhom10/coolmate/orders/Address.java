package com.nhom10.coolmate.orders;

import com.nhom10.coolmate.user.User; // Import User
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Entity
@Table(name = "addresses")
@Data
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "full_name")
    private String fullName;

    private String phone;
    private String address;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    // QUAN HỆ: Nhiều Address thuộc về 1 User
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Khóa ngoại trỏ về bảng users
    @ToString.Exclude
    private User user;
}