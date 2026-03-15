package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SystemConfigItemRequest {

    @NotBlank(message = "Khóa cấu hình không được để trống")
    private String key;

    private String value;

    private String description;
}
