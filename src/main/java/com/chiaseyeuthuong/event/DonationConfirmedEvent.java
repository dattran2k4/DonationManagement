package com.chiaseyeuthuong.event;

import lombok.Getter;
import vn.payos.model.webhooks.WebhookData;

@Getter
public class DonationConfirmedEvent {
    private final Long donationId;
    private final WebhookData webhookData;

    public DonationConfirmedEvent(Long donationId, WebhookData webhookData) {
        this.donationId = donationId;
        this.webhookData = webhookData;
    }
}
