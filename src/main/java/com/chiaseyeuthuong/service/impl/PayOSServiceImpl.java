package com.chiaseyeuthuong.service.impl;

import com.chiaseyeuthuong.config.PayOSConfig;
import com.chiaseyeuthuong.exception.InvalidDataException;
import com.chiaseyeuthuong.exception.ResourceNotFoundException;
import com.chiaseyeuthuong.model.Donation;
import com.chiaseyeuthuong.repository.DonationRepository;
import com.chiaseyeuthuong.service.PayOSService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "PAYOS-SERVICE")
public class PayOSServiceImpl implements PayOSService {

    private final DonationRepository donationRepository;
    private final PayOSConfig payOSConfig;
    private final PayOS payOS;

    @Override
    public String createPaymentLink(String donationMemoCode) {
        log.info("Processing create payment link for donationMemoCode {}", donationMemoCode);

        Donation donation = donationRepository.findByMemoCode(donationMemoCode).orElseThrow(() -> new ResourceNotFoundException("Donation not found"));

        Long code = donation.getOrderCode();
        BigDecimal amountToPay = donation.getAmount();
        String description = donation.getMemoCode();

        PaymentLinkItem item = PaymentLinkItem.builder()
                .name("Đóng góp từ thiện chia sẻ yêu thương")
                .quantity(1)
                .price(amountToPay.longValue())
                .build();

        CreatePaymentLinkRequest paymentData = CreatePaymentLinkRequest.builder()
                .orderCode(code)
                .amount(amountToPay.longValue())
                .description(description)
                .returnUrl(payOSConfig.getReturnPaymentSuccessUrl())
                .cancelUrl(payOSConfig.getReturnPaymentCancelUrl())
                .item(item)
                .build();

        try {
            CreatePaymentLinkResponse data = payOS.paymentRequests().create(paymentData);
            log.info("Created payment link successfully for donationId {}", donationMemoCode);
            return data.getCheckoutUrl();
        } catch (Exception ex) {
            log.error("Payment link could not be created, message={}", ex.getMessage(), ex);
            throw new InvalidDataException("Cannot create PayOS payment link", ex);
        }
    }
}
