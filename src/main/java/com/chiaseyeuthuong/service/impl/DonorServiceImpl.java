package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.OrganizationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Donor;
import com.chiaseyeuthuong.model.Organization;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.repository.DonorRepository;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.DonorSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DONOR-SERVICE")
public class DonorServiceImpl implements DonorService {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveIndividualDonor(IndividualDonorRequest request) {
        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());
        log.info("Processing saving donor for donor phone: {}", phone);

        Donor donor = resolveDonor(phone, email);
        donor.setFullName(request.getFullName());
        donor.setPhone(phone);
        donor.setReferralSource(request.getReferralSource());
        donor.setDisplayName(request.getDisplayName());
        donor.setEmail(email);
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
        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());

        Donor donor = resolveDonor(phone, email);

        Organization organization = (donor.getOrganization() != null) ? donor.getOrganization() : new Organization();

        donor.setType(EDonorType.ORGANIZATION);
        donor.setFullName(request.getName());
        donor.setPhone(phone);
        donor.setDisplayName(request.getName());
        donor.setReferralSource(request.getReferralSource());
        donor.setEmail(email);
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
    @Transactional(rollbackFor = Exception.class)
    public long updateIndividualDonor(Long donorId, IndividualDonorRequest request) {
        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());

        Donor donor = getExistingDonor(donorId);
        validateDonorType(donor, EDonorType.INDIVIDUAL);
        validateUniqueContactForUpdate(donorId, phone, email);

        donor.setType(EDonorType.INDIVIDUAL);
        donor.setFullName(request.getFullName());
        donor.setDisplayName(request.getDisplayName());
        donor.setPhone(phone);
        donor.setEmail(email);
        donor.setReferralSource(request.getReferralSource());
        donor.setNote(request.getNote());

        Donor result = donorRepository.save(donor);
        log.info("Individual Donor updated successfully with id={}", result.getId());
        return result.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long updateOrganizeDonor(Long donorId, OrganizeDonorRequest request) {
        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());

        Donor donor = getExistingDonor(donorId);
        validateDonorType(donor, EDonorType.ORGANIZATION);
        validateUniqueContactForUpdate(donorId, phone, email);

        Organization organization = donor.getOrganization() != null ? donor.getOrganization() : new Organization();

        donor.setType(EDonorType.ORGANIZATION);
        donor.setFullName(request.getName());
        donor.setDisplayName(request.getName());
        donor.setPhone(phone);
        donor.setEmail(email);
        donor.setReferralSource(request.getReferralSource());
        donor.setNote(request.getNote());

        organization.setName(request.getName());
        organization.setTaxCode(request.getTaxCode());
        organization.setRepresentative(request.getRepresentative());
        organization.setBillingAddress(request.getBillingAddress());
        donor.setOrganization(organization);

        Donor result = donorRepository.save(donor);
        log.info("Organization Donor updated successfully with id={}", result.getId());
        return result.getId();
    }

    @Override
    public PageResponse<DonorResponse> getAllDonor(int page, int size, String search, EDonorType type, String sortBy, String sortDir) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        Specification<Donor> specification = DonorSpecification.filterDonor(search, type);
        List<DonorResponse> filteredDonors = donorRepository.findAll(specification)
                .stream()
                .map(this::toResponse)
                .sorted(buildDonorComparator(sortBy, sortDir))
                .toList();

        int safeSize = size > 0 ? size : 50;
        int totalItems = filteredDonors.size();
        int totalPages = totalItems == 0 ? 0 : (int) Math.ceil((double) totalItems / safeSize);
        int startIndex = Math.min(pageNumber * safeSize, totalItems);
        int endIndex = Math.min(startIndex + safeSize, totalItems);
        List<DonorResponse> response = filteredDonors.subList(startIndex, endIndex);

        return PageResponse.<DonorResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalItems(totalItems)
                .totalPages(totalPages)
                .data(response)
                .build();
    }

    @Override
    public DonorResponse getDonorById(Long donorId) {
        return toResponse(getExistingDonor(donorId));
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

    private Donor resolveDonor(String phone, String email) {
        Optional<Donor> donorByPhone = donorRepository.findByPhone(phone);
        Optional<Donor> donorByEmail = StringUtils.hasText(email)
                ? donorRepository.findByEmailIgnoreCase(email)
                : Optional.empty();

        if (donorByPhone.isPresent() && donorByEmail.isPresent()) {
            Donor phoneOwner = donorByPhone.get();
            Donor emailOwner = donorByEmail.get();

            if (!phoneOwner.getId().equals(emailOwner.getId())) {
                throw new InvalidDataException("Số điện thoại và email đang thuộc về hai nhà hảo tâm khác nhau");
            }
        }

        return donorByPhone.orElseGet(() -> donorByEmail.orElseGet(Donor::new));
    }

    private Donor getExistingDonor(Long donorId) {
        return donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhà hảo tâm"));
    }

    private void validateUniqueContactForUpdate(Long donorId, String phone, String email) {
        donorRepository.findByPhone(phone)
                .filter(existingDonor -> !existingDonor.getId().equals(donorId))
                .ifPresent(existingDonor -> {
                    throw new InvalidDataException("Số điện thoại đã được dùng cho nhà hảo tâm khác");
                });

        if (!StringUtils.hasText(email)) {
            return;
        }

        donorRepository.findByEmailIgnoreCase(email)
                .filter(existingDonor -> !existingDonor.getId().equals(donorId))
                .ifPresent(existingDonor -> {
                    throw new InvalidDataException("Email đã được dùng cho nhà hảo tâm khác");
                });
    }

    private void validateDonorType(Donor donor, EDonorType expectedType) {
        if (donor.getType() != expectedType) {
            throw new InvalidDataException("Loại nhà hảo tâm không khớp với biểu mẫu chỉnh sửa");
        }
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private Comparator<DonorResponse> buildDonorComparator(String sortBy, String sortDir) {
        String normalizedSortBy = normalizeSortBy(sortBy);
        boolean descending = "desc".equalsIgnoreCase(sortDir);

        Comparator<String> textComparator = Comparator.nullsLast(getVietnameseCollator());
        Comparator<DonorResponse> baseComparator = switch (normalizedSortBy) {
            case "name" -> Comparator.comparing(this::getSortableDonorName, textComparator);
            case "type" -> Comparator.comparing(donor -> donor.getType() != null ? donor.getType().name() : null, textComparator);
            case "contact" -> Comparator.comparing(this::getSortableContact, textComparator);
            case "createdAt" -> Comparator.comparing(DonorResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "numberOfDonations" -> Comparator.comparing(donor -> donor.getNumberOfDonations() != null ? donor.getNumberOfDonations() : 0);
            case "totalDonationAmount" -> Comparator.comparing(donor -> donor.getTotalDonationAmount() != null ? donor.getTotalDonationAmount() : BigDecimal.ZERO);
            default -> Comparator.comparing(DonorResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        Comparator<Long> idComparator = descending
                ? Comparator.nullsLast(Comparator.<Long>reverseOrder())
                : Comparator.nullsLast(Comparator.<Long>naturalOrder());
        Comparator<DonorResponse> tieBreaker = Comparator.comparing(DonorResponse::getId, idComparator);

        return descending ? baseComparator.reversed().thenComparing(tieBreaker) : baseComparator.thenComparing(tieBreaker);
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) return "id";

        return switch (sortBy.trim()) {
            case "name", "type", "contact", "createdAt", "numberOfDonations", "totalDonationAmount", "id" -> sortBy.trim();
            default -> "id";
        };
    }

    private String getSortableDonorName(DonorResponse donor) {
        if (donor == null) return null;

        if (donor.getOrganization() != null && StringUtils.hasText(donor.getOrganization().getName())) {
            return donor.getOrganization().getName();
        }

        if (StringUtils.hasText(donor.getFullName())) {
            return donor.getFullName();
        }

        return donor.getDisplayName();
    }

    private String getSortableContact(DonorResponse donor) {
        if (donor == null) return null;
        String phone = donor.getPhone() != null ? donor.getPhone() : "";
        String email = donor.getEmail() != null ? donor.getEmail() : "";
        String combined = (phone + " " + email).trim();
        return combined.isEmpty() ? null : combined;
    }

    private Collator getVietnameseCollator() {
        Collator collator = Collator.getInstance(Locale.forLanguageTag("vi-VN"));
        collator.setStrength(Collator.PRIMARY);
        return collator;
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
