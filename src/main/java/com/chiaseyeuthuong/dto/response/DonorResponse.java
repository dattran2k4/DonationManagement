package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EDonorType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class DonorResponse {

    private Long id;

    private String fullName;

    private String phone;

    private String email;

    private String referralSource;

    private Integer numberOfDonations;

    private BigDecimal totalDonationAmount;

    private String note;

    private String createdBy;

    private EDonorType type;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private OrganizationResponse organization;
}
