package com.chiaseyeuthuong.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DonorOrganizationRelationshipResponse {
    private Long id;
    private Long relatedDonorId;
    private String relatedDonorName;
    private String relatedDonorPhone;
    private String relatedDonorEmail;
    private String relatedDonorType;
    private Long roleTypeId;
    private String roleTypeCode;
    private String roleTypeName;
    private String note;
    private LocalDateTime updatedAt;
}
