package com.nhom10.coolmate.user;
//DTO cho chức năng Đăng ký
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // <<< THÊM NoArgsConstructor

@Data
@AllArgsConstructor
@NoArgsConstructor // <<< THÊM CONSTRUCTOR RỖNG CHO FORM
public class RegisterRequest {
    @NotBlank(message = "Tên không được để trống")
    private String fullName;

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu phải có ít nhất 6 ký tự")
    private String password;

    private String phone;
    private Gender gender = Gender.Nam;
}