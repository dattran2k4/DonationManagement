package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EDashboardPeriod;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-DASHBOARD-CONTROLLER")
@RequestMapping("/api/dashboard")
public class ApiDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/donation-trend")
    public ApiResponse getDonationTrend(
            @RequestParam(required = false, defaultValue = "WEEK") EDashboardPeriod period,
            @RequestParam(required = false) List<Long> eventIds
    ) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy dữ liệu xu hướng quyên góp thành công")
                .data(dashboardService.getDonationTrend(period, eventIds))
                .build();
    }
}
