package com.chiaseyeuthuong.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonorPersonRelationshipRequest {

    @NotNull(message = "Vui lòng chọn nhà hảo tâm liên quan")
    private Long relatedDonorId;

    @NotNull(message = "Vui lòng chọn loại mối quan hệ")
    private Long relationshipTypeId;

    private String note;
}
