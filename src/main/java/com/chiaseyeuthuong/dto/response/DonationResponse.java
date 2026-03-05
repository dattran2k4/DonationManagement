package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.*;
import com.chiaseyeuthuong.model.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DonationResponse {
    private Long id;
    private BigDecimal amount;
    private String message;
    private String memoCode;
    private Boolean needReceipt;
    private String receiptName;
    private String receiptEmail;
    private EPaymentMethod paymentMethod;
    private EDonationStatus status;
    private EDonationType type;
    private EDonationTarget target;
    private EDonationVia donationVia;
    private Boolean approvalRequired;
    private User createdBy;
    private User confirmedBy;
    private LocalDateTime donatedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime confirmedAt;
    private String donorName;
    private String objectName;
}
