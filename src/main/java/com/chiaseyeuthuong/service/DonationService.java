package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonationType;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.model.Donation;

import java.math.BigDecimal;

public interface DonationService {

    String createWebDonation(DonationRequest request);

    void createStaffDonation(DonationRequest request, String username);

    void changeStatusDonation(EDonationStatus status, Long id);

    PageResponse<DonationResponse> getAllDonations(String search, EDonationStatus status, EDonationTarget target, EDonationType type, int page, int size);

    Donation getDonation(Long id);

    Donation getDonationByMemoCode(String memoCode);

    Donation getDonationByOrderCode(Long orderCode);

    BigDecimal getTotalConfirmedDonationsAmount();
}
