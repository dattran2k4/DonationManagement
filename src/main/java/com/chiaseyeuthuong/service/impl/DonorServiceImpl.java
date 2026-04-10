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
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Donor;
import com.chiaseyeuthuong.model.Organization;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.repository.DonorRepository;
import com.chiaseyeuthuong.service.AuditLogService;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.MailService;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "DONOR-SERVICE")
public class DonorServiceImpl implements DonorService {

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final MailService mailService;
    private final AuditLogService auditLogService;

    private static final String DONOR_NOT_FOUND_MESSAGE = "Không tìm thấy nhà hảo tâm";
    private static final String TARGET_NOT_FOUND = "Không gắn mục tiêu";

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveIndividualDonor(IndividualDonorRequest request) {
        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());
        log.info("Processing saving donor for donor phone: {}", phone);

        Donor donor = donorRepository.findByPhone(request.getPhone()).orElse(new Donor());

        toEntity(donor, request, phone, email);

        Donor newDonor = donorRepository.save(donor);
        log.info("Individual Donor saved successfully with id={}", newDonor.getId());
        Map<String, Object> afterValues = buildDonorAuditMap(newDonor);
        auditLogService.logCreate(EEntityType.DONOR, newDonor.getId(), "Tạo mới nhà hảo tâm cá nhân", afterValues);

        return newDonor.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long saveOrganizeDonor(OrganizeDonorRequest request) {
        String phone = normalizePhone(request.getPhone());
        String email = normalizeEmail(request.getEmail());

        Donor donor = donorRepository.findByPhone(request.getPhone()).orElse(new Donor());

        toEntity(donor, request, phone, email);

        Donor result = donorRepository.save(donor);
        log.info("Organization Donor saved successfully with id={}", result.getId());
        Map<String, Object> afterValues = buildDonorAuditMap(result);
        auditLogService.logCreate(EEntityType.DONOR, result.getId(), "Tạo mới nhà hảo tâm tổ chức", afterValues);

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
        Map<String, Object> beforeValues = buildDonorAuditMap(donor);

        toEntity(donor, request, phone, email);

        Donor result = donorRepository.save(donor);
        log.info("Individual Donor updated successfully with id={}", result.getId());
        auditLogService.logUpdate(
                EEntityType.DONOR,
                result.getId(),
                "Cập nhật nhà hảo tâm cá nhân",
                beforeValues,
                buildDonorAuditMap(result)
        );
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
        Map<String, Object> beforeValues = buildDonorAuditMap(donor);

        toEntity(donor, request, phone, email);

        Donor result = donorRepository.save(donor);
        log.info("Organization Donor updated successfully with id={}", result.getId());
        auditLogService.logUpdate(
                EEntityType.DONOR,
                result.getId(),
                "Cập nhật nhà hảo tâm tổ chức",
                beforeValues,
                buildDonorAuditMap(result)
        );
        return result.getId();
    }

    @Override
    public PageResponse<DonorResponse> getAllDonor(int page, int size, String search, EDonorType type, String sortBy, String sortDir) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 50;

        Specification<Donor> specification = DonorSpecification.filterDonor(search, type);
        List<DonorResponse> filteredDonors = donorRepository.findAll(specification)
                .stream()
                .map(this::toResponse)
                .sorted(buildDonorComparator(sortBy, sortDir))
                .toList();

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
    public PageResponse<DonorDonationHistoryResponse> getDonorDonations(Long donorId, int page, int size) {
        getExistingDonor(donorId);

        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 10;
        PageRequest pageRequest = PageRequest.of(pageNumber, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Donation> donationPage = donationRepository.findByDonorId(donorId, pageRequest);

        List<DonorDonationHistoryResponse> data = donationPage.stream()
                .map(this::toDonorDonationHistoryResponse)
                .toList();

        return PageResponse.<DonorDonationHistoryResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalItems(donationPage.getTotalElements())
                .totalPages(donationPage.getTotalPages())
                .data(data)
                .build();
    }

    @Override
    public PageResponse<DonorDonationHistoryResponse> getDonorDonationsByEmail(String email, String code, int page, int size) {
        String normalizedEmail = normalizeEmail(email);
        if (!StringUtils.hasText(normalizedEmail)) {
            throw new InvalidDataException("Email không hợp lệ");
        }

        if (!mailService.verifyLookupCode(normalizedEmail, code)) {
            throw new InvalidDataException("Mã xác thực không hợp lệ hoặc đã hết hạn");
        }

        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 10;
        PageRequest pageRequest = PageRequest.of(pageNumber, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Donation> donationPage = donationRepository.findByDonorEmailIgnoreCase(normalizedEmail, pageRequest);

        List<DonorDonationHistoryResponse> data = donationPage.stream()
                .map(this::toDonorDonationHistoryResponse)
                .toList();

        return PageResponse.<DonorDonationHistoryResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalItems(donationPage.getTotalElements())
                .totalPages(donationPage.getTotalPages())
                .data(data)
                .build();
    }

    @Override
    public PageResponse<DonorResponse> getDonorsByEventId(Long eventId, int page, int size) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 10;

        PageRequest pageRequest = PageRequest.of(pageNumber, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Donor> donorPage = donorRepository.findDonorsByEventId(eventId, pageRequest);

        List<DonorResponse> data = donorPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DonorResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalItems(donorPage.getTotalElements())
                .totalPages(donorPage.getTotalPages())
                .data(data)
                .build();
    }

    @Override
    public PageResponse<DonorResponse> getDonorsByActivityId(Long activityId, int page, int size) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        int safeSize = size > 0 ? size : 10;

        PageRequest pageRequest = PageRequest.of(pageNumber, safeSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<Donor> donorPage = donorRepository.findDonorsByActivityId(activityId, pageRequest);

        List<DonorResponse> data = donorPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DonorResponse>builder()
                .page(pageNumber + 1)
                .pageSize(safeSize)
                .totalItems(donorPage.getTotalElements())
                .totalPages(donorPage.getTotalPages())
                .data(data)
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

    @Override
    public void sendLookupCodeIfEmailExists(String email) {
        log.info("Sending lookup code to email: {}", email);

        mailService.sendVerificationCodeMailAsync(normalizeEmail(email));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDonor(Long donorId) {
        Donor donor = getExistingDonor(donorId);
        long donationCount = donationRepository.countByDonorId(donorId);
        if (donationCount > 0) {
            throw new InvalidDataException("Không thể xóa nhà hảo tâm đã phát sinh quyên góp");
        }
        donorRepository.delete(donor);
    }

    private Donor getExistingDonor(Long donorId) {
        return donorRepository.findById(donorId)
                .orElseThrow(() -> new ResourceNotFoundException(DONOR_NOT_FOUND_MESSAGE));
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

    private void toEntity(Donor donor, IndividualDonorRequest request, String phone, String email) {
        donor.setType(EDonorType.INDIVIDUAL);
        donor.setFullName(request.getFullName());
        donor.setDisplayName(request.getDisplayName());
        donor.setPhone(phone);
        donor.setEmail(email);
        donor.setReferralSource(request.getReferralSource());
        donor.setNote(request.getNote());
        donor.setOrganization(null);
    }

    private void toEntity(Donor donor, OrganizeDonorRequest request, String phone, String email) {
        donor.setType(EDonorType.ORGANIZATION);
        donor.setFullName(request.getName());
        donor.setDisplayName(request.getName());
        donor.setPhone(phone);
        donor.setEmail(email);
        donor.setReferralSource(request.getReferralSource());
        donor.setNote(request.getNote());

        Organization organization = toEntity(donor.getOrganization(), request);
        donor.setOrganization(organization);
    }

    private Organization toEntity(Organization organization, OrganizeDonorRequest request) {
        Organization target = organization != null ? organization : new Organization();
        target.setName(request.getName());
        target.setTaxCode(request.getTaxCode());
        target.setRepresentative(request.getRepresentative());
        target.setBillingAddress(request.getBillingAddress());
        return target;
    }

    private Map<String, Object> buildDonorAuditMap(Donor donor) {
        Organization org = donor.getOrganization();
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("type", donor.getType() != null ? donor.getType().name() : null);
        values.put("fullName", donor.getFullName());
        values.put("displayName", donor.getDisplayName());
        values.put("phone", donor.getPhone());
        values.put("email", donor.getEmail());
        values.put("referralSource", donor.getReferralSource());
        values.put("note", donor.getNote());
        values.put("organizationName", org != null ? org.getName() : null);
        values.put("organizationTaxCode", org != null ? org.getTaxCode() : null);
        values.put("organizationRepresentative", org != null ? org.getRepresentative() : null);
        values.put("organizationBillingAddress", org != null ? org.getBillingAddress() : null);
        return values;
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
            case "type" ->
                    Comparator.comparing(donor -> donor.getType() != null ? donor.getType().name() : null, textComparator);
            case "contact" -> Comparator.comparing(this::getSortableContact, textComparator);
            case "createdAt" ->
                    Comparator.comparing(DonorResponse::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            case "numberOfDonations" ->
                    Comparator.comparing(donor -> donor.getNumberOfDonations() != null ? donor.getNumberOfDonations() : 0);
            case "totalDonationAmount" ->
                    Comparator.comparing(donor -> donor.getTotalDonationAmount() != null ? donor.getTotalDonationAmount() : BigDecimal.ZERO);
            default -> Comparator.comparing(DonorResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        Comparator<Long> idComparator = descending
                ? Comparator.nullsLast(Comparator.reverseOrder())
                : Comparator.nullsLast(Comparator.<Long>naturalOrder());
        Comparator<DonorResponse> tieBreaker = Comparator.comparing(DonorResponse::getId, idComparator);

        return descending ? baseComparator.reversed().thenComparing(tieBreaker) : baseComparator.thenComparing(tieBreaker);
    }

    private String normalizeSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) return "id";

        return switch (sortBy.trim()) {
            case "name", "type", "contact", "createdAt", "numberOfDonations", "totalDonationAmount", "id" ->
                    sortBy.trim();
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
        String combined = ("%s %s".formatted(phone, email)).trim();
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
        response.setCreatedAt(donor.getCreatedAt());
        response.setCreatedBy(donor.getCreatedBy());
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
        response.setTargetLabel(getTargetLabel(donation.getTarget()));
        response.setPaymentMethod(donation.getPaymentMethod());
        response.setPaymentMethodLabel(donation.getPaymentMethod() != null ? donation.getPaymentMethod().getValue() : "---");
        response.setDonatedAt(donation.getDonatedAt() != null ? donation.getDonatedAt() : donation.getCreatedAt());

        if (EDonationTarget.EVENT.equals(donation.getTarget()) && donation.getEvent() != null) {
            response.setTargetTitle(donation.getEvent().getName());
            response.setTargetUrl(donation.getEvent().getSlug() != null ? "/su-kien/%s".formatted(donation.getEvent().getSlug()) : null);
        } else if (EDonationTarget.ACTIVITY.equals(donation.getTarget()) && donation.getActivity() != null) {
            response.setTargetTitle(donation.getActivity().getName());
            response.setTargetUrl(donation.getActivity().getSlug() != null ? "/hoat-dong/%s".formatted(donation.getActivity().getSlug()) : null);
        } else {
            response.setTargetTitle(TARGET_NOT_FOUND);
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

    private String getTargetLabel(EDonationTarget target) {
        if (target == null) {
            return TARGET_NOT_FOUND;
        }
        return switch (target) {
            case EVENT -> "Sự kiện";
            case ACTIVITY -> "Hoạt động";
            case NONE -> TARGET_NOT_FOUND;
        };
    }
}
