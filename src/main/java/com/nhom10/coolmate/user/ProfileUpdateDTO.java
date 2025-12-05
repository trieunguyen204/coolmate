package com.nhom10.coolmate.user;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import java.sql.Date;

@Data
public class ProfileUpdateDTO {
    private String fullName;
    private String phone;
    // private Date birthday; // Bỏ comment nếu bạn đã thêm cột này trong User
    private Gender gender;

    // Đổi mật khẩu
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    // Địa chỉ mặc định
    private Integer defaultAddressId;

    // --- PHẦN AVATAR MỚI ---
    private String avatarUrl; // Để hiển thị ảnh cũ
    private MultipartFile avatarFile; // Để nhận file ảnh mới upload lên
}