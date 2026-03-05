package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.common.EEventStatus;
import com.chiaseyeuthuong.service.DonationService;
import com.chiaseyeuthuong.service.DonorService;
import com.chiaseyeuthuong.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/dashboard")
public class AdminDashBoardController {

    private final DonationService donationService;
    private final EventService eventService;
    private final DonorService donorService;

    @GetMapping
    public String showAdminDashBoardPage(Model model) {
        model.addAttribute("totalDonationAmount", donationService.getTotalConfirmedDonationsAmount());
        model.addAttribute("upcomingEvent", eventService.getEventCount(EEventStatus.ONGOING));
        model.addAttribute("numberOfDonors", donorService.getDorCountByObjectId(null, null));
        return "pages/admin/dashboard";
    }

}
