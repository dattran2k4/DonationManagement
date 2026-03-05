package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class OrganizeDonorRequest {

    @NotBlank(message = "Tên tổ chức không được để trống")
    private String name;

    @NotBlank(message = "Mã số thuế không được để trống")
    private String taxCode;

    @NotBlank(message = "Người đại diện không được để trống")
    private String representative;

    @NotBlank(message = "Số điện thoại liên hệ không được để trống")
    @Pattern(
            regexp = "^\\+?[0-9\\s\\-()]{7,20}$",
            message = "Định dạng số điện thoại không hợp lệ"
    )
    private String phone;

    @NotBlank(message = "Email liên hệ không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String billingAddress;

    private String note;

    private String referralSource;
}
