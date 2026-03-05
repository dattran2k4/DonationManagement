package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;

import java.math.BigDecimal;
import java.util.List;

public interface DonorService {

    long saveIndividualDonor(IndividualDonorRequest request);

    long saveOrganizeDonor(OrganizeDonorRequest request);

    PageResponse<DonorResponse> getAllDonor();

    long getDorCountByObjectId(Long objectId, EEntityType type);

    Integer getConfirmedDonationCount(Long donorId, EDonationStatus status);

    BigDecimal getConfirmedDonationTotalAmount(Long donorId, EDonationStatus status);
}
