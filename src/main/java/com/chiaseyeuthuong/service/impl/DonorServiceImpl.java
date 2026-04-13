package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.common.EEntityType;
import com.chiaseyeuthuong.common.sort.SortParamUtils;
import com.chiaseyeuthuong.dto.request.DonorOrganizationRelationshipRequest;
import com.chiaseyeuthuong.dto.request.DonorPersonRelationshipRequest;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.DonorDonationHistoryResponse;
import com.chiaseyeuthuong.dto.response.DonorOrganizationRelationshipResponse;
import com.chiaseyeuthuong.dto.response.DonorPersonRelationshipResponse;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.OrganizationRoleTypeResponse;
import com.chiaseyeuthuong.dto.response.OrganizationResponse;
import com.chiaseyeuthuong.dto.response.PageResponse;
import com.chiaseyeuthuong.dto.response.PersonRelationshipTypeResponse;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Donor;
import com.chiaseyeuthuong.model.DonorOrganizationRelationship;
import com.chiaseyeuthuong.model.DonorPersonRelationship;
import com.chiaseyeuthuong.model.OrganizationRoleType;
import com.chiaseyeuthuong.model.Organization;
import com.chiaseyeuthuong.model.PersonRelationshipType;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.repository.DonorRepository;
import com.chiaseyeuthuong.repository.DonorOrganizationRelationshipRepository;
import com.chiaseyeuthuong.repository.DonorPersonRelationshipRepository;
import com.chiaseyeuthuong.repository.OrganizationRoleTypeRepository;
import com.chiaseyeuthuong.repository.PersonRelationshipTypeRepository;
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

    private static final Map<String, String> DONOR_RELATION_SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("name", "fullName"),
            Map.entry("contact", "phone"),
            Map.entry("type", "type"),
            Map.entry("createdAt", "createdAt")
    );
    private static final Map<String, String> DONOR_HISTORY_SORT_FIELDS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("donationCode", "memoCode"),
            Map.entry("amount", "amount"),
            Map.entry("paymentMethod", "paymentMethod"),
            Map.entry("createdAt", "createdAt"),
            Map.entry("target", "target")
    );
    private static final Map<String, String> DONOR_HISTORY_UNSAFE_SORT_FIELDS = Map.ofEntries(
            Map.entry("donatedAt", "coalesce(donatedAt, createdAt)"),
            Map.entry("status", "case when status = 'PENDING_PAYMENT' then 1 when status = 'PENDING_APPROVED' then 2 when status = 'CONFIRMED' then 3 when status = 'REJECTED' then 4 when status = 'CANCELLED' then 5 when status = 'FAILED' then 6 else 99 end")
    );

    private final DonorRepository donorRepository;
    private final DonationRepository donationRepository;
    private final MailService mailService;
    private final AuditLogService auditLogService;
    private final PersonRelationshipTypeRepository personRelationshipTypeRepository;
    private final OrganizationRoleTypeRepository organizationRoleTypeRepository;
    private final DonorPersonRelationshipRepository donorPersonRelationshipRepository;
    private final DonorOrganizationRelationshipRepository donorOrganizationRelationshipRepository;

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
    @Transactional(readOnly = true)
    public List<PersonRelationshipTypeResponse> getActivePersonRelationshipTypes() {
        return personRelationshipTypeRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrganizationRoleTypeResponse> getActiveOrganizationRoleTypes() {
        return organizationRoleTypeRepository.findByIsActiveTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonorPersonRelationshipResponse> getPersonRelationships(Long donorId, String sortBy, String sortDir) {
        Donor donor = getExistingDonor(donorId);
        if (donor.getType() != EDonorType.INDIVIDUAL) {
            return List.of();
        }

        return donorPersonRelationshipRepository.findByDonorIdAndIsActiveTrueOrderByUpdatedAtDescIdDesc(donorId)
                .stream()
                .map(this::toResponse)
                .sorted(buildPersonRelationshipComparator(sortBy, sortDir))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DonorOrganizationRelationshipResponse> getOrganizationRelationships(Long donorId, String sortBy, String sortDir) {
        getExistingDonor(donorId);
        return donorOrganizationRelationshipRepository.findByDonorIdAndIsActiveTrueOrderByUpdatedAtDescIdDesc(donorId)
                .stream()
                .map(this::toResponse)
                .sorted(buildOrganizationRelationshipComparator(sortBy, sortDir))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createPersonRelationship(Long donorId, DonorPersonRelationshipRequest request) {
        Donor donor = validateIndividualRelationshipOwnerDonor(donorId);
        Donor relatedDonor = validateRelatedPersonDonor(donor, request.getRelatedDonorId());
        PersonRelationshipType relationshipType = getPersonRelationshipType(request.getRelationshipTypeId());
        PersonRelationshipType reverseRelationshipType = getReversePersonRelationshipType(relationshipType);

        ensurePersonRelationshipNotDuplicated(donorId, relatedDonor.getId(), relationshipType.getId(), null);
        ensurePersonRelationshipNotDuplicated(relatedDonor.getId(), donorId, reverseRelationshipType.getId(), null);

        DonorPersonRelationship relationship = new DonorPersonRelationship();
        relationship.setDonor(donor);
        relationship.setRelatedDonor(relatedDonor);
        relationship.setRelationshipType(relationshipType);
        relationship.setNote(normalizeNote(request.getNote()));

        DonorPersonRelationship reverseRelationship = new DonorPersonRelationship();
        reverseRelationship.setDonor(relatedDonor);
        reverseRelationship.setRelatedDonor(donor);
        reverseRelationship.setRelationshipType(reverseRelationshipType);
        reverseRelationship.setNote(normalizeNote(request.getNote()));

        DonorPersonRelationship savedRelationship = donorPersonRelationshipRepository.save(relationship);
        donorPersonRelationshipRepository.save(reverseRelationship);
        auditLogService.logCreate(
                EEntityType.DONOR,
                donorId,
                "Thêm mối quan hệ cá nhân",
                buildPersonRelationshipAuditMap(savedRelationship)
        );
        return savedRelationship.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long updatePersonRelationship(Long donorId, Long relationshipId, DonorPersonRelationshipRequest request) {
        validateIndividualRelationshipOwnerDonor(donorId);
        DonorPersonRelationship relationship = getPersonRelationship(donorId, relationshipId);
        Map<String, Object> beforeValues = buildPersonRelationshipAuditMap(relationship);
        DonorPersonRelationship reverseRelationship = getReversePersonRelationship(relationship);

        Donor relatedDonor = validateRelatedPersonDonor(relationship.getDonor(), request.getRelatedDonorId());
        PersonRelationshipType relationshipType = getPersonRelationshipType(request.getRelationshipTypeId());
        PersonRelationshipType reverseRelationshipType = getReversePersonRelationshipType(relationshipType);

        ensurePersonRelationshipNotDuplicated(donorId, relatedDonor.getId(), relationshipType.getId(), relationshipId);
        ensurePersonRelationshipNotDuplicated(relatedDonor.getId(), donorId, reverseRelationshipType.getId(),
                reverseRelationship != null ? reverseRelationship.getId() : null);

        relationship.setRelatedDonor(relatedDonor);
        relationship.setRelationshipType(relationshipType);
        relationship.setNote(normalizeNote(request.getNote()));

        if (reverseRelationship == null) {
            reverseRelationship = new DonorPersonRelationship();
        }
        reverseRelationship.setDonor(relatedDonor);
        reverseRelationship.setRelatedDonor(relationship.getDonor());
        reverseRelationship.setRelationshipType(reverseRelationshipType);
        reverseRelationship.setNote(normalizeNote(request.getNote()));

        DonorPersonRelationship savedRelationship = donorPersonRelationshipRepository.save(relationship);
        donorPersonRelationshipRepository.save(reverseRelationship);
        auditLogService.logUpdate(
                EEntityType.DONOR,
                donorId,
                "Cập nhật mối quan hệ cá nhân",
                beforeValues,
                buildPersonRelationshipAuditMap(savedRelationship)
        );
        return savedRelationship.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivatePersonRelationship(Long donorId, Long relationshipId) {
        validateIndividualRelationshipOwnerDonor(donorId);
        DonorPersonRelationship relationship = getPersonRelationship(donorId, relationshipId);
        Map<String, Object> beforeValues = buildPersonRelationshipAuditMap(relationship);
        DonorPersonRelationship reverseRelationship = getReversePersonRelationship(relationship);

        relationship.setIsActive(false);
        donorPersonRelationshipRepository.save(relationship);
        if (reverseRelationship != null) {
            reverseRelationship.setIsActive(false);
            donorPersonRelationshipRepository.save(reverseRelationship);
        }

        auditLogService.logUpdate(
                EEntityType.DONOR,
                donorId,
                "Ngừng sử dụng mối quan hệ cá nhân",
                beforeValues,
                buildPersonRelationshipAuditMap(relationship)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long createOrganizationRelationship(Long donorId, DonorOrganizationRelationshipRequest request) {
        Donor donor = getExistingDonor(donorId);
        Donor relatedDonor = validateOrganizationRelatedDonor(donor, request.getOrganizationDonorId());
        OrganizationRoleType roleType = getOrganizationRoleType(request.getRoleTypeId());

        ensureOrganizationRelationshipNotDuplicated(donorId, relatedDonor.getId(), roleType.getId(), null);
        ensureOrganizationRelationshipNotDuplicated(relatedDonor.getId(), donorId, roleType.getId(), null);

        DonorOrganizationRelationship relationship = new DonorOrganizationRelationship();
        relationship.setDonor(donor);
        relationship.setOrganizationDonor(relatedDonor);
        relationship.setRoleType(roleType);
        relationship.setNote(normalizeNote(request.getNote()));

        DonorOrganizationRelationship reverseRelationship = new DonorOrganizationRelationship();
        reverseRelationship.setDonor(relatedDonor);
        reverseRelationship.setOrganizationDonor(donor);
        reverseRelationship.setRoleType(roleType);
        reverseRelationship.setNote(normalizeNote(request.getNote()));

        DonorOrganizationRelationship savedRelationship = donorOrganizationRelationshipRepository.save(relationship);
        donorOrganizationRelationshipRepository.save(reverseRelationship);
        auditLogService.logCreate(
                EEntityType.DONOR,
                donorId,
                "Thêm mối quan hệ tổ chức",
                buildOrganizationRelationshipAuditMap(savedRelationship)
        );
        return savedRelationship.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public long updateOrganizationRelationship(Long donorId, Long relationshipId, DonorOrganizationRelationshipRequest request) {
        getExistingDonor(donorId);
        DonorOrganizationRelationship relationship = getOrganizationRelationship(donorId, relationshipId);
        Map<String, Object> beforeValues = buildOrganizationRelationshipAuditMap(relationship);
        DonorOrganizationRelationship reverseRelationship = getReverseOrganizationRelationship(relationship);

        Donor relatedDonor = validateOrganizationRelatedDonor(relationship.getDonor(), request.getOrganizationDonorId());
        OrganizationRoleType roleType = getOrganizationRoleType(request.getRoleTypeId());

        ensureOrganizationRelationshipNotDuplicated(donorId, relatedDonor.getId(), roleType.getId(), relationshipId);
        ensureOrganizationRelationshipNotDuplicated(relatedDonor.getId(), donorId, roleType.getId(),
                reverseRelationship != null ? reverseRelationship.getId() : null);

        relationship.setOrganizationDonor(relatedDonor);
        relationship.setRoleType(roleType);
        relationship.setNote(normalizeNote(request.getNote()));

        if (reverseRelationship == null) {
            reverseRelationship = new DonorOrganizationRelationship();
        }
        reverseRelationship.setDonor(relatedDonor);
        reverseRelationship.setOrganizationDonor(relationship.getDonor());
        reverseRelationship.setRoleType(roleType);
        reverseRelationship.setNote(normalizeNote(request.getNote()));

        DonorOrganizationRelationship savedRelationship = donorOrganizationRelationshipRepository.save(relationship);
        donorOrganizationRelationshipRepository.save(reverseRelationship);
        auditLogService.logUpdate(
                EEntityType.DONOR,
                donorId,
                "Cập nhật mối quan hệ tổ chức",
                beforeValues,
                buildOrganizationRelationshipAuditMap(savedRelationship)
        );
        return savedRelationship.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateOrganizationRelationship(Long donorId, Long relationshipId) {
        getExistingDonor(donorId);
        DonorOrganizationRelationship relationship = getOrganizationRelationship(donorId, relationshipId);
        Map<String, Object> beforeValues = buildOrganizationRelationshipAuditMap(relationship);
        DonorOrganizationRelationship reverseRelationship = getReverseOrganizationRelationship(relationship);

        relationship.setIsActive(false);
        donorOrganizationRelationshipRepository.save(relationship);
        if (reverseRelationship != null) {
            reverseRelationship.setIsActive(false);
            donorOrganizationRelationshipRepository.save(reverseRelationship);
        }

        auditLogService.logUpdate(
                EEntityType.DONOR,
                donorId,
                "Ngừng sử dụng mối quan hệ tổ chức",
                beforeValues,
                buildOrganizationRelationshipAuditMap(relationship)
        );
    }

    @Override
    public PageResponse<DonorDonationHistoryResponse> getDonorDonations(Long donorId, int page, int size, String sortBy, String sortDir) {
        getExistingDonor(donorId);

        Sort sort = SortParamUtils.buildSort(DONOR_HISTORY_SORT_FIELDS, DONOR_HISTORY_UNSAFE_SORT_FIELDS,
                sortBy, sortDir, "donatedAt", Sort.Direction.DESC, "id");
        PageRequest pageRequest = PageRequest.of(
                SortParamUtils.normalizePageNumber(page),
                SortParamUtils.normalizePageSize(size, 10),
                sort
        );
        Page<Donation> donationPage = donationRepository.findByDonorId(donorId, pageRequest);

        List<DonorDonationHistoryResponse> data = donationPage.stream()
                .map(this::toDonorDonationHistoryResponse)
                .toList();

        return PageResponse.<DonorDonationHistoryResponse>builder()
                .page(SortParamUtils.normalizePageNumber(page) + 1)
                .pageSize(SortParamUtils.normalizePageSize(size, 10))
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
    public PageResponse<DonorResponse> getDonorsByEventId(Long eventId, int page, int size, String sortBy, String sortDir) {
        Sort sort = SortParamUtils.buildSort(DONOR_RELATION_SORT_FIELDS, Map.of(),
                sortBy, sortDir, "createdAt", Sort.Direction.DESC, "id");
        PageRequest pageRequest = PageRequest.of(
                SortParamUtils.normalizePageNumber(page),
                SortParamUtils.normalizePageSize(size, 10),
                sort
        );
        Page<Donor> donorPage = donorRepository.findDonorsByEventId(eventId, pageRequest);

        List<DonorResponse> data = donorPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DonorResponse>builder()
                .page(SortParamUtils.normalizePageNumber(page) + 1)
                .pageSize(SortParamUtils.normalizePageSize(size, 10))
                .totalItems(donorPage.getTotalElements())
                .totalPages(donorPage.getTotalPages())
                .data(data)
                .build();
    }

    @Override
    public PageResponse<DonorResponse> getDonorsByActivityId(Long activityId, int page, int size, String sortBy, String sortDir) {
        Sort sort = SortParamUtils.buildSort(DONOR_RELATION_SORT_FIELDS, Map.of(),
                sortBy, sortDir, "createdAt", Sort.Direction.DESC, "id");
        PageRequest pageRequest = PageRequest.of(
                SortParamUtils.normalizePageNumber(page),
                SortParamUtils.normalizePageSize(size, 10),
                sort
        );
        Page<Donor> donorPage = donorRepository.findDonorsByActivityId(activityId, pageRequest);

        List<DonorResponse> data = donorPage.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return PageResponse.<DonorResponse>builder()
                .page(SortParamUtils.normalizePageNumber(page) + 1)
                .pageSize(SortParamUtils.normalizePageSize(size, 10))
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

    private Donor validateIndividualRelationshipOwnerDonor(Long donorId) {
        Donor donor = getExistingDonor(donorId);
        if (donor.getType() != EDonorType.INDIVIDUAL) {
            throw new InvalidDataException("Chỉ có thể quản lý mối quan hệ cho nhà hảo tâm cá nhân");
        }
        return donor;
    }

    private Donor validateRelatedPersonDonor(Donor donor, Long relatedDonorId) {
        Donor relatedDonor = getExistingDonor(relatedDonorId);
        if (relatedDonor.getType() != EDonorType.INDIVIDUAL) {
            throw new InvalidDataException("Nhà hảo tâm liên quan phải là cá nhân");
        }
        if (donor.getId().equals(relatedDonor.getId())) {
            throw new InvalidDataException("Không thể tạo mối quan hệ với chính mình");
        }
        return relatedDonor;
    }

    private Donor validateOrganizationRelatedDonor(Donor donor, Long relatedDonorId) {
        Donor relatedDonor = getExistingDonor(relatedDonorId);
        if (donor.getId().equals(relatedDonor.getId())) {
            throw new InvalidDataException("Không thể tạo mối quan hệ với chính mình");
        }
        if (donor.getType() == EDonorType.INDIVIDUAL && relatedDonor.getType() != EDonorType.ORGANIZATION) {
            throw new InvalidDataException("Từ hồ sơ cá nhân, bạn chỉ có thể liên kết tới tổ chức");
        }
        if (donor.getType() == EDonorType.ORGANIZATION && relatedDonor.getType() != EDonorType.INDIVIDUAL) {
            throw new InvalidDataException("Từ hồ sơ tổ chức, bạn chỉ có thể liên kết tới nhà hảo tâm cá nhân");
        }
        return relatedDonor;
    }

    private PersonRelationshipType getPersonRelationshipType(Long relationshipTypeId) {
        return personRelationshipTypeRepository.findById(relationshipTypeId)
                .filter(type -> Boolean.TRUE.equals(type.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại mối quan hệ cá nhân"));
    }

    private PersonRelationshipType getReversePersonRelationshipType(PersonRelationshipType relationshipType) {
        String reverseCode = relationshipType.getReverseCode();
        if (!StringUtils.hasText(reverseCode)) {
            return relationshipType;
        }

        return personRelationshipTypeRepository.findByCode(reverseCode)
                .filter(type -> Boolean.TRUE.equals(type.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại mối quan hệ ngược"));
    }

    private OrganizationRoleType getOrganizationRoleType(Long roleTypeId) {
        return organizationRoleTypeRepository.findById(roleTypeId)
                .filter(type -> Boolean.TRUE.equals(type.getIsActive()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò tổ chức"));
    }

    private DonorPersonRelationship getPersonRelationship(Long donorId, Long relationshipId) {
        return donorPersonRelationshipRepository.findByIdAndDonorIdAndIsActiveTrue(relationshipId, donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mối quan hệ cá nhân"));
    }

    private DonorOrganizationRelationship getOrganizationRelationship(Long donorId, Long relationshipId) {
        return donorOrganizationRelationshipRepository.findByIdAndDonorIdAndIsActiveTrue(relationshipId, donorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mối quan hệ tổ chức"));
    }

    private DonorPersonRelationship getReversePersonRelationship(DonorPersonRelationship relationship) {
        Long reverseTypeId = getReversePersonRelationshipType(relationship.getRelationshipType()).getId();
        return donorPersonRelationshipRepository.findFirstByDonorIdAndRelatedDonorIdAndRelationshipTypeIdAndIsActiveTrue(
                relationship.getRelatedDonor().getId(),
                relationship.getDonor().getId(),
                reverseTypeId
        ).orElse(null);
    }

    private DonorOrganizationRelationship getReverseOrganizationRelationship(DonorOrganizationRelationship relationship) {
        return donorOrganizationRelationshipRepository.findFirstByDonorIdAndOrganizationDonorIdAndRoleTypeIdAndIsActiveTrue(
                relationship.getOrganizationDonor().getId(),
                relationship.getDonor().getId(),
                relationship.getRoleType().getId()
        ).orElse(null);
    }

    private void ensurePersonRelationshipNotDuplicated(Long donorId, Long relatedDonorId, Long relationshipTypeId, Long currentRelationshipId) {
        donorPersonRelationshipRepository.findFirstByDonorIdAndRelatedDonorIdAndRelationshipTypeIdAndIsActiveTrue(
                        donorId,
                        relatedDonorId,
                        relationshipTypeId
                )
                .filter(existing -> currentRelationshipId == null || !existing.getId().equals(currentRelationshipId))
                .ifPresent(existing -> {
                    throw new InvalidDataException("Mối quan hệ cá nhân này đã tồn tại");
                });
    }

    private void ensureOrganizationRelationshipNotDuplicated(Long donorId, Long organizationDonorId, Long roleTypeId, Long currentRelationshipId) {
        donorOrganizationRelationshipRepository.findFirstByDonorIdAndOrganizationDonorIdAndRoleTypeIdAndIsActiveTrue(
                        donorId,
                        organizationDonorId,
                        roleTypeId
                )
                .filter(existing -> currentRelationshipId == null || !existing.getId().equals(currentRelationshipId))
                .ifPresent(existing -> {
                    throw new InvalidDataException("Vai trò này với tổ chức đã tồn tại");
                });
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

    private Map<String, Object> buildPersonRelationshipAuditMap(DonorPersonRelationship relationship) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("loạiQuanHệ", relationship.getRelationshipType().getName());
        values.put("nhàHảoTâmLiênQuanId", relationship.getRelatedDonor().getId());
        values.put("nhàHảoTâmLiênQuan", relationship.getRelatedDonor().getFullName());
        values.put("sốĐiệnThoạiLiênQuan", relationship.getRelatedDonor().getPhone());
        values.put("ghiChú", relationship.getNote());
        values.put("đangHoạtĐộng", relationship.getIsActive());
        return values;
    }

    private Map<String, Object> buildOrganizationRelationshipAuditMap(DonorOrganizationRelationship relationship) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("vaiTròTổChức", relationship.getRoleType().getName());
        values.put("nhàHảoTâmLiênQuanId", relationship.getOrganizationDonor().getId());
        values.put("nhàHảoTâmLiênQuan", getOrganizationDisplayName(relationship.getOrganizationDonor()));
        values.put("ghiChú", relationship.getNote());
        values.put("đangHoạtĐộng", relationship.getIsActive());
        return values;
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.trim();
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String normalizeNote(String note) {
        if (!StringUtils.hasText(note)) {
            return null;
        }
        return note.trim();
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
        if (!StringUtils.hasText(sortBy)) return "createdAt";

        return switch (sortBy.trim()) {
            case "name", "type", "contact", "createdAt", "numberOfDonations", "totalDonationAmount", "id" ->
                    sortBy.trim();
            default -> "createdAt";
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

    private Comparator<DonorPersonRelationshipResponse> buildPersonRelationshipComparator(String sortBy, String sortDir) {
        String normalizedSortBy = normalizePersonRelationshipSortBy(sortBy);
        boolean descending = "desc".equalsIgnoreCase(sortDir);

        Comparator<String> textComparator = Comparator.nullsLast(getVietnameseCollator());
        Comparator<DonorPersonRelationshipResponse> baseComparator = switch (normalizedSortBy) {
            case "name" -> Comparator.comparing(this::getSortablePersonRelationshipName, textComparator);
            case "contact" -> Comparator.comparing(this::getSortablePersonRelationshipContact, textComparator);
            case "relationshipType" -> Comparator.comparing(DonorPersonRelationshipResponse::getRelationshipTypeName, textComparator);
            case "note" -> Comparator.comparing(DonorPersonRelationshipResponse::getNote, textComparator);
            case "updatedAt" -> Comparator.comparing(DonorPersonRelationshipResponse::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(DonorPersonRelationshipResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        Comparator<Long> idComparator = descending
                ? Comparator.nullsLast(Comparator.reverseOrder())
                : Comparator.nullsLast(Comparator.<Long>naturalOrder());
        Comparator<DonorPersonRelationshipResponse> tieBreaker = Comparator.comparing(DonorPersonRelationshipResponse::getId, idComparator);

        return descending ? baseComparator.reversed().thenComparing(tieBreaker) : baseComparator.thenComparing(tieBreaker);
    }

    private Comparator<DonorOrganizationRelationshipResponse> buildOrganizationRelationshipComparator(String sortBy, String sortDir) {
        String normalizedSortBy = normalizeOrganizationRelationshipSortBy(sortBy);
        boolean descending = "desc".equalsIgnoreCase(sortDir);

        Comparator<String> textComparator = Comparator.nullsLast(getVietnameseCollator());
        Comparator<DonorOrganizationRelationshipResponse> baseComparator = switch (normalizedSortBy) {
            case "name" -> Comparator.comparing(this::getSortableOrganizationRelationshipName, textComparator);
            case "contact" -> Comparator.comparing(this::getSortableOrganizationRelationshipContact, textComparator);
            case "roleType" -> Comparator.comparing(DonorOrganizationRelationshipResponse::getRoleTypeName, textComparator);
            case "note" -> Comparator.comparing(DonorOrganizationRelationshipResponse::getNote, textComparator);
            case "updatedAt" -> Comparator.comparing(DonorOrganizationRelationshipResponse::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()));
            default -> Comparator.comparing(DonorOrganizationRelationshipResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()));
        };

        Comparator<Long> idComparator = descending
                ? Comparator.nullsLast(Comparator.reverseOrder())
                : Comparator.nullsLast(Comparator.<Long>naturalOrder());
        Comparator<DonorOrganizationRelationshipResponse> tieBreaker = Comparator.comparing(DonorOrganizationRelationshipResponse::getId, idComparator);

        return descending ? baseComparator.reversed().thenComparing(tieBreaker) : baseComparator.thenComparing(tieBreaker);
    }

    private String normalizePersonRelationshipSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) return "updatedAt";

        return switch (sortBy.trim()) {
            case "name", "contact", "relationshipType", "note", "updatedAt", "id" -> sortBy.trim();
            default -> "updatedAt";
        };
    }

    private String normalizeOrganizationRelationshipSortBy(String sortBy) {
        if (!StringUtils.hasText(sortBy)) return "updatedAt";

        return switch (sortBy.trim()) {
            case "name", "contact", "roleType", "note", "updatedAt", "id" -> sortBy.trim();
            default -> "updatedAt";
        };
    }

    private String getSortablePersonRelationshipName(DonorPersonRelationshipResponse relationship) {
        if (relationship == null) return null;
        if (StringUtils.hasText(relationship.getRelatedDonorName())) {
            return relationship.getRelatedDonorName();
        }
        return relationship.getRelatedDonorDisplayName();
    }

    private String getSortablePersonRelationshipContact(DonorPersonRelationshipResponse relationship) {
        if (relationship == null) return null;
        String phone = relationship.getRelatedDonorPhone() != null ? relationship.getRelatedDonorPhone() : "";
        String email = relationship.getRelatedDonorEmail() != null ? relationship.getRelatedDonorEmail() : "";
        String combined = ("%s %s".formatted(phone, email)).trim();
        return combined.isEmpty() ? null : combined;
    }

    private String getSortableOrganizationRelationshipName(DonorOrganizationRelationshipResponse relationship) {
        return relationship != null ? relationship.getRelatedDonorName() : null;
    }

    private String getSortableOrganizationRelationshipContact(DonorOrganizationRelationshipResponse relationship) {
        if (relationship == null) return null;
        String phone = relationship.getRelatedDonorPhone() != null ? relationship.getRelatedDonorPhone() : "";
        String email = relationship.getRelatedDonorEmail() != null ? relationship.getRelatedDonorEmail() : "";
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

    private PersonRelationshipTypeResponse toResponse(PersonRelationshipType relationshipType) {
        PersonRelationshipTypeResponse response = new PersonRelationshipTypeResponse();
        response.setId(relationshipType.getId());
        response.setCode(relationshipType.getCode());
        response.setName(relationshipType.getName());
        return response;
    }

    private OrganizationRoleTypeResponse toResponse(OrganizationRoleType roleType) {
        OrganizationRoleTypeResponse response = new OrganizationRoleTypeResponse();
        response.setId(roleType.getId());
        response.setCode(roleType.getCode());
        response.setName(roleType.getName());
        return response;
    }

    private DonorPersonRelationshipResponse toResponse(DonorPersonRelationship relationship) {
        Donor relatedDonor = relationship.getRelatedDonor();
        DonorPersonRelationshipResponse response = new DonorPersonRelationshipResponse();
        response.setId(relationship.getId());
        response.setRelatedDonorId(relatedDonor.getId());
        response.setRelatedDonorName(relatedDonor.getFullName());
        response.setRelatedDonorDisplayName(relatedDonor.getDisplayName());
        response.setRelatedDonorPhone(relatedDonor.getPhone());
        response.setRelatedDonorEmail(relatedDonor.getEmail());
        response.setRelationshipTypeId(relationship.getRelationshipType().getId());
        response.setRelationshipTypeCode(relationship.getRelationshipType().getCode());
        response.setRelationshipTypeName(relationship.getRelationshipType().getName());
        response.setNote(relationship.getNote());
        response.setUpdatedAt(relationship.getUpdatedAt());
        return response;
    }

    private DonorOrganizationRelationshipResponse toResponse(DonorOrganizationRelationship relationship) {
        Donor relatedDonor = relationship.getOrganizationDonor();
        DonorOrganizationRelationshipResponse response = new DonorOrganizationRelationshipResponse();
        response.setId(relationship.getId());
        response.setRelatedDonorId(relatedDonor.getId());
        response.setRelatedDonorName(getOrganizationDisplayName(relatedDonor));
        response.setRelatedDonorPhone(relatedDonor.getPhone());
        response.setRelatedDonorEmail(relatedDonor.getEmail());
        response.setRelatedDonorType(relatedDonor.getType() != null ? relatedDonor.getType().name() : null);
        response.setRoleTypeId(relationship.getRoleType().getId());
        response.setRoleTypeCode(relationship.getRoleType().getCode());
        response.setRoleTypeName(relationship.getRoleType().getName());
        response.setNote(relationship.getNote());
        response.setUpdatedAt(relationship.getUpdatedAt());
        return response;
    }

    private String getOrganizationDisplayName(Donor donor) {
        if (donor == null) {
            return null;
        }

        if (donor.getOrganization() != null && StringUtils.hasText(donor.getOrganization().getName())) {
            return donor.getOrganization().getName();
        }

        if (StringUtils.hasText(donor.getFullName())) {
            return donor.getFullName();
        }

        return donor.getDisplayName();
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
