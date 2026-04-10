package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonationType;
import com.chiaseyeuthuong.common.EDonorWallPeriod;
import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.response.DonorWallResponse;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.model.Donation;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.util.List;

public interface DonationService {

    String createWebDonation(DonationRequest request);

    long createStaffDonation(DonationRequest request, String username);

    void updateStaffDonation(Long id, DonationRequest request);

    void submitForApproval(Long id);

    void changeStatusDonation(EDonationStatus status, Long id);

    void rejectDonation(Long id, String reason, String username);

    void confirmDonation(Long id, WebhookData webhookData);

    PageResponse<DonationResponse> getAllDonations(String search, EDonationStatus status, EDonationTarget target,
                                                   EDonationType type, EPaymentMethod paymentMethod,
                                                   BigDecimal minAmount, BigDecimal maxAmount, int page, int size);

    DonationResponse getDonationResponseById(Long id);

    Donation getDonation(Long id);

    Donation getDonationByMemoCode(String memoCode);

    Donation getDonationByOrderCode(Long orderCode);

    BigDecimal getTotalConfirmedDonationsAmount();

    List<DonationResponse> getRecentDonationsByDonorId(Long donorId, int limit);

    PageResponse<DonationResponse> getDonationsByEventId(Long eventId, int page, int size);

    PageResponse<DonationResponse> getDonationsByActivityId(Long activityId, int page, int size);

    DonorWallResponse getDonorWall(EDonorWallPeriod period, Integer year, Integer month);
}
