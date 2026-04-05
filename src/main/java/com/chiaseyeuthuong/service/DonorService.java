package com.chiaseyeuthuong.service;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.request.DonorOrganizationRelationshipRequest;
import com.chiaseyeuthuong.dto.request.DonorPersonRelationshipRequest;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.DonorDonationHistoryResponse;
import com.chiaseyeuthuong.dto.response.DonorOrganizationRelationshipResponse;
import com.chiaseyeuthuong.dto.response.DonorPersonRelationshipResponse;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.OrganizationRoleTypeResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.PersonRelationshipTypeResponse;

import java.math.BigDecimal;
import java.util.List;

public interface DonorService {

    long saveIndividualDonor(IndividualDonorRequest request);

    long saveOrganizeDonor(OrganizeDonorRequest request);

    long updateIndividualDonor(Long donorId, IndividualDonorRequest request);

    long updateOrganizeDonor(Long donorId, OrganizeDonorRequest request);

    PageResponse<DonorResponse> getAllDonor(int page, int size, String search, EDonorType type, String sortBy, String sortDir);

    DonorResponse getDonorById(Long donorId);

    List<PersonRelationshipTypeResponse> getActivePersonRelationshipTypes();

    List<OrganizationRoleTypeResponse> getActiveOrganizationRoleTypes();

    List<DonorPersonRelationshipResponse> getPersonRelationships(Long donorId);

    List<DonorOrganizationRelationshipResponse> getOrganizationRelationships(Long donorId);

    long createPersonRelationship(Long donorId, DonorPersonRelationshipRequest request);

    long updatePersonRelationship(Long donorId, Long relationshipId, DonorPersonRelationshipRequest request);

    void deactivatePersonRelationship(Long donorId, Long relationshipId);

    long createOrganizationRelationship(Long donorId, DonorOrganizationRelationshipRequest request);

    long updateOrganizationRelationship(Long donorId, Long relationshipId, DonorOrganizationRelationshipRequest request);

    void deactivateOrganizationRelationship(Long donorId, Long relationshipId);

    PageResponse<DonorDonationHistoryResponse> getDonorDonations(Long donorId, int page, int size);

    PageResponse<DonorDonationHistoryResponse> getDonorDonationsByEmail(String email, String code, int page, int size);

    PageResponse<DonorResponse> getDonorsByEventId(Long eventId, int page, int size);

    PageResponse<DonorResponse> getDonorsByActivityId(Long activityId, int page, int size);

    long getDorCountByObjectId(Long objectId, EEntityType type);

    Integer getConfirmedDonationCount(Long donorId, EDonationStatus status);

    BigDecimal getConfirmedDonationTotalAmount(Long donorId, EDonationStatus status);

    void sendLookupCodeIfEmailExists(String email);
}
