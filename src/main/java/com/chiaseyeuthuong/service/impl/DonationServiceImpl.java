package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.event.DonationConfirmedEvent;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.*;
import com.chiaseyeuthuong.repository.*;
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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DONATION-SERVICE")
public class DonationServiceImpl implements DonationService {
    private static final String WHOLE_AMOUNT_MESSAGE = "Chỗ này chưa code huhu, vui lòng nhập tiền chẳn";

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final EventRepository eventRepository;
    private final DonorRepository donorRepository;

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
        donation.setApprovalRequired(false);

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

        User staff = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Donation donation = new Donation();

        saveDonation(donation, request);

        donation.setDonationVia(EDonationVia.STAFF);
        donation.setStatus(EDonationStatus.PENDING_APPROVED);
        donation.setCreatedBy(staff);
        donation.setPaymentMethod(request.getPaymentMethod());
        
        Donation result = donationRepository.save(donation);
        log.info("Donation saved from staff {}", result.getId());
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStaffDonation(Long id, DonationRequest request) {
        log.info("Processing update donation from staff for id {}", id);

        Donation donation = getDonation(id);

        if (!EDonationVia.STAFF.equals(donation.getDonationVia())) {
            throw new InvalidDataException("Chỉ hỗ trợ chỉnh sửa khoản quyên góp được tạo nội bộ");
        }
        if (EDonationStatus.CONFIRMED.equals(donation.getStatus())) {
            throw new InvalidDataException("Không thể chỉnh sửa khoản quyên góp đã xác nhận");
        }

        saveDonation(donation, request);
        donation.setPaymentMethod(request.getPaymentMethod());

        Donation result = donationRepository.save(donation);
        log.info("Donation updated from staff {}", result.getId());
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

            donation.setActivity(activity);
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
        donation.setMessage(request.getMessage());
        donation.setNeedReceipt(request.getNeedReceipt());
        donation.setReceiptEmail(request.getReceiptEmail());
        donation.setReceiptName(request.getReceiptName());
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

        Donation donation = getDonation(id);
        donation.setStatus(status);
        donationRepository.save(donation);
        log.info("Donation updated status to {}", status);
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

    private DonationResponse toResponse(Donation donation) {
        DonationResponse response = new DonationResponse();
        BeanUtils.copyProperties(donation, response);
        response.setDonorId(donation.getDonor() != null ? donation.getDonor().getId() : null);
        response.setDonorPhone(donation.getDonor() != null ? donation.getDonor().getPhone() : null);
        response.setEventId(donation.getEvent() != null ? donation.getEvent().getId() : null);
        response.setActivityId(donation.getActivity() != null ? donation.getActivity().getId() : null);
        response.setDonorName(donation.getDonor().getFullName());
        response.setObjectName(getObjectName(donation, donation.getTarget()));
        return response;
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
}
