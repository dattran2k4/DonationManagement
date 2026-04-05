package com.chiaseyeuthuong.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class DonorPersonRelationshipResponse {
    private Long id;
    private Long relatedDonorId;
    private String relatedDonorName;
    private String relatedDonorDisplayName;
    private String relatedDonorPhone;
    private String relatedDonorEmail;
    private Long relationshipTypeId;
    private String relationshipTypeCode;
    private String relationshipTypeName;
    private String note;
    private LocalDateTime updatedAt;
}
