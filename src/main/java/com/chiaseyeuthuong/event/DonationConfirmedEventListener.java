package com.chiaseyeuthuong.event;

import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.EventService;
import com.chiaseyeuthuong.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DONATION-CONFIRMED-LISTENER")
public class DonationConfirmedEventListener {

    private final DonationRepository donationRepository;
    private final TransactionService transactionService;
    private final ActivityService activityService;
    private final EventService eventService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDonationConfirmed(DonationConfirmedEvent event) {
        Donation donation = donationRepository.findById(event.getDonationId()).orElseThrow(() -> new IllegalStateException("Donation not found when handling DonationConfirmedEvent"));

        transactionService.createTransactionFromPayOS(event.getWebhookData(), donation);

        BigDecimal amount = donation.getAmount();
        if (EDonationTarget.EVENT.equals(donation.getTarget()) && donation.getEvent() != null) {
            eventService.updateEventCurrentAmount(donation.getEvent(), amount);
            return;
        }

        if (EDonationTarget.ACTIVITY.equals(donation.getTarget()) && donation.getActivity() != null) {
            activityService.updateCurrentAmount(donation.getActivity(), amount);
            if (donation.getActivity().getEvent() != null) {
                eventService.updateEventCurrentAmount(donation.getActivity().getEvent(), amount);
            }
        }

        log.info("Handled DonationConfirmedEvent for donationId={}", donation.getId());
    }
}
