package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EDashboardPeriod;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class DonationTrendResponse {

    private EDashboardPeriod period;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<Long> eventIds;
    private List<DonationTrendPointResponse> points;
}
