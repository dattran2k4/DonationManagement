package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.OrganizationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.model.Donor;
import com.chiaseyeuthuong.model.Organization;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.repository.DonorRepository;
import com.chiaseyeuthuong.service.DonorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveIndividualDonor(IndividualDonorRequest request) {
        log.info("Processing saving donor for donor phone: {}", request.getPhone());

        Donor donor = donorRepository.findByPhone(request.getPhone())
                .orElseGet(Donor::new);
        donor.setFullName(request.getFullName());
        donor.setPhone(request.getPhone());
        donor.setReferralSource(request.getReferralSource());
        donor.setDisplayName(request.getDisplayName());
        donor.setEmail(request.getEmail());
        donor.setOrganization(null);
        donor.setNote(request.getNote());
        donor.setType(EDonorType.INDIVIDUAL);

        Donor newDonor = donorRepository.save(donor);
        log.info("Individual Donor saved successfully with id={}", newDonor.getId());

        return newDonor.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveOrganizeDonor(OrganizeDonorRequest request) {
        Donor donor = donorRepository.findByPhone(request.getPhone())
                .orElseGet(Donor::new);

        Organization organization = (donor.getOrganization() != null) ? donor.getOrganization() : new Organization();

        donor.setType(EDonorType.ORGANIZATION);
        donor.setFullName(request.getName());
        donor.setPhone(request.getPhone());
        donor.setDisplayName(request.getName());
        donor.setReferralSource(request.getReferralSource());
        donor.setEmail(request.getEmail());
        donor.setNote(request.getNote());

        organization.setName(request.getName());
        organization.setTaxCode(request.getTaxCode());
        organization.setRepresentative(request.getRepresentative());
        organization.setBillingAddress(request.getBillingAddress());

        donor.setOrganization(organization);

        Donor result = donorRepository.save(donor);

        log.info("Organization Donor saved successfully with id={}", result.getId());
        return result.getId();
    }

    @Override
    public PageResponse<DonorResponse> getAllDonor() {

        PageRequest pageRequest = PageRequest.of(0, 10);

        Page<Donor> donorPage = donorRepository.findAll(pageRequest);

        List<DonorResponse> response = donorPage.stream().map(this::toResponse).toList();

        return PageResponse.<DonorResponse>builder()
                .page(0)
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
}
