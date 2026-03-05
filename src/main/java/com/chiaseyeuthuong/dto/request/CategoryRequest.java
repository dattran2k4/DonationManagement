package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CategoryRequest {
    @Min(1)
    private Integer id;
    @NotBlank(message = "Tên danh mục không được để trống")
    private String name;
}
