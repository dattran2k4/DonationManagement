package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.dto.response.ApiResponse;
import com.chiaseyeuthuong.service.PayOSService;
import com.chiaseyeuthuong.service.impl.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.payos.PayOS;
import vn.payos.model.webhooks.WebhookData;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Validated
@Slf4j(topic = "API-PAYMENT-CONTROLLER")
@RequestMapping("/api/payments")
public class ApiPaymentController {

    private final PayOSService payOSService;
    private final WebhookService webhookService;
    private final PayOS payOS;

    @PostMapping
    public ApiResponse createPaymentUrl(@RequestBody Map<String, Object> param) {
        String paymentUrl = payOSService.createPaymentLink(param.get("donationMemoCode").toString());
        return ApiResponse.builder()
                .status(201)
                .message("Payment URL created")
                .data(paymentUrl)
                .build();
    }

    @PostMapping("/payos-ipn")
    public void processWebhook(@RequestBody Map<String, Object> body) {
        log.info("PayOS return received: {}", body);
        WebhookData data = payOS.webhooks().verify(body);
        webhookService.processWebhookPayOS(data);
    }
}
