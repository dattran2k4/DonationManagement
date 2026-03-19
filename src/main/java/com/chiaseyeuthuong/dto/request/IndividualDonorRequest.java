package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class IndividualDonorRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Tên hiển thị trên website không được để trống")
    private String displayName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(
            regexp = "^\\+?\\d{10,11}$",
            message = "Số điện thoại phải có 10 hoặc 11 chữ số"
    )
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    private String note;

    private String referralSource;
}
