package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.dto.request.DonorLookupDonationRequest;
import com.chiaseyeuthuong.dto.request.DonorLookupRequest;
import com.chiaseyeuthuong.dto.request.DonorOrganizationRelationshipRequest;
import com.chiaseyeuthuong.dto.request.DonorPersonRelationshipRequest;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.DonorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-DONOR-CONTROLLER")
@RequestMapping("/api/donors")
public class ApiDonorController {

    private final DonorService donorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getAllDonors(@RequestParam(required = false, defaultValue = "1") int page,
                                    @RequestParam(required = false, defaultValue = "10") int size,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) EDonorType type,
                                    @RequestParam(required = false, defaultValue = "id") String sortBy,
                                    @RequestParam(required = false, defaultValue = "desc") String sortDir) {
        return ApiResponse.builder()
                .status(200)
                .message("Get donor list successfully")
                .data(donorService.getAllDonor(page, size, search, type, sortBy, sortDir))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getDonorById(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Get donor detail successfully")
                .data(donorService.getDonorById(id))
                .build();
    }

    @GetMapping("/relationship-types/person")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getPersonRelationshipTypes() {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách loại mối quan hệ cá nhân thành công")
                .data(donorService.getActivePersonRelationshipTypes())
                .build();
    }

    @GetMapping("/relationship-types/organization-roles")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getOrganizationRoleTypes() {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách vai trò tổ chức thành công")
                .data(donorService.getActiveOrganizationRoleTypes())
                .build();
    }

    @GetMapping("/{id}/relationships/person")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getPersonRelationships(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách mối quan hệ cá nhân thành công")
                .data(donorService.getPersonRelationships(id))
                .build();
    }

    @GetMapping("/{id}/relationships/organizations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getOrganizationRelationships(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách mối quan hệ tổ chức thành công")
                .data(donorService.getOrganizationRelationships(id))
                .build();
    }

    @GetMapping("/{id}/donations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getDonorDonations(@PathVariable Long id,
                                         @RequestParam(required = false, defaultValue = "1") int page,
                                         @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Get donor donation history successfully")
                .data(donorService.getDonorDonations(id, page, size))
                .build();
    }

    @PostMapping("/lookup/send-code")
    public ApiResponse sendLookupCode(@Valid @RequestBody DonorLookupRequest request) {
        donorService.sendLookupCodeIfEmailExists(request.getEmail());
        return ApiResponse.builder()
                .status(200)
                .message("Nếu email tồn tại trong hệ thống, mã xác nhận đã được gửi.")
                .build();
    }

    @PostMapping("/lookup/donations")
    public ApiResponse getLookupDonations(@Valid @RequestBody DonorLookupDonationRequest request,
                                          @RequestParam(required = false, defaultValue = "1") int page,
                                          @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy lịch sử quyên góp thành công")
                .data(donorService.getDonorDonationsByEmail(request.getEmail(), request.getCode(), page, size))
                .build();
    }

    @PostMapping("/individuals")
    public ApiResponse saveIndividualDonor(@Valid @RequestBody IndividualDonorRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Donation saved successfully")
                .data(donorService.saveIndividualDonor(request))
                .build();
    }

    @PostMapping("/organizations")
    public ApiResponse createOrganizeDonor(@Valid @RequestBody OrganizeDonorRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Donation saved successfully")
                .data(donorService.saveOrganizeDonor(request))
                .build();
    }

    @PutMapping("/{id}/individuals")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse updateIndividualDonor(@PathVariable Long id, @Valid @RequestBody IndividualDonorRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Donor updated successfully")
                .data(donorService.updateIndividualDonor(id, request))
                .build();
    }

    @PutMapping("/{id}/organizations")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse updateOrganizeDonor(@PathVariable Long id, @Valid @RequestBody OrganizeDonorRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Donor updated successfully")
                .data(donorService.updateOrganizeDonor(id, request))
                .build();
    }

    @PostMapping("/{id}/relationships/person")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse createPersonRelationship(@PathVariable Long id,
                                                @Valid @RequestBody DonorPersonRelationshipRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Thêm mối quan hệ cá nhân thành công")
                .data(donorService.createPersonRelationship(id, request))
                .build();
    }

    @PutMapping("/{id}/relationships/person/{relationshipId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse updatePersonRelationship(@PathVariable Long id,
                                                @PathVariable Long relationshipId,
                                                @Valid @RequestBody DonorPersonRelationshipRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật mối quan hệ cá nhân thành công")
                .data(donorService.updatePersonRelationship(id, relationshipId, request))
                .build();
    }

    @DeleteMapping("/{id}/relationships/person/{relationshipId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse deactivatePersonRelationship(@PathVariable Long id,
                                                    @PathVariable Long relationshipId) {
        donorService.deactivatePersonRelationship(id, relationshipId);
        return ApiResponse.builder()
                .status(200)
                .message("Đã ngừng sử dụng mối quan hệ cá nhân")
                .build();
    }

    @PostMapping("/{id}/relationships/organizations")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse createOrganizationRelationship(@PathVariable Long id,
                                                      @Valid @RequestBody DonorOrganizationRelationshipRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Thêm mối quan hệ tổ chức thành công")
                .data(donorService.createOrganizationRelationship(id, request))
                .build();
    }

    @PutMapping("/{id}/relationships/organizations/{relationshipId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse updateOrganizationRelationship(@PathVariable Long id,
                                                      @PathVariable Long relationshipId,
                                                      @Valid @RequestBody DonorOrganizationRelationshipRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật mối quan hệ tổ chức thành công")
                .data(donorService.updateOrganizationRelationship(id, relationshipId, request))
                .build();
    }

    @DeleteMapping("/{id}/relationships/organizations/{relationshipId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse deactivateOrganizationRelationship(@PathVariable Long id,
                                                          @PathVariable Long relationshipId) {
        donorService.deactivateOrganizationRelationship(id, relationshipId);
        return ApiResponse.builder()
                .status(200)
                .message("Đã ngừng sử dụng mối quan hệ tổ chức")
                .build();
    }
}
