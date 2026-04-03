package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
public class DonorWallItemResponse {
    private Long donorId;
    private String displayName;
    private BigDecimal totalAmount;
    private Long donationCount;
    private Integer rank;
}
