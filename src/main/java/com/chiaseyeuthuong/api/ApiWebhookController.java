package com.chiaseyeuthuong.api;

import com.chiaseyeuthuong.service.PayOSService;
import com.chiaseyeuthuong.service.impl.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
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
@Slf4j(topic = "API-WEBHOOK-CONTROLLER")
@RequestMapping("/api/webhooks")
public class ApiWebhookController {

    private final WebhookService webhookService;
    private final PayOS payOS;

    @Operation(
            summary = "PayOS webhook (internal)",
            description = "This endpoint is called by PayOS, not by client"
    )
    @PostMapping("/payos-ipn")
    public void processWebhook(@RequestBody Map<String, Object> body) {
        log.info("PayOS return received: {}", body);
        WebhookData data = payOS.webhooks().verify(body);
        webhookService.processWebhookPayOS(data);
    }
}
