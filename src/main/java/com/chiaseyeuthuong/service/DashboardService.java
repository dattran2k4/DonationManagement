package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDashboardPeriod;
import com.chiaseyeuthuong.dto.response.AdminDashboardSummaryResponse;
import com.chiaseyeuthuong.dto.response.DonationTrendResponse;

import java.util.List;

public interface DashboardService {

    AdminDashboardSummaryResponse getAdminDashboardSummary();

    DonationTrendResponse getDonationTrend(EDashboardPeriod period, List<Long> eventIds);
}
