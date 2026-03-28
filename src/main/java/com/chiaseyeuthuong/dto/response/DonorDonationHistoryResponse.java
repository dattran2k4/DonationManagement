package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EDonationStatus;
import com.chiaseyeuthuong.common.EDonationTarget;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class DonorDonationHistoryResponse {
    private Long donationId;
    private String donationCode;
    private BigDecimal amount;
    private EDonationStatus status;
    private String statusLabel;
    private EDonationTarget target;
    private String targetLabel;
    private String targetTitle;
    private String targetUrl;
    private LocalDateTime donatedAt;
}
