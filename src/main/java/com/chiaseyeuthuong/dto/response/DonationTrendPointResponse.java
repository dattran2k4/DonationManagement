package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DonationTrendPointResponse {

    private String key;
    private String label;
    private BigDecimal totalAmount;
}
