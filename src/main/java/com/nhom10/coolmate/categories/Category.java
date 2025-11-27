package com.nhom10.coolmate.categories;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 100, message = "Tên danh mục không được vượt quá 100 ký tự")
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    // Trạng thái: 1 - Hoạt động, 0 - Ngừng hoạt động
    @Column(name = "status", columnDefinition = "tinyint default 1")
    private Integer status = 1;

    // Hàm khởi tạo không có ID, dùng khi thêm mới
    public Category(String name, Integer status) {
        this.name = name;
        this.status = status;
    }
}