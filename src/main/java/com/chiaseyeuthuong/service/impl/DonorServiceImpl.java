package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.DonorDonationHistoryResponse;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.OrganizationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Donor;
import com.chiaseyeuthuong.model.Organization;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.repository.DonorRepository;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.DonorSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DONOR-SERVICE")
public class DonorServiceImpl implements DonorService {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;

    private static final String DONOR_NOT_FOUND_MESSAGE = "Không tìm thấy nhà hảo tâm";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveIndividualDonor(IndividualDonorRequest request) {
        log.info("Processing saving donor for donor phone: {}", request.getPhone());

        Donor donor = donorRepository.findByPhone(request.getPhone())
                .orElseGet(Donor::new);
        toIndividualEntity(donor, request);

        Donor newDonor = donorRepository.save(donor);
        log.info("Individual Donor saved successfully with id={}", newDonor.getId());

        return newDonor.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveOrganizeDonor(OrganizeDonorRequest request) {
        Donor donor = donorRepository.findByPhone(request.getPhone())
                .orElseGet(Donor::new);
        toOrganizationEntity(donor, request);

        Donor result = donorRepository.save(donor);

        log.info("Organization Donor saved successfully with id={}", result.getId());
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long updateIndividualDonor(Long id, IndividualDonorRequest request) {
        Donor donor = donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DONOR_NOT_FOUND_MESSAGE));

        toIndividualEntity(donor, request);

        Donor result = donorRepository.save(donor);
        log.info("Individual Donor updated successfully with id={}", result.getId());
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long updateOrganizeDonor(Long id, OrganizeDonorRequest request) {
        Donor donor = donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DONOR_NOT_FOUND_MESSAGE));
        toOrganizationEntity(donor, request);

        Donor result = donorRepository.save(donor);
        log.info("Organization Donor updated successfully with id={}", result.getId());
        return result.getId();
    }

    @Override
    public DonorResponse getDonorById(Long id) {
        Donor donor = donorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(DONOR_NOT_FOUND_MESSAGE));
        return toResponse(donor);
    }

    @Override
    public PageResponse<DonorDonationHistoryResponse> getDonorDonations(Long donorId, int page, int size) {
        donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException(DONOR_NOT_FOUND_MESSAGE));

        int pageNumber = (page > 0) ? page - 1 : 0;
        PageRequest pageRequest = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<Donation> donationPage = donationRepository.findByDonorId(donorId, pageRequest);

        List<DonorDonationHistoryResponse> data = donationPage.stream()
                .map(this::toDonorDonationHistoryResponse)
                .toList();

        return PageResponse.<DonorDonationHistoryResponse>builder()
                .page(pageNumber + 1)
                .pageSize(size)
                .totalItems(donationPage.getTotalElements())
                .totalPages(donationPage.getTotalPages())
                .data(data)
                .build();
    }

    @Override
    public PageResponse<DonorResponse> getAllDonor(int page, int size, String search, EDonorType type) {

        int pageNumber = (page > 0) ? page - 1 : 0;
        PageRequest pageRequest = PageRequest.of(pageNumber, size, Sort.by(Sort.Direction.DESC, "id"));

        Specification<Donor> specification = DonorSpecification.filterDonor(search, type);

        Page<Donor> donorPage = donorRepository.findAll(specification, pageRequest);

        List<DonorResponse> response = donorPage.stream().map(this::toResponse).toList();

        return PageResponse.<DonorResponse>builder()
                .page(pageNumber + 1)
                .pageSize(size)
                .totalItems(donorPage.getTotalElements())
                .totalPages(donorPage.getTotalPages())
                .data(response)
                .build();
    }

    @Override
    public long getDorCountByObjectId(Long objectId, EEntityType type) {
        if (EEntityType.EVENT.equals(type)) {
            return donorRepository.countDonorByEventId(objectId);
        } else if (EEntityType.ACTIVITY.equals(type)) {
            return donorRepository.countDonorByActivityId(objectId);
        }
        return donorRepository.countDonor();
    }

    @Override
    public Integer getConfirmedDonationCount(Long donorId, EDonationStatus status) {
        return donationRepository.countByDonorIdAndStatus(donorId, EDonationStatus.CONFIRMED);
    }

    @Override
    public BigDecimal getConfirmedDonationTotalAmount(Long donorId, EDonationStatus status) {
        return donationRepository.sumAmountByDonorIdAndStatus(donorId, EDonationStatus.CONFIRMED);
    }

    private void toIndividualEntity(Donor donor, IndividualDonorRequest request) {
        donor.setType(EDonorType.INDIVIDUAL);
        donor.setFullName(request.getFullName());
        donor.setDisplayName(request.getDisplayName());
        donor.setPhone(request.getPhone());
        donor.setEmail(request.getEmail());
        donor.setReferralSource(request.getReferralSource());
        donor.setNote(request.getNote());
        donor.setOrganization(null);
    }

    private void toOrganizationEntity(Donor donor, OrganizeDonorRequest request) {
        Organization organization = (donor.getOrganization() != null) ? donor.getOrganization() : new Organization();

        donor.setType(EDonorType.ORGANIZATION);
        donor.setFullName(request.getName());
        donor.setDisplayName(request.getName());
        donor.setPhone(request.getPhone());
        donor.setEmail(request.getEmail());
        donor.setReferralSource(request.getReferralSource());
        donor.setNote(request.getNote());

        organization.setName(request.getName());
        organization.setTaxCode(request.getTaxCode());
        organization.setRepresentative(request.getRepresentative());
        organization.setBillingAddress(request.getBillingAddress());
        donor.setOrganization(organization);
    }

    private DonorResponse toResponse(Donor donor) {
        DonorResponse response = new DonorResponse();
        BeanUtils.copyProperties(donor, response);
        if (donor.getOrganization() != null) {
            OrganizationResponse orgRes = new OrganizationResponse();
            BeanUtils.copyProperties(donor.getOrganization(), orgRes);
            response.setOrganization(orgRes);
        }
        response.setNumberOfDonations(getConfirmedDonationCount(donor.getId(), EDonationStatus.CONFIRMED));
        response.setTotalDonationAmount(getConfirmedDonationTotalAmount(donor.getId(), EDonationStatus.CONFIRMED));
        return response;
    }

    private DonorDonationHistoryResponse toDonorDonationHistoryResponse(Donation donation) {
        DonorDonationHistoryResponse response = new DonorDonationHistoryResponse();
        response.setDonationId(donation.getId());
        response.setDonationCode(donation.getMemoCode());
        response.setAmount(donation.getAmount());
        response.setStatus(donation.getStatus());
        response.setStatusLabel(getStatusLabel(donation.getStatus()));
        response.setTarget(donation.getTarget());
        response.setTargetLabel(donation.getTarget() != null ? donation.getTarget().getValue() : null);
        response.setDonatedAt(donation.getDonatedAt() != null ? donation.getDonatedAt() : donation.getCreatedAt());

        if (EDonationTarget.EVENT.equals(donation.getTarget()) && donation.getEvent() != null) {
            response.setTargetTitle(donation.getEvent().getName());
            response.setTargetUrl(donation.getEvent().getSlug() != null ? "/events/" + donation.getEvent().getSlug() : null);
        } else if (EDonationTarget.ACTIVITY.equals(donation.getTarget()) && donation.getActivity() != null) {
            response.setTargetTitle(donation.getActivity().getName());
            response.setTargetUrl(donation.getActivity().getSlug() != null ? "/activities/" + donation.getActivity().getSlug() : null);
        } else {
            response.setTargetTitle("Không gắn mục tiêu");
            response.setTargetUrl(null);
        }

        return response;
    }

    private String getStatusLabel(EDonationStatus status) {
        if (status == null) {
            return "Chưa xác định";
        }
        return switch (status) {
            case PENDING_PAYMENT -> "Chờ thanh toán";
            case PENDING_APPROVED -> "Chờ duyệt";
            case CONFIRMED -> "Đã xác nhận";
            case CANCELLED -> "Đã hủy";
            case REJECTED -> "Đã từ chối";
            case FAILED -> "Thất bại";
        };
    }
}
