package com.nhom10.coolmate.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import java.util.List; // Import cần thiết cho List<User>
import java.util.Optional; // <-- Bổ sung import để giải quyết lỗi "Cannot resolve symbol 'Optional'"


@Controller
public class UserController {

    @Autowired
    private UserService userService;

    // Login page
    @GetMapping("/login")
    public String loginPage() {
        return "user/login";
    }

    // Registration page
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("registrationDTO", new UserRegistrationDTO());
        return "user/register";
    }

    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDTO") UserRegistrationDTO dto,
                               BindingResult result, Model model) {
        if(result.hasErrors()) return "user/register";

        try {
            userService.registerNewUser(dto);
            return "redirect:/login?register_success";
        } catch(IllegalStateException e) {
            result.rejectValue("email", "error.user", e.getMessage());
            return "user/register";
        }
    }

    // User home
    @GetMapping("/")
    public String userHome() {
        return "/user/home";
    }

    //=================== ADMIN START ====================

    // Admin index page - Chuyển hướng đến home nếu Admin truy cập /admin
    @GetMapping("/admin")
    public String adminIndex() {
        return "redirect:/admin/home";
    }

    // Admin home
    @GetMapping("/admin/home")
    public String adminHome() {
        return "admin/home";
    }

    // Lấy danh sách người dùng để hiển thị trên trang Admin
    @GetMapping("/admin/users")
    public String listUsers(Model model) {
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);
        // Thêm User object rỗng cho Form Thêm mới (dùng trong Modal của user.html)
        model.addAttribute("newUser", new User());
        return "admin/user";
    }

    // 1. Xem Chi Tiết Khách Hàng (Dùng AJAX/Fetch từ user.html cho cả xem và sửa)
    @GetMapping("/admin/users/detail/{id}")
    @ResponseBody // Trả về JSON/data cho AJAX/Fetch
    public Optional<User> detailUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // 2. Xử lý Thêm Mới Khách Hàng
    @PostMapping("/admin/users/add")
    public String addUser(@Valid @ModelAttribute("newUser") User user,
                          BindingResult result) {
        // Xử lý lỗi validation (thường không lý tưởng khi dùng modal, nhưng đây là cách redirect cơ bản)
        if (result.hasErrors()) {
            return "redirect:/admin/users?error_validation";
        }
        try {
            userService.saveUser(user);
            return "redirect:/admin/users?success_add";
        } catch (IllegalStateException e) {
            // Xử lý Email đã tồn tại
            return "redirect:/admin/users?error_exist";
        }
    }



    // 4. POST: Cập Nhật Khách Hàng (Dùng cho Modal Sửa, POST trực tiếp từ JS)
    @PostMapping("/admin/users/update/{id}")
    public String updateUser(@PathVariable Long id,
                             @Valid @ModelAttribute("user") User user,
                             BindingResult result) {
        if (result.hasErrors()) {
            // Khi dùng modal, redirect về trang list và báo lỗi chung
            return "redirect:/admin/users?error_update";
        }
        user.setId(id); // Đảm bảo ID được thiết lập lại cho việc cập nhật
        userService.updateUser(user);
        return "redirect:/admin/users?success_update";
    }

    // 5. Xử lý Xóa Khách Hàng
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return "redirect:/admin/users?success_delete";
    }

    //=================== ADMIN END ====================
}