package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonorLookupDonationRequest {

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mã xác thực không được để trống")
    @Pattern(regexp = "\\d{6}", message = "Mã xác thực phải gồm đúng 6 chữ số")
    private String code;
}
