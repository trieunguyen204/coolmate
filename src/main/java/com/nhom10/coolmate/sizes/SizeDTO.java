package com.nhom10.coolmate.sizes;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SizeDTO {
    private Integer id;

    @NotBlank(message = "Tên size không được để trống")
    private String sizeName;
}