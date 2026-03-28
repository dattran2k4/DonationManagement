package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.service.DonorService;
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
    public String showDonorDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("donor", donorService.getDonorById(id));
        model.addAttribute("recentDonations", donationService.getRecentDonationsByDonorId(id, 10));
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
}
