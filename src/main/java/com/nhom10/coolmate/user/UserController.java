package com.nhom10.coolmate.user;

import com.nhom10.coolmate.address.Address;
import com.nhom10.coolmate.address.AddressRepository;
import com.nhom10.coolmate.cart.CartService;
import com.nhom10.coolmate.category.CategoryRepository;
import com.nhom10.coolmate.exception.AppException;

import com.nhom10.coolmate.order.OrderDTO;
import com.nhom10.coolmate.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressRepository addressRepository;
    private final OrderService orderService;
    private final CategoryRepository categoryRepository;
    private final CartService cartService;


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

    @GetMapping("/access-denied")
    public String accessDeniedPage(Model model) {
        model.addAttribute("message", "Bạn không có quyền truy cập vào trang này.");
        return "/error/access-denied";
    }


    // --- TRANG HỒ SƠ CÁ NHÂN ---
    @GetMapping("/user/profile")
    public String showProfile(Model model, Principal principal) {
        String email = principal.getName();
        User user = userService.getUserEntityByEmail(email); // Viết thêm hàm này trong Service trả về Entity

        // Load dữ liệu ra form
        ProfileUpdateDTO profileDTO = userService.getCurrentUserProfile(email);

        // Load danh sách địa chỉ để hiển thị vào Dropdown
        List<Address> addresses = addressRepository.findByUser(user);

        model.addAttribute("profileDTO", profileDTO);
        model.addAttribute("addresses", addresses);
        model.addAttribute("userEmail", email); // Email disable
        model.addAttribute("activeTab", "profile"); // Để highlight menu

        return "user/profile";
    }

    @PostMapping("/user/profile/update")
    public String updateProfile(@ModelAttribute("profileDTO") ProfileUpdateDTO dto,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            userService.updateProfile(principal.getName(), dto);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật hồ sơ thành công!");
        } catch (AppException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/user/profile";
    }

    // --- THÊM: HIỂN THỊ LỊCH SỬ ĐƠN HÀNG ---
    @GetMapping("/user/my_orders")
    public String myOrders(Model model, Principal principal ) {
        if (principal == null) {
            return "redirect:/login";
        }

        // Lấy danh sách đơn hàng của user hiện tại
        List<OrderDTO> myOrders = orderService.getMyOrders(principal.getName());

        model.addAttribute("orders", myOrders);
        model.addAttribute("activeTab", "order"); // Để highlight sidebar menu


        return "user/my_orders"; // Trả về file html
    }



}