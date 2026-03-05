package com.chiaseyeuthuong.dto.response;

import com.chiaseyeuthuong.common.EPaymentMethod;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class TransactionResponse {

    private Long id;

    private String accountBankId;

    private String counterAccountNumber;

    private String counterAccountName;

    private String description;

    private BigDecimal amount;

    private String transactionCode;

    private EPaymentMethod paymentMethod;

//    @Column(name = "status")
//    @Enumerated(EnumType.STRING)
//    private ETransactionStatus status;

    private String transactionDateTime;

    private String rawApiData; //webhook callback

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
