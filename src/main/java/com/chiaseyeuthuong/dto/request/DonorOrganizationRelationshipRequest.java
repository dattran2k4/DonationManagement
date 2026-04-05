package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonorOrganizationRelationshipRequest {

    @NotNull(message = "Vui lòng chọn tổ chức liên quan")
    private Long organizationDonorId;

    @NotNull(message = "Vui lòng chọn vai trò trong tổ chức")
    private Long roleTypeId;

    private String note;
}
