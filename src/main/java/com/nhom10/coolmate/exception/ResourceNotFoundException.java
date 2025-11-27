package com.nhom10.coolmate.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Đánh dấu để Spring Boot tự động trả về lỗi 404 NOT FOUND khi exception này bị ném
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    // Constructor cơ bản
    public ResourceNotFoundException(String message) {
        super(message);
    }

    // Constructor có thể thêm cause (nguyên nhân gốc)
    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    // Dùng cho trường hợp không tìm thấy tài nguyên theo ID cụ thể
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s không được tìm thấy với %s : '%s'", resourceName, fieldName, fieldValue));
    }
}