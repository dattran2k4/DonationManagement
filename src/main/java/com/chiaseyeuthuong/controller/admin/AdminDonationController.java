package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationVia;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/donations")
public class AdminDonationController {

    private final DonationService donationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showAdminDonationPage(Model model) {
        return "pages/admin/donations";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showAdminDonationDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("donation", donationService.getDonationResponseById(id));
        return "pages/admin/donation-detail";
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showAdminDonationFormPage(Model model) {
        model.addAttribute("donationId", null);
        return "pages/admin/donation-form";
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showEditAdminDonationFormPage(@PathVariable Long id, Model model) {
        DonationResponse donation = donationService.getDonationResponseById(id);
        if (!EDonationVia.STAFF.equals(donation.getDonationVia())) {
            return "redirect:/admin/donations/" + id;
        }
        if (EDonationStatus.CONFIRMED.equals(donation.getStatus())) {
            return "redirect:/admin/donations/" + id;
        }
        model.addAttribute("donationId", id);
        return "pages/admin/donation-form";
    }
}
