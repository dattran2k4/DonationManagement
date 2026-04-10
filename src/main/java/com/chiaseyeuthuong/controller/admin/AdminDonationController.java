package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/donations")
public class AdminDonationController {

    private final DonationService donationService;
    private final DonorService donorService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showAdminDonationPage(Model model) {
        return "pages/admin/donations";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showAdminDonationDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("donation", donationService.getDonationResponseById(id));
        model.addAttribute("prefillDonor", null);
        model.addAttribute("returnToUrl", "/admin/donations");
        return "pages/admin/donation-detail";
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showAdminDonationFormPage(@RequestParam(required = false) Long donorId,
                                            @RequestParam(required = false) String returnTo,
                                            Model model) {
        model.addAttribute("donation", DonationResponse.builder().build());
        model.addAttribute("prefillDonor", resolvePrefillDonor(donorId));
        model.addAttribute("returnToUrl", sanitizeReturnTo(returnTo));
        return "pages/admin/donation-detail";
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showEditAdminDonationFormPage(@PathVariable Long id, Model model) {
        return "redirect:/admin/donations/" + id;
    }

    private DonorResponse resolvePrefillDonor(Long donorId) {
        if (donorId == null || donorId <= 0) {
            return null;
        }
        return donorService.getDonorById(donorId);
    }

    private String sanitizeReturnTo(String returnTo) {
        if (returnTo == null || returnTo.isBlank()) {
            return "/admin/donations";
        }
        if (returnTo.startsWith("/admin/")) {
            return returnTo;
        }
        return "/admin/donations";
    }
}
