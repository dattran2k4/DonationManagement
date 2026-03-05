package com.chiaseyeuthuong.controller.admin;

import com.chiaseyeuthuong.service.DonationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin/donations")
public class AdminDonationController {

    private final DonationService donationService;

    @GetMapping
    public String showAdminDonationPage(Model model) {
        return "pages/admin/donations";
    }
}
