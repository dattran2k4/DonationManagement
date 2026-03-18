package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EDashboardPeriod;
import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.dto.response.AdminDashboardSummaryResponse;
import com.chiaseyeuthuong.dto.response.DonationTrendPointResponse;
import com.chiaseyeuthuong.dto.response.DonationTrendResponse;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.repository.DonorRepository;
import com.chiaseyeuthuong.service.DashboardService;
import com.chiaseyeuthuong.service.DonationService;
import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DonationService donationService;
    private final EventService eventService;
    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;

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

    @Override
    public DonationTrendResponse getDonationTrend(EDashboardPeriod period, List<Long> eventIds) {
        EDashboardPeriod safePeriod = period == null ? EDashboardPeriod.WEEK : period;
        LocalDate today = LocalDate.now();
        List<Long> selectedEventIds = eventIds == null ? List.of() : eventIds;

        List<Donation> donations = selectedEventIds.isEmpty()
                ? donationRepository.findAllByStatusAndTargetAndEventIsNotNull(EDonationStatus.CONFIRMED, EDonationTarget.EVENT)
                : donationRepository.findAllByStatusAndTargetAndEventIdIn(EDonationStatus.CONFIRMED, EDonationTarget.EVENT, selectedEventIds);

        return switch (safePeriod) {
            case MONTH -> buildDailyTrend(safePeriod, today.minusDays(29), today, selectedEventIds, donations);
            case QUARTER -> buildMonthlyTrend(safePeriod, today, 3, selectedEventIds, donations);
            case YEAR -> buildMonthlyTrend(safePeriod, today, 12, selectedEventIds, donations);
            case WEEK -> buildDailyTrend(safePeriod, today.minusDays(6), today, selectedEventIds, donations);
        };
    }

    private DonationTrendResponse buildDailyTrend(EDashboardPeriod period, LocalDate fromDate, LocalDate toDate, List<Long> eventIds, List<Donation> donations) {
        Map<String, DonationTrendPointResponse.DonationTrendPointResponseBuilder> buckets = new LinkedHashMap<>();
        DateTimeFormatter keyFormatter = DateTimeFormatter.ISO_DATE;
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");

        LocalDate cursor = fromDate;
        while (!cursor.isAfter(toDate)) {
            String key = cursor.format(keyFormatter);
            buckets.put(key, DonationTrendPointResponse.builder()
                    .key(key)
                    .label(cursor.format(labelFormatter))
                    .totalAmount(BigDecimal.ZERO));
            cursor = cursor.plusDays(1);
        }

        for (Donation donation : donations) {
            LocalDate donationDate = resolveDonationDate(donation);
            if (donationDate == null || donationDate.isBefore(fromDate) || donationDate.isAfter(toDate)) {
                continue;
            }

            String bucketKey = donationDate.format(keyFormatter);
            DonationTrendPointResponse.DonationTrendPointResponseBuilder current = buckets.get(bucketKey);
            if (current == null) continue;

            BigDecimal currentAmount = current.build().getTotalAmount().add(donation.getAmount());
            buckets.put(bucketKey, DonationTrendPointResponse.builder()
                    .key(bucketKey)
                    .label(donationDate.format(labelFormatter))
                    .totalAmount(currentAmount));
        }

        return DonationTrendResponse.builder()
                .period(period)
                .fromDate(fromDate)
                .toDate(toDate)
                .eventIds(eventIds)
                .points(buckets.values().stream().map(DonationTrendPointResponse.DonationTrendPointResponseBuilder::build).toList())
                .build();
    }

    private DonationTrendResponse buildMonthlyTrend(EDashboardPeriod period, LocalDate today, int monthCount, List<Long> eventIds, List<Donation> donations) {
        YearMonth endMonth = YearMonth.from(today);
        YearMonth startMonth = endMonth.minusMonths(monthCount - 1L);
        Map<String, DonationTrendPointResponse.DonationTrendPointResponseBuilder> buckets = new LinkedHashMap<>();
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("MM/yyyy");

        YearMonth cursor = startMonth;
        while (!cursor.isAfter(endMonth)) {
            String key = cursor.toString();
            buckets.put(key, DonationTrendPointResponse.builder()
                    .key(key)
                    .label(cursor.format(labelFormatter))
                    .totalAmount(BigDecimal.ZERO));
            cursor = cursor.plusMonths(1);
        }

        for (Donation donation : donations) {
            LocalDate donationDate = resolveDonationDate(donation);
            if (donationDate == null) continue;

            YearMonth donationMonth = YearMonth.from(donationDate);
            if (donationMonth.isBefore(startMonth) || donationMonth.isAfter(endMonth)) {
                continue;
            }

            String bucketKey = donationMonth.toString();
            DonationTrendPointResponse.DonationTrendPointResponseBuilder current = buckets.get(bucketKey);
            if (current == null) continue;

            BigDecimal currentAmount = current.build().getTotalAmount().add(donation.getAmount());
            buckets.put(bucketKey, DonationTrendPointResponse.builder()
                    .key(bucketKey)
                    .label(donationMonth.format(labelFormatter))
                    .totalAmount(currentAmount));
        }

        return DonationTrendResponse.builder()
                .period(period)
                .fromDate(startMonth.atDay(1))
                .toDate(endMonth.atEndOfMonth())
                .eventIds(eventIds)
                .points(buckets.values().stream().map(DonationTrendPointResponse.DonationTrendPointResponseBuilder::build).toList())
                .build();
    }

    private LocalDate resolveDonationDate(Donation donation) {
        if (donation.getConfirmedAt() != null) {
            return donation.getConfirmedAt().toLocalDate();
        }
        if (donation.getDonatedAt() != null) {
            return donation.getDonatedAt().toLocalDate();
        }
        return donation.getCreatedAt() != null ? donation.getCreatedAt().toLocalDate() : null;
    }

}
