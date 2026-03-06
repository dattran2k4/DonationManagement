package com.chiaseyeuthuong.controller;

import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.service.DonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Slf4j(topic = "PAYMENT-CONTROLLER")
@RequiredArgsConstructor
public class PaymentController {

    private final DonationService donationService;

    @GetMapping("/thanh-toan/thanh-cong")
    public String showSuccessPaymentPage(@RequestParam("orderCode") long orderCode, Model model) {
        Donation donation = donationService.getDonationByOrderCode(orderCode);

        model.addAttribute("donor", donation.getDonor());
        model.addAttribute("donation", donation);

        return "pages/web/payment-success";
    }

    @GetMapping("/thanh-toan/that-bai")
    public String showHomePage() {
        return "redirect:pages/web/index";
    }
}
