package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RejectDonationRequest {

    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    @Size(max = 1000, message = "Lý do từ chối không được vượt quá 1000 ký tự")
    private String reason;
}
