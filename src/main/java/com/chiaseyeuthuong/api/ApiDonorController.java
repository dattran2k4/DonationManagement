package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.dto.request.IndividualDonorRequest;
import com.chiaseyeuthuong.dto.request.OrganizeDonorRequest;
import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.DonorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-DONOR-CONTROLLER")
@RequestMapping("/api/donors")
public class ApiDonorController {

    private final DonorService donorService;

    @GetMapping
    public ApiResponse getAllDonors(@RequestParam(required = false, defaultValue = "1") int page,
                                    @RequestParam(required = false, defaultValue = "10") int size,
                                    @RequestParam(required = false) String search,
                                    @RequestParam(required = false) EDonorType type) {
        return ApiResponse.builder()
                .status(200)
                .message("Get donor list successfully")
                .data(donorService.getAllDonor(page, size, search, type))
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse getDonorById(@PathVariable Long id) {
        return ApiResponse.builder()
                .status(200)
                .message("Get donor detail successfully")
                .data(donorService.getDonorById(id))
                .build();
    }

    @GetMapping("/{id}/donations")
    public ApiResponse getDonorDonations(@PathVariable Long id,
                                         @RequestParam(required = false, defaultValue = "1") int page,
                                         @RequestParam(required = false, defaultValue = "10") int size) {
        return ApiResponse.builder()
                .status(200)
                .message("Get donor donation history successfully")
                .data(donorService.getDonorDonations(id, page, size))
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

    @PutMapping("/{id}/individuals")
    public ApiResponse updateIndividualDonor(@PathVariable Long id, @Valid @RequestBody IndividualDonorRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Donor updated successfully")
                .data(donorService.updateIndividualDonor(id, request))
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

    @PutMapping("/{id}/organizations")
    public ApiResponse updateOrganizeDonor(@PathVariable Long id, @Valid @RequestBody OrganizeDonorRequest request) {
        return ApiResponse.builder()
                .status(200)
                .message("Donor updated successfully")
                .data(donorService.updateOrganizeDonor(id, request))
                .build();
    }
}
