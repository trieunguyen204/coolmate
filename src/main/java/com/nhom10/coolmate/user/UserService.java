package com.nhom10.coolmate.user;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional; // <-- Bổ sung import này để giải quyết lỗi

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng với email: " + email));
    }

    public void registerNewUser(UserRegistrationDTO dto) {
        if(userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalStateException("Email đã được sử dụng.");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword()); // Không mã hóa mật khẩu
        user.setRole("ROLE_USER");
        // Giả định: Enabled mặc định là true hoặc được xử lý trong constructor của User
        // user.setEnabled(true);

        userRepository.save(user);
    }

    // Phương thức: Lấy tất cả người dùng (Cho trang Admin)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ========== PHƯƠNG THỨC MỚI CHO ADMIN ==========

    // 1. Lấy User theo ID
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    // 2. Lưu User (dùng cho Thêm mới từ Admin)
    public void saveUser(User user) {
        if(userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalStateException("Email đã được sử dụng.");
        }
        userRepository.save(user);
    }

    // 3. Cập nhật User (dùng cho Sửa)
    public void updateUser(User updatedUser) {
        User existingUser = userRepository.findById(updatedUser.getId())
                .orElseThrow(() -> new IllegalStateException("User not found with ID: " + updatedUser.getId()));

        // Cập nhật các trường được phép sửa từ Admin
        existingUser.setFullName(updatedUser.getFullName());
        existingUser.setEmail(updatedUser.getEmail());
        existingUser.setRole(updatedUser.getRole());
        // Giả định có trường enabled trong User.java
        // existingUser.setEnabled(updatedUser.getEnabled()); // Bỏ comment nếu có trường enabled

        // Không sửa password ở đây

        userRepository.save(existingUser);
    }

    // 4. Xóa User
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}