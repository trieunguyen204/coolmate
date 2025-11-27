package com.nhom10.coolmate.sizes;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size; // Import bình thường, không lo trùng tên class
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "sizes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Sizes { // Tên class là Sizes

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "Tên size không được để trống")
    @Size(max = 20, message = "Tên size tối đa 20 ký tự")
    @Column(name = "size_name", length = 20, unique = true, nullable = false)
    private String sizeName;
}