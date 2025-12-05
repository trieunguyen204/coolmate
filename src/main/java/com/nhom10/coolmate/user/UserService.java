package com.nhom10.coolmate.user;

import com.nhom10.coolmate.address.Address;
import com.nhom10.coolmate.address.AddressRepository;
import com.nhom10.coolmate.exception.AppException;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AddressRepository addressRepository; // Inject thêm cái này

    // --- LOGIC PROFILE ---


    // Hàm này trả về Entity User để dùng trong AddressController và UserController
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Không tìm thấy người dùng với email: " + email));
    }

    // 1. Lấy thông tin hiển thị lên form
    public ProfileUpdateDTO getCurrentUserProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found"));

        // Tìm địa chỉ mặc định (isDefault = 1)
        Optional<Address> defaultAddr = addressRepository.findByUserAndIsDefault(user, 1);

        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setGender(user.getGender());
        dto.setAvatarUrl(user.getAvatarUrl()); // Lấy avatar hiện tại

        if (defaultAddr.isPresent()) {
            dto.setDefaultAddressId(defaultAddr.get().getId());
        }
        return dto;
    }

    @Transactional
    public void updateProfile(String email, ProfileUpdateDTO dto) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found"));

        user.setFullName(dto.getFullName());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender());

        // --- 1. XỬ LÝ UPLOAD ẢNH ---
        if (dto.getAvatarFile() != null && !dto.getAvatarFile().isEmpty()) {
            try {
                String fileName = saveFile(dto.getAvatarFile());
                user.setAvatarUrl("/uploads/" + fileName); // Lưu đường dẫn vào DB
            } catch (Exception e) {
                throw new AppException("Lỗi khi lưu ảnh: " + e.getMessage());
            }
        }

        // --- 2. XỬ LÝ ĐỔI MẬT KHẨU (Giữ nguyên logic cũ) ---
        if (dto.getCurrentPassword() != null && !dto.getCurrentPassword().isEmpty()) {
            if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPassword())) {
                throw new AppException("Mật khẩu hiện tại không đúng.");
            }
            if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
                throw new AppException("Mật khẩu xác nhận không khớp.");
            }
            user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        }

        // --- 3. XỬ LÝ ĐỊA CHỈ MẶC ĐỊNH ---
        if (dto.getDefaultAddressId() != null) {
            // Reset tất cả về 0
            List<Address> addresses = addressRepository.findByUser(user);
            for (Address addr : addresses) {
                addr.setIsDefault(0); // Dùng 0 thay vì false
            }
            // Set cái mới thành 1
            Address newDefault = addressRepository.findById(dto.getDefaultAddressId())
                    .orElseThrow(() -> new AppException("Địa chỉ không tồn tại"));

            if(!newDefault.getUser().getId().equals(user.getId())) {
                throw new AppException("Không có quyền truy cập địa chỉ này");
            }

            newDefault.setIsDefault(1); // Dùng 1 thay vì true
            addressRepository.saveAll(addresses);
        }

        userRepository.save(user);
    }

    // --- HÀM HỖ TRỢ LƯU FILE ---
    private String saveFile(MultipartFile file) throws Exception {
        // Đường dẫn lưu file: src/main/resources/static/uploads
        // Lưu ý: Khi chạy thực tế có thể cần cấu hình external path để không mất ảnh khi restart
        String uploadDir = "src/main/resources/static/uploads";

        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = UUID.randomUUID().toString() + extension; // Tạo tên file ngẫu nhiên để tránh trùng

        Path filePath = uploadPath.resolve(newFileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        return newFileName;
    }

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