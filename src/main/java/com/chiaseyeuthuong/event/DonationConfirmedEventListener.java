package com.chiaseyeuthuong.event;

import com.chiaseyeuthuong.common.EDonationTarget;
import com.chiaseyeuthuong.common.EDonationVia;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.service.ActivityService;
import com.chiaseyeuthuong.service.EventService;
import com.chiaseyeuthuong.service.MailService;
import com.chiaseyeuthuong.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j(topic = "DONATION-CONFIRMED-LISTENER")
public class DonationConfirmedEventListener {

    private final DonationRepository donationRepository;
    private final TransactionService transactionService;
    private final ActivityService activityService;
    private final EventService eventService;
    private final MailService mailService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onDonationConfirmed(DonationConfirmedEvent event) {
        Donation donation = donationRepository.findById(event.donationId()).orElseThrow(() -> new IllegalStateException("Donation not found when handling DonationConfirmedEvent"));

        transactionService.createTransactionFromPayOS(event.webhookData(), donation);

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

        if (EDonationVia.WEB.equals(donation.getDonationVia()) && donation.getDonor() != null && StringUtils.hasText(donation.getDonor().getEmail())) {
            mailService.sendDonationThankYouMailAsync(donation.getDonor().getEmail(), donation.getDonor().getDisplayName(),
                    donation.getMemoCode(), getTargetTitle(donation), formatVndAmount(donation.getAmount()));
        }

        log.info("Handled DonationConfirmedEvent for donationId={}", donation.getId());
    }

    private String getTargetTitle(Donation donation) {
        if (EDonationTarget.EVENT.equals(donation.getTarget()) && donation.getEvent() != null) {
            return donation.getEvent().getName();
        }
        if (EDonationTarget.ACTIVITY.equals(donation.getTarget()) && donation.getActivity() != null) {
            return donation.getActivity().getName();
        }
        return "Không gắn mục tiêu";
    }

    private String formatVndAmount(BigDecimal amount) {
        if (amount == null) {
            return "---";
        }
        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        numberFormat.setMaximumFractionDigits(0);
        return "%s VNĐ".formatted(numberFormat.format(amount));
    }
}
