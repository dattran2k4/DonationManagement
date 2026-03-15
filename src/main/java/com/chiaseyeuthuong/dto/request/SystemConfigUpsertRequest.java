package com.chiaseyeuthuong.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SystemConfigUpsertRequest {

    @Valid
    @NotEmpty(message = "Danh sách cấu hình không được rỗng")
    private List<SystemConfigItemRequest> configs;
}
