package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.model.Activity;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.model.Event;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.DonationService;
import com.chiaseyeuthuong.service.EventService;
import com.chiaseyeuthuong.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.model.webhooks.WebhookData;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "WEBHOOK-SERVICE")
public class WebhookService {

    private final DonationService donationService;
    private final TransactionService transactionService;
    private final ActivityService activityService;
    private final EventService eventService;

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

        //CHANGE TO CONFIRM
        donationService.changeStatusDonation(EDonationStatus.CONFIRMED, donation.getId());

        donation.setDonatedAt(LocalDateTime.now());

        //Create Transaction
        transactionService.createTransactionFromPayOS(data, donation);

        BigDecimal amount = donation.getAmount();
        if (EDonationTarget.EVENT.equals(donation.getTarget())) {
            Event event = donation.getEvent();
            eventService.updateEventCurrentAmount(event, amount);
        } else if (EDonationTarget.ACTIVITY.equals(donation.getTarget())) {
            activityService.updateCurrentAmount(donation.getActivity(), amount);
            eventService.updateEventCurrentAmount(donation.getActivity().getEvent(), amount);
        }
    }
}
