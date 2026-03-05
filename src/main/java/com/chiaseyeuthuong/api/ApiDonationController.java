package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonationType;
import com.chiaseyeuthuong.dto.request.DonationRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.DonationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-DONATION-CONTROLLER")
@RequestMapping("/api/donations")
public class ApiDonationController {

    private final DonationService donationService;

    @GetMapping("/list")
    public ApiResponse getAllDonations(@RequestParam(required = false) String search,
                                       @RequestParam(required = false) EDonationStatus status,
                                       @RequestParam(required = false) EDonationTarget target,
                                       @RequestParam(required = false) EDonationType type,
                                       @RequestParam(required = false, defaultValue = "0") int page,
                                       @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Hi, how are you doing?")
                .data(donationService.getAllDonations(search, status, target, type, page, size))
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

    @PostMapping("/staff-create")
    public ApiResponse createDonationFromStaff(@Valid @RequestBody DonationRequest request, Principal principal) {
        donationService.createStaffDonation(request, principal.getName());
        return ApiResponse.builder()
                .status(200)
                .message("Tạo đơn từ thiện thành công từ staff")
                .build();
    }

    @PatchMapping("/{id}/change-status")
    public ApiResponse updateState(@Min(1) @PathVariable Long id, @RequestParam EDonationStatus status) {
        donationService.changeStatusDonation(status, id);
        return ApiResponse.builder()
                .status(200)
                .message("Cập nhật trạng thái đơn từ thiện thành công")
                .build();
    }
}
