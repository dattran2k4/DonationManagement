package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.DonorDonationHistoryResponse;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;

import java.math.BigDecimal;

public interface DonorService {

    long saveIndividualDonor(IndividualDonorRequest request);

    long saveOrganizeDonor(OrganizeDonorRequest request);

    long updateIndividualDonor(Long id, IndividualDonorRequest request);

    long updateOrganizeDonor(Long id, OrganizeDonorRequest request);

    DonorResponse getDonorById(Long id);

    PageResponse<DonorDonationHistoryResponse> getDonorDonations(Long donorId, int page, int size);

    PageResponse<DonorResponse> getAllDonor(int page, int size, String search, EDonorType type);

    long getDorCountByObjectId(Long objectId, EEntityType type);

    Integer getConfirmedDonationCount(Long donorId, EDonationStatus status);

    BigDecimal getConfirmedDonationTotalAmount(Long donorId, EDonationStatus status);
}
