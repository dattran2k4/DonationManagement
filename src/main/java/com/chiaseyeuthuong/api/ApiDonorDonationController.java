package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.DonorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/donor")
public class ApiDonorDonationController {

    private final DonorService donorService;

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
}
