package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.service.DonationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.WebhookData;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "WEBHOOK-SERVICE")
public class WebhookService {

    private final DonationService donationService;

    @Transactional(rollbackFor = Exception.class)
    public void processWebhookPayOS(WebhookData data) {
        log.info("Processing Webhook Payos for orderCode={}", data.getOrderCode());

        Long orderCode = data.getOrderCode();

        Donation donation = donationService.getDonationByOrderCode(orderCode);

        if (donation.getStatus().equals(EDonationStatus.CONFIRMED)) {
            log.info("Donation {} was already processed. Skipping...", donation.getId());
            return;
        }

        if (!data.getCode().equals("00")) {
            return;
        }

        donationService.confirmDonation(donation.getId(), data);
    }
}
