package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonationType;
import com.chiaseyeuthuong.common.EDonorWallPeriod;
import com.chiaseyeuthuong.common.EPaymentMethod;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.request.RejectDonationRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.DonationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-DONATION-CONTROLLER")
@RequestMapping("/api/donations")
public class ApiDonationController {

    private final DonationService donationService;

    @GetMapping("/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getAllDonations(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) EDonationStatus status,
                                       @RequestParam(required = false) EDonationTarget target,
                                       @RequestParam(required = false) EDonationType type,
                                       @RequestParam(required = false) EPaymentMethod paymentMethod,
                                       @RequestParam(required = false) BigDecimal minAmount,
                                       @RequestParam(required = false) BigDecimal maxAmount,
                                       @RequestParam(required = false, defaultValue = "1") int page,
                                       @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy danh sách quyên góp thành công")
                .data(donationService.getAllDonations(search, status, target, type, paymentMethod, minAmount, maxAmount, page, size))
                .build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public ApiResponse getDonationById(@Min(1) @PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy chi tiết khoản quyên góp thành công")
                .data(donationService.getDonationResponseById(id))
                .build();
    }

    @PostMapping("/donor-create")
    public ApiResponse createDonationFromWeb(@Valid @RequestBody DonationRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Tạo đơn từ thiện thành công từ web")
                .data(donationService.createWebDonation(request))
                .build();
    }

    @GetMapping("/donor-wall")
    public ApiResponse getDonorWall(@RequestParam(required = false, defaultValue = "MONTH") EDonorWallPeriod period,
                                    @RequestParam(required = false) Integer year,
                                    @RequestParam(required = false) Integer month) {
        return ApiResponse.builder()
                .status(200)
                .message("Lấy bảng vàng tri ân thành công")
                .data(donationService.getDonorWall(period, year, month))
                .build();
    }

    @PostMapping("/staff-create")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse createDonationFromStaff(@Valid @RequestBody DonationRequest request, Principal principal) {
        donationService.createStaffDonation(request, principal.getName());
        return ApiResponse.builder()
                .status(200)
                .message("Tạo đơn từ thiện nội bộ thành công")
                .build();
    }

    @PutMapping("/{id}/staff-update")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse updateDonationFromStaff(@Min(1) @PathVariable Long id, @Valid @RequestBody DonationRequest request) {
        donationService.updateStaffDonation(id, request);
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật đơn quyên góp thành công")
                .build();
    }

    @PatchMapping("/{id}/submit-approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ApiResponse submitForApproval(@Min(1) @PathVariable Long id) {
        donationService.submitForApproval(id);
        return ApiResponse.builder()
                .status(200)
                .message("Gửi duyệt khoản quyên góp thành công")
                .build();
    }

    @PatchMapping("/{id}/change-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse updateState(@Min(1) @PathVariable Long id, @RequestParam EDonationStatus status) {
        donationService.changeStatusDonation(status, id);
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật trạng thái đơn từ thiện thành công")
                .build();
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING')")
    public ApiResponse rejectDonation(@Min(1) @PathVariable Long id,
                                      @Valid @RequestBody RejectDonationRequest request,
                                      Principal principal) {
        donationService.rejectDonation(id, request.getReason(), principal.getName());
        return ApiResponse.builder()
                .status(200)
                .message("Từ chối đơn từ thiện thành công")
                .build();
    }
}
