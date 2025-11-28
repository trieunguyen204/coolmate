package com.nhom10.coolmate.util;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class FileUploadHelper {

    // Thư mục gốc cho file upload
    private static final Path UPLOAD_DIR = Paths.get("src/main/resources/static/uploads");

    static {
        // Đảm bảo thư mục tồn tại
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            System.err.println("Không thể tạo thư mục uploads: " + e.getMessage());
        }
    }

    /**
     * Lưu file đã upload vào thư mục chỉ định và trả về URL tương đối.
     *
     * @param file File MultipartFile từ request.
     * @param subDir Thư mục con (ví dụ: "products/").
     * @return Đường dẫn URL tương đối (ví dụ: "/uploads/products/uuid.jpg").
     * @throws IOException Nếu quá trình lưu thất bại.
     */
    public static String saveFile(MultipartFile file, String subDir) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        int i = originalFilename.lastIndexOf('.');
        if (i > 0) {
            extension = originalFilename.substring(i);
        }

        // Tạo tên file duy nhất
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        Path targetDir = UPLOAD_DIR.resolve(subDir);
        Files.createDirectories(targetDir); // Đảm bảo thư mục con tồn tại

        Path targetPath = targetDir.resolve(uniqueFileName);

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        // Trả về URL tương đối để lưu vào database
        return "/uploads/" + subDir.replace("\\", "/") + uniqueFileName;
    }
}