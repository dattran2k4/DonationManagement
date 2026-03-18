package com.chiaseyeuthuong.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class AdminDashboardSummaryResponse {

    private BigDecimal totalConfirmedDonationAmount;
    private long ongoingEventCount;
    private long newDonorsToday;
}
