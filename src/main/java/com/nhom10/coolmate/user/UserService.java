package com.nhom10.coolmate.user;

import com.nhom10.coolmate.exception.AppException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Phương thức này được Spring Security gọi khi đăng nhập
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Email " + email + " không tìm thấy."));
    }

    // --- LOGIC ĐĂNG KÝ ---
    @Transactional
    public UserDTO register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException("Email đã tồn tại.");
        }

        User newUser = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(request.getPassword())
                .phone(request.getPhone())
                .role(Role.USER)
                .status(1)
                .gender(request.getGender() != null ? request.getGender() : Gender.Nam)
                .build();

        User savedUser = userRepository.save(newUser);
        return mapToDTO(savedUser);
    }


    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .gender(user.getGender())
                .build();
    }


    // --- LOGIC ADMIN CRUD ---

    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    public UserDTO getUserById(Integer id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng với ID: " + id));
        return mapToDTO(user);
    }

    @Transactional
    public void saveUserByAdmin(User user, String rawRole) {
        // 1. Xử lý Role: Chuyển đổi chuỗi ROLE_USER/ROLE_ADMIN từ HTML sang Enum USER/ADMIN
        Role roleToSet = Role.USER; // Giá trị mặc định
        if (rawRole != null) {
            String roleName = rawRole.replace("ROLE_", "");
            try {
                roleToSet = Role.valueOf(roleName);
            } catch (IllegalArgumentException e) {
                // Giữ giá trị mặc định nếu chuỗi không hợp lệ
            }
        }
        user.setRole(roleToSet);


        // 2. Kiểm tra logic Cập nhật hay Thêm mới
        if (user.getId() != null) {
            // --- CẬP NHẬT (Update) ---
            User existingUser = userRepository.findById(user.getId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng để cập nhật: ID " + user.getId()));

            // Cập nhật các trường:
            existingUser.setFullName(user.getFullName());
            existingUser.setEmail(user.getEmail());
            existingUser.setRole(user.getRole()); // Lấy Role đã được xử lý
            existingUser.setStatus(user.getStatus());

            // Chỉ cập nhật mật khẩu nếu Admin nhập mật khẩu mới
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(user.getPassword()));
            }

            userRepository.save(existingUser);
        } else {
            // --- THÊM MỚI (Create) ---
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new AppException("Email đã tồn tại.");
            }
            // Mã hóa mật khẩu
            if (user.getPassword() != null && !user.getPassword().isEmpty()) {
                user.setPassword(passwordEncoder.encode(user.getPassword()));
            } else {
                throw new AppException("Mật khẩu không được để trống khi thêm mới.");
            }
            // Set các giá trị mặc định cho user mới
            if (user.getGender() == null) user.setGender(Gender.Nam);
            if (user.getStatus() == null) user.setStatus(1); // Mặc định kích hoạt

            userRepository.save(user);
        }
    }

    @Transactional
    public void deleteUser(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy người dùng để xóa");
        }
        userRepository.deleteById(id);
    }


}