package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.response.AdminDashboardSummaryResponse;
import com.chiaseyeuthuong.repository.DonorRepository;
import com.chiaseyeuthuong.service.DashboardService;
import com.chiaseyeuthuong.service.DonationService;
import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DonationService donationService;
    private final EventService eventService;
    private final DonorRepository donorRepository;

    @Override
    public AdminDashboardSummaryResponse getAdminDashboardSummary() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();

        return AdminDashboardSummaryResponse.builder()
                .totalConfirmedDonationAmount(donationService.getTotalConfirmedDonationsAmount())
                .ongoingEventCount(eventService.getEventCount(EEventStatus.ONGOING))
                .newDonorsToday(donorRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(startOfToday, startOfTomorrow))
                .build();
    }
}
