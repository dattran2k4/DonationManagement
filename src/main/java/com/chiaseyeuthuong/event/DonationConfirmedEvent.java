package com.chiaseyeuthuong.event;

import vn.payos.model.webhooks.WebhookData;

public record DonationConfirmedEvent(Long donationId, WebhookData webhookData) {
}
