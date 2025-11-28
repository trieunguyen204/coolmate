package com.nhom10.coolmate.user;

import com.nhom10.coolmate.exception.AppException;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // --- HIỂN THỊ FORM ĐĂNG KÝ ---
    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        if (!model.containsAttribute("registrationDTO")) {
            model.addAttribute("registrationDTO", new RegisterRequest());
        }
        return "user/register"; // Trả về templates/user/register.html
    }

    // --- XỬ LÝ ĐĂNG KÝ ---
    @PostMapping("/register")
    public String register(
            @Valid @ModelAttribute("registrationDTO") RegisterRequest request,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.registrationDTO", bindingResult);
            redirectAttributes.addFlashAttribute("registrationDTO", request);
            return "redirect:/register";
        }

        try {
            userService.register(request);
            // Đăng ký thành công
            redirectAttributes.addAttribute("register_success", true);
            return "redirect:/login";
        } catch (AppException e) {
            // Đăng ký thất bại
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("registrationDTO", request);
            return "redirect:/register";
        }
    }

    // --- HIỂN THỊ FORM ĐĂNG NHẬP ---
    @GetMapping("/login")
    public String showLoginForm(Model model) {
        return "user/login";
    }

    // --- TRANG CHỦ USER ---
    @GetMapping({"/", "/user/home"})
    public String userHomePage(){
        return "user/home";
    }

    // --- TRANG CHỦ ADMIN ---
    @GetMapping({"/admin/", "/admin/home"})
    public String adminHomePage(){
        return "admin/home";
    }

    // ============================================================
    // QUẢN LÝ USER (CRUD CHO ADMIN)
    // ============================================================

    // 1. READ: Hiển thị danh sách khách hàng
    // LƯU Ý: Đây là phương thức duy nhất xử lý GET /admin/users
    @GetMapping("/admin/users")
    public String adminUsersPage(Model model) {
        List<UserDTO> users = userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("newUser", new User()); // Object cho modal Thêm mới

        // Trả về view: "admin/user" tương ứng với file templates/admin/user.html (hoặc user.html tùy cấu trúc folder của bạn)
        // Dựa vào code cũ của bạn, tôi để là "admin/user"
        return "admin/user";
    }


    // 2. CREATE: Thêm mới user từ Admin
    @PostMapping("/admin/users/add")
    public String addUser(
            @RequestParam("fullName") String fullName,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "role", required = false) String roleStr,
            @RequestParam(value = "enabled", required = false) boolean enabled,
            RedirectAttributes redirectAttributes) {
        try {
            // Tạo đối tượng User thủ công để truyền vào Service
            User newUser = User.builder()
                    .fullName(fullName)
                    .email(email)
                    .password(password)
                    .status(enabled ? 1 : 0)
                    // Không cần set role ở đây, Service sẽ xử lý từ roleStr
                    .build();

            // Gọi service lưu (truyền newUser và roleStr để xử lý chuỗi ROLE_USER)
            userService.saveUserByAdmin(newUser, roleStr);

            redirectAttributes.addFlashAttribute("successMessage", "Thêm người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // 3. READ DETAIL: API trả về JSON cho Modal Xem chi tiết và Modal Sửa
    @GetMapping("/admin/users/detail/{id}")
    @ResponseBody
    public ResponseEntity<UserDTO> getUserDetail(@PathVariable Integer id) {
        try {
            UserDTO userDTO = userService.getUserById(id);
            return ResponseEntity.ok(userDTO);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 4. UPDATE: Cập nhật thông tin
    @PostMapping("/admin/users/update/{id}")
    public String updateUser(@PathVariable Integer id,
                             @RequestParam("fullName") String fullName,
                             @RequestParam("email") String email,
                             // Mật khẩu là tùy chọn trong update
                             @RequestParam(value = "password", required = false) String password,
                             @RequestParam(value = "role", required = false) String roleStr,
                             @RequestParam(value = "enabled", required = false) boolean enabled,
                             RedirectAttributes redirectAttributes) {
        try {
            // Tạo đối tượng User thủ công
            User userUpdate = User.builder()
                    .id(id)
                    .fullName(fullName)
                    .email(email)
                    .password(password) // Có thể null hoặc rỗng
                    .status(enabled ? 1 : 0)
                    .build();

            userService.saveUserByAdmin(userUpdate, roleStr);

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thành công!");
        } catch (Exception e) {
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi cập nhật: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // 5. DELETE: Xóa user
    @GetMapping("/admin/users/delete/{id}")
    public String deleteUser(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            userService.deleteUser(id);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi xóa: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }
}