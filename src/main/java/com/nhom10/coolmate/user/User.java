package com.nhom10.coolmate.user;

import com.nhom10.coolmate.comment.Comment; // Import Entity Comment
import com.nhom10.coolmate.orders.Address; // Import Entity Address
import com.nhom10.coolmate.vouchers.UserVoucher;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements UserDetails {

    // 1. FIX LỖI InvalidClassException (Quan trọng)
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(nullable = false)
    private String role = "ROLE_USER"; // ROLE_USER hoặc ROLE_ADMIN

    @Column(columnDefinition = "tinyint default 1")
    private Integer status = 1;

    // 2. QUAN HỆ: 1 User có nhiều Address
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude // Tránh lỗi vòng lặp khi in log
    private List<Address> addresses = new ArrayList<>();

    // 3. QUAN HỆ: 1 User có nhiều Comment
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<Comment> comments = new ArrayList<>();

    // --- BỔ SUNG VÍ VOUCHER ---
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<UserVoucher> userVouchers = new ArrayList<>();

    // --- PHƯƠNG THỨC CỦA SPRING SECURITY ---
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return status == 1; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return status == 1; }
}