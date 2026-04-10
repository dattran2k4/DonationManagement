package com.chiaseyeuthuong.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequiredArgsConstructor
@RequestMapping({"/donations", "/quyen-gop"})
public class DonationController {

    @GetMapping
    public String showDonationPage(Model model) {
        return "pages/web/donation";
    }

    @GetMapping({"/lookup", "/tra-cuu"})
    public String showDonationLookupPage() {
        return "pages/web/donation-lookup";
    }

    @GetMapping({"/donor-wall", "/bang-vang-tri-an"})
    public String showDonorWallPage() {
        return "pages/web/donor-wall";
    }
}
