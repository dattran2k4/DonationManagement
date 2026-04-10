package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.response.DonorWallItemResponse;
import com.chiaseyeuthuong.dto.response.DonorWallResponse;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.event.DonationConfirmedEvent;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.*;
import com.chiaseyeuthuong.repository.*;
import com.chiaseyeuthuong.service.AuditLogService;
import com.chiaseyeuthuong.service.DonationService;
import com.chiaseyeuthuong.service.DonationSpecification;
import jakarta.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DONATION-SERVICE")
public class DonationServiceImpl implements DonationService {
    private static final String WHOLE_AMOUNT_MESSAGE = "Chỗ này chưa code huhu, vui lòng nhập tiền chẳn";

    private final DonationRepository donationRepository;
    private final ActivityRepository activityRepository;
    private final EventRepository eventRepository;
    private final DonorRepository donorRepository;
    private final AuditLogService auditLogService;

    private final ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createWebDonation(DonationRequest request) {
        log.info("Processing create donation for donorId {}", request.getDonorId());

        Donation donation = new Donation();

        saveDonation(donation, request);

        donation.setDonationVia(EDonationVia.WEB);
        donation.setStatus(EDonationStatus.PENDING_PAYMENT);
        donation.setPaymentMethod(EPaymentMethod.BANK_TRANSFER_ONLINE);
        donation.setMemoCode(generateMemoCode());
        donation.setOrderCode(generatePaymentCode());

        Donation newDonation = donationRepository.save(donation);
        log.info("Donation saved {} from web", newDonation.getId());

        return newDonation.getMemoCode();
    }

    private Long generatePaymentCode() {
        long timestamp = Instant.now().getEpochSecond();
        int randomBits = ThreadLocalRandom.current().nextInt(1000, 10000);
        String orderCodeStr = String.valueOf(timestamp) + randomBits;
        return Long.parseLong(orderCodeStr);
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createStaffDonation(DonationRequest request, String username) {
        log.info("Processing create donation from staff {}", username);

        Donation donation = new Donation();

        saveDonation(donation, request);

        donation.setDonationVia(EDonationVia.STAFF);
        donation.setStatus(EDonationStatus.PENDING_APPROVED);
        donation.setPaymentMethod(request.getPaymentMethod());

        Donation result = donationRepository.save(donation);
        auditLogService.logCreate(
                EEntityType.DONATION,
                result.getId(),
                "Tạo mới khoản quyên góp nội bộ",
                buildDonationAuditMap(result)
        );
        log.info("Donation saved from staff {}", result.getId());
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStaffDonation(Long id, DonationRequest request) {
        log.info("Processing update donation from staff for id {}", id);

        Donation donation = getDonation(id);
        Map<String, Object> beforeValues = buildDonationAuditMap(donation);

        if (EDonationStatus.CONFIRMED.equals(donation.getStatus())) {
            throw new InvalidDataException("Không thể chỉnh sửa khoản quyên góp đã xác nhận");
        }
        if (!EDonationStatus.REJECTED.equals(donation.getStatus())
                && !EDonationStatus.PENDING_APPROVED.equals(donation.getStatus())) {
            throw new InvalidDataException("Chỉ được chỉnh sửa khoản quyên góp ở trạng thái Chờ duyệt hoặc Từ chối");
        }

        saveDonation(donation, request);
        donation.setPaymentMethod(request.getPaymentMethod());
        donation.setStatus(EDonationStatus.PENDING_APPROVED);

        Donation result = donationRepository.save(donation);
        auditLogService.logUpdate(
                EEntityType.DONATION,
                result.getId(),
                "Cập nhật khoản quyên góp nội bộ",
                beforeValues,
                buildDonationAuditMap(result)
        );
        log.info("Donation updated from staff {}", result.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitForApproval(Long id) {
        Donation donation = getDonation(id);
        if (!EDonationVia.STAFF.equals(donation.getDonationVia())) {
            throw new InvalidDataException("Chỉ hỗ trợ gửi duyệt với khoản quyên góp tạo nội bộ");
        }
        if (EDonationStatus.CONFIRMED.equals(donation.getStatus())) {
            throw new InvalidDataException("Khoản quyên góp đã xác nhận, không thể gửi duyệt");
        }

        Map<String, Object> beforeValues = buildDonationAuditMap(donation);
        donation.setStatus(EDonationStatus.PENDING_APPROVED);
        donation.setRejectionReason(null);
        Donation savedDonation = donationRepository.save(donation);

        auditLogService.logUpdate(
                EEntityType.DONATION,
                savedDonation.getId(),
                "Gửi duyệt khoản quyên góp nội bộ",
                beforeValues,
                buildDonationAuditMap(savedDonation)
        );
    }

    private void saveDonation(Donation donation, DonationRequest request) {
        validateWholeAmount(request.getAmount());

        Donor donor = donorRepository.findById(request.getDonorId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà hảo tâm"));
        donation.setDonor(donor);
        donation.setEvent(null);
        donation.setActivity(null);

        if (request.getActivityId() != null) {
            log.info("Processing activity {}", request.getActivityId());
            Activity activity = activityRepository.findById(request.getActivityId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hoạt động từ thiện"));
            Event parentEvent = activity.getEvent();
            if (parentEvent == null) {
                throw new InvalidDataException("Hoạt động không có sự kiện cha hợp lệ");
            }

            donation.setActivity(activity);
            donation.setEvent(parentEvent);
            donation.setTarget(EDonationTarget.ACTIVITY);
        } else if (request.getEventId() != null) {
            log.info("Processing event {}", request.getEventId());
            Event event = eventRepository.findById(request.getEventId()).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sự kiện từ thiện"));
            donation.setEvent(event);
            donation.setTarget(EDonationTarget.EVENT);
        } else {
            donation.setTarget(EDonationTarget.NONE);
        }

        donation.setAmount(request.getAmount());
        if (request.getDonatedAt() != null) {
            donation.setDonatedAt(request.getDonatedAt());
        }
        donation.setMessage(request.getMessage() != null && !request.getMessage().isBlank() ? request.getMessage().trim() : null);
        donation.setMemoCode(request.getMemoCode() != null && !request.getMemoCode().isBlank() ? request.getMemoCode().trim() : null);
        donation.setNeedReceipt(request.getNeedReceipt());
        if (Boolean.TRUE.equals(request.getNeedReceipt())) {
            if (request.getReceiptEmail() == null) {
                throw new InvalidDataException("Email không được để trống");
            }
            if (request.getReceiptName() == null) {
                throw new InvalidDataException("Tên không được để trống");
            }
            donation.setReceiptEmail(request.getReceiptEmail());
            donation.setReceiptName(request.getReceiptName());
        } else {
            donation.setReceiptEmail(null);
            donation.setReceiptName(null);
        }

        donation.setRejectionReason(null);
    }

    private void validateWholeAmount(BigDecimal amount) {
        if (amount == null) {
            return;
        }

        if (amount.stripTrailingZeros().scale() > 0) {
            throw new InvalidDataException(WHOLE_AMOUNT_MESSAGE);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeStatusDonation(EDonationStatus status, Long id) {
        if (status.equals(EDonationStatus.CONFIRMED)) {
            confirmDonation(id, null);
            return;
        }

        if (EDonationStatus.REJECTED.equals(status)) {
            throw new InvalidDataException("Vui lòng dùng API từ chối riêng và cung cấp lý do từ chối");
        }

        Donation donation = getDonation(id);
        donation.setStatus(status);
        donationRepository.save(donation);
        log.info("Donation updated status to {}", status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectDonation(Long id, String reason, String username) {
        Donation donation = getDonation(id);

        if (!EDonationVia.STAFF.equals(donation.getDonationVia())) {
            throw new InvalidDataException("Chỉ hỗ trợ từ chối khoản quyên góp tạo bởi nhân sự nội bộ");
        }

        if (!EDonationStatus.PENDING_APPROVED.equals(donation.getStatus())) {
            throw new InvalidDataException("Chỉ được từ chối khoản quyên góp đang ở trạng thái chờ duyệt");
        }

        String normalizedReason = reason == null ? "" : reason.trim();
        if (normalizedReason.isBlank()) {
            throw new InvalidDataException("Vui lòng nhập lý do từ chối");
        }

        Map<String, Object> beforeValues = buildDonationAuditMap(donation);

        donation.setStatus(EDonationStatus.REJECTED);
        donation.setRejectionReason(normalizedReason);
        Donation savedDonation = donationRepository.save(donation);

        auditLogService.logUpdate(EEntityType.DONATION, savedDonation.getId(),
                "Từ chối khoản quyên góp nội bộ", beforeValues, buildDonationAuditMap(savedDonation));

        log.info("Donation {} rejected by {}", savedDonation.getId(), username);
    }

    @Override
    public void confirmDonation(Long id, WebhookData webhookData) {
        Donation donation = getDonation(id);

        if (EDonationStatus.CONFIRMED.equals(donation.getStatus())) {
            log.info("Donation {} already confirmed. Skip confirm flow.", donation.getId());
            return;
        }

        donation.setStatus(EDonationStatus.CONFIRMED);
        donation.setConfirmedAt(LocalDateTime.now());
        donation.setDonatedAt(LocalDateTime.now());
        donationRepository.save(donation);

        applicationEventPublisher.publishEvent(new DonationConfirmedEvent(donation.getId(), webhookData));
        log.info("Donation {} confirmed and DonationConfirmedEvent published", donation.getId());
    }

    @Override
    public PageResponse<DonationResponse> getAllDonations(String search, EDonationStatus status, EDonationTarget target,
                                                          EDonationType type, EPaymentMethod paymentMethod,
                                                          BigDecimal minAmount, BigDecimal maxAmount, int page, int size) {
        log.info("Processing get all donations");

        int pageNumber = (page > 0) ? page - 1 : 0;

        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by("id").descending());

        Specification<Donation> specification = DonationSpecification.filterDonation(
                search, status, target, type, paymentMethod, minAmount, maxAmount
        );

        Page<Donation> donationPage = donationRepository.findAll(specification, pageable);

        List<DonationResponse> data = donationPage.stream().map(this::toResponse).toList();
        return PageResponse.<DonationResponse>builder()
                .page(pageNumber + 1)
                .pageSize(size)
                .totalPages(donationPage.getTotalPages())
                .totalItems(donationPage.getTotalElements())
                .data(data)
                .build();
    }

    @Override
    public Donation getDonation(Long id) {
        return donationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
    }

    @Override
    public DonationResponse getDonationResponseById(Long id) {
        return toResponse(getDonation(id));
    }

    @Override
    public Donation getDonationByMemoCode(String memoCode) {
        return donationRepository.findByMemoCode(memoCode).orElseThrow(() -> new ResourceNotFoundException("Donation not found"));
    }

    @Override
    public Donation getDonationByOrderCode(Long orderCode) {
        return donationRepository.findByOrderCode(orderCode).orElseThrow(() -> new ResourceNotFoundException("Donation not founded"));
    }

    @Override
    public BigDecimal getTotalConfirmedDonationsAmount() {
        return donationRepository.sumConfirmedDonationsAmount();
    }

    @Override
    public List<DonationResponse> getRecentDonationsByDonorId(Long donorId, int limit) {
        int pageSize = Math.max(limit, 1);
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by("createdAt").descending());
        return donationRepository.findByDonorIdOrderByCreatedAtDesc(donorId, pageable)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PageResponse<DonationResponse> getDonationsByEventId(Long eventId, int page, int size) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 10;

        Pageable pageable = PageRequest.of(pageNumber, safeSize, Sort.by("id").descending());
        Page<Donation> donationPage = donationRepository.findByEventScopeId(eventId, pageable);

        List<DonationResponse> data = donationPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DonationResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalPages(donationPage.getTotalPages())
                .totalItems(donationPage.getTotalElements())
                .data(data)
                .build();
    }

    @Override
    public PageResponse<DonationResponse> getDonationsByActivityId(Long activityId, int page, int size) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 10;

        Pageable pageable = PageRequest.of(pageNumber, safeSize, Sort.by("id").descending());
        Page<Donation> donationPage = donationRepository.findByActivityId(activityId, pageable);

        List<DonationResponse> data = donationPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DonationResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalPages(donationPage.getTotalPages())
                .totalItems(donationPage.getTotalElements())
                .data(data)
                .build();
    }

    @Override
    public DonorWallResponse getDonorWall(EDonorWallPeriod period, Integer year, Integer month) {
        EDonorWallPeriod safePeriod = period != null ? period : EDonorWallPeriod.MONTH;

        LocalDate now = LocalDate.now();
        int safeYear = year != null ? year : now.getYear();
        int safeMonth = month != null ? month : now.getMonthValue();

        if (safeMonth < 1 || safeMonth > 12) {
            throw new InvalidDataException("Tháng không hợp lệ");
        }

        LocalDate fromDate;
        LocalDate toDate;
        int quarter;
        if (EDonorWallPeriod.QUARTER.equals(safePeriod)) {
            quarter = ((safeMonth - 1) / 3) + 1;
            int firstMonthOfQuarter = (quarter - 1) * 3 + 1;
            fromDate = LocalDate.of(safeYear, firstMonthOfQuarter, 1);
            toDate = fromDate.plusMonths(3).minusDays(1);
        } else {
            quarter = ((safeMonth - 1) / 3) + 1;
            fromDate = LocalDate.of(safeYear, safeMonth, 1);
            toDate = fromDate.withDayOfMonth(fromDate.lengthOfMonth());
        }

        List<DonationRepository.DonorWallAggregation> aggregations = donationRepository.aggregateDonorWall(fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));

        List<DonorWallItemResponse> donors = mapDonorWallItems(aggregations);

        return DonorWallResponse.builder()
                .period(safePeriod)
                .periodLabel(buildPeriodLabel(safePeriod, safeYear, safeMonth, quarter))
                .year(safeYear)
                .month(safeMonth)
                .quarter(quarter)
                .fromDate(fromDate)
                .toDate(toDate)
                .donors(donors)
                .build();
    }

    private DonationResponse toResponse(Donation donation) {
        DonationResponse response = new DonationResponse();
        BeanUtils.copyProperties(donation, response);
        response.setDonorId(donation.getDonor() != null ? donation.getDonor().getId() : null);
        response.setDonorPhone(donation.getDonor() != null ? donation.getDonor().getPhone() : null);
        response.setDonorEmail(donation.getDonor() != null ? donation.getDonor().getEmail() : null);
        response.setEventId(resolveEventId(donation));
        response.setActivityId(donation.getActivity() != null ? donation.getActivity().getId() : null);
        response.setDonorName(donation.getDonor() != null ? donation.getDonor().getFullName() : null);
        response.setObjectName(getObjectName(donation, donation.getTarget()));
        response.setEventName(resolveEventName(donation));
        response.setActivityName(donation.getActivity() != null ? donation.getActivity().getName() : null);
        response.setParentEventName(donation.getActivity() != null && donation.getActivity().getEvent() != null
                ? donation.getActivity().getEvent().getName()
                : null);
        return response;
    }

    private Long resolveEventId(Donation donation) {
        if (donation.getEvent() != null) {
            return donation.getEvent().getId();
        }
        if (donation.getActivity() != null && donation.getActivity().getEvent() != null) {
            return donation.getActivity().getEvent().getId();
        }
        return null;
    }

    private String resolveEventName(Donation donation) {
        if (donation.getEvent() != null) {
            return donation.getEvent().getName();
        }
        if (donation.getActivity() != null && donation.getActivity().getEvent() != null) {
            return donation.getActivity().getEvent().getName();
        }
        return null;
    }

    private String getObjectName(Donation donation, EDonationTarget target) {
        if (target == EDonationTarget.ACTIVITY) {
            return donation.getActivity().getName();
        } else if (target == EDonationTarget.EVENT) {
            return donation.getEvent().getName();
        } else if (target == EDonationTarget.NONE) {
            return "";
        }
        return null;
    }

    private String generateMemoCode() {
        String prefix = "THN";
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("ddMM"));
        // Sinh chuỗi ngẫu nhiên 3 k≥ý tự (A-Z, 0-9)
        String randomPart = RandomStringUtils.randomAlphanumeric(3).toUpperCase();
        return prefix + datePart + randomPart;
    }

    private List<DonorWallItemResponse> mapDonorWallItems(List<DonationRepository.DonorWallAggregation> aggregations) {
        List<DonorWallItemResponse> items = new ArrayList<>();
        int rank = 1;
        for (DonationRepository.DonorWallAggregation aggregation : aggregations) {
            if (rank > 100) {
                break;
            }
            items.add(DonorWallItemResponse.builder()
                    .donorId(aggregation.getDonorId())
                    .displayName(resolveDonorDisplayName(aggregation.getDisplayName(), aggregation.getFullName()))
                    .totalAmount(aggregation.getTotalAmount())
                    .donationCount(aggregation.getDonationCount())
                    .rank(rank)
                    .build());
            rank++;
        }
        return items;
    }

    private String resolveDonorDisplayName(String displayName, String fullName) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        if (fullName != null && !fullName.isBlank()) {
            return fullName;
        }
        return "Nhà hảo tâm ẩn danh";
    }

    private String buildPeriodLabel(EDonorWallPeriod period, int year, int month, int quarter) {
        if (EDonorWallPeriod.QUARTER.equals(period)) {
            return "Quý %d/%d".formatted(quarter, year);
        }
        return "Tháng %02d/%d".formatted(month, year);
    }

    private Map<String, Object> buildDonationAuditMap(Donation donation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("amount", donation.getAmount());
        values.put("message", donation.getMessage());
        values.put("target", donation.getTarget() != null ? donation.getTarget().name() : null);
        values.put("eventId", donation.getEvent() != null ? donation.getEvent().getId() : null);
        values.put("eventName", donation.getEvent() != null ? donation.getEvent().getName() : null);
        values.put("activityId", donation.getActivity() != null ? donation.getActivity().getId() : null);
        values.put("activityName", donation.getActivity() != null ? donation.getActivity().getName() : null);
        values.put("donorId", donation.getDonor() != null ? donation.getDonor().getId() : null);
        values.put("donorName", donation.getDonor() != null ? donation.getDonor().getFullName() : null);
        values.put("needReceipt", donation.getNeedReceipt());
        values.put("receiptName", donation.getReceiptName());
        values.put("receiptEmail", donation.getReceiptEmail());
        values.put("paymentMethod", donation.getPaymentMethod() != null ? donation.getPaymentMethod().name() : null);
        values.put("donatedAt", donation.getDonatedAt());
        values.put("status", donation.getStatus() != null ? donation.getStatus().name() : null);
        values.put("rejectionReason", donation.getRejectionReason());
        values.put("donationVia", donation.getDonationVia() != null ? donation.getDonationVia().name() : null);
        return values;
    }
}
