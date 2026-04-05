package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/donors")
public class AdminDonorController {

    private final DonorService donorService;
    private final DonationService donationService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showDonorsPage(Model model) {
        return "pages/admin/donors";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showDonorDetailPage(@PathVariable Long id, Model model, Authentication authentication) {
        model.addAttribute("donor", donorService.getDonorById(id));
        model.addAttribute("recentDonations", donationService.getRecentDonationsByDonorId(id, 10));
        model.addAttribute("canEditRelationships", hasAnyAuthority(authentication, "ROLE_ADMIN", "ROLE_STAFF"));
        return "pages/admin/donor-detail";
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showCreateDonorPage(Model model) {
        model.addAttribute("donorId", null);
        return "pages/admin/donor-form";
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showEditDonorPage(@PathVariable Long id, Model model) {
        model.addAttribute("donorId", id);
        return "pages/admin/donor-form";
    }

    @GetMapping("/{id}/donations")
    public String showDonorDonationHistoryPage(@PathVariable Long id, Model model) {
        model.addAttribute("donorId", id);
        model.addAttribute("donor", donorService.getDonorById(id));
        return "pages/admin/donor-donations";
    }

    private boolean hasAnyAuthority(Authentication authentication, String... roles) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        for (String role : roles) {
            boolean matched = authentication.getAuthorities().stream()
                    .anyMatch(authority -> role.equals(authority.getAuthority()));
            if (matched) {
                return true;
            }
        }
        return false;
    }
}
