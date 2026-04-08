package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EDonorType;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.dto.response.DonorResponse;
import com.chiaseyeuthuong.dto.response.DonationResponse;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/donors")
public class AdminDonorController {

    private final DonorService donorService;
    private final DonationService donationService;

    private static final String DONOR_ID = "donorId";

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showDonorsPage() {
        return "pages/admin/donors";
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTING', 'STAFF')")
    public String showDonorDetailPage(@PathVariable Long id, Model model) {
        model.addAttribute("donor", donorService.getDonorById(id));
        List<DonationResponse> recentDonations = donationService.getRecentDonationsByDonorId(id, 200);

        DonationResponse recentEventDonation = recentDonations.stream()
                .filter(donation -> EDonationTarget.EVENT.equals(donation.getTarget()) && donation.getEventId() != null)
                .findFirst()
                .orElse(null);
        DonationResponse recentActivityDonation = recentDonations.stream()
                .filter(donation -> EDonationTarget.ACTIVITY.equals(donation.getTarget()) && donation.getActivityId() != null)
                .findFirst()
                .orElse(null);

        long joinedEventCount = recentDonations.stream()
                .filter(donation -> EDonationTarget.EVENT.equals(donation.getTarget()) && donation.getEventId() != null)
                .map(DonationResponse::getEventId)
                .distinct()
                .count();

        long joinedActivityCount = recentDonations.stream()
                .filter(donation -> EDonationTarget.ACTIVITY.equals(donation.getTarget()) && donation.getActivityId() != null)
                .map(DonationResponse::getActivityId)
                .distinct()
                .count();

        model.addAttribute("recentDonations", recentDonations);
        model.addAttribute("recentEventDonation", recentEventDonation);
        model.addAttribute("recentActivityDonation", recentActivityDonation);
        model.addAttribute("joinedEventCount", joinedEventCount);
        model.addAttribute("joinedActivityCount", joinedActivityCount);
        return "pages/admin/donor-detail";
    }

    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showCreateDonorPage(Model model) {
        DonorResponse donor = new DonorResponse();
        donor.setType(EDonorType.INDIVIDUAL);
        model.addAttribute("donor", donor);
        model.addAttribute("recentDonations", List.of());
        model.addAttribute("recentEventDonation", null);
        model.addAttribute("recentActivityDonation", null);
        model.addAttribute("joinedEventCount", 0L);
        model.addAttribute("joinedActivityCount", 0L);
        return "pages/admin/donor-detail";
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public String showEditDonorPage(@PathVariable Long id) {
        return "redirect:/admin/donors/" + id;
    }

    @GetMapping("/{id}/donations")
    public String showDonorDonationHistoryPage(@PathVariable Long id, Model model) {
        model.addAttribute(DONOR_ID, id);
        model.addAttribute("donor", donorService.getDonorById(id));
        return "pages/admin/donor-donations";
    }
}
