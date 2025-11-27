package com.nhom10.coolmate.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

@Data
public class UserRegistrationDTO {

    @NotEmpty(message = "Tên không được để trống")
    private String fullName; // Sửa tên trường

    @NotEmpty(message = "Email không được để trống")
    @Email(message = "Địa chỉ email không hợp lệ")
    private String email;

    @NotEmpty(message = "Mật khẩu không được để trống")
    private String password;
}