package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.EPaymentMethod;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "donation_transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_bank_id")
    private String accountBankId;

    @Column(name = "counter_account_number")
    private String counterAccountNumber;

    @Column(name = "counter_account_name")
    private String counterAccountName;

    @Column(name = "description")
    private String description;

    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "transaction_code")
    private String transactionCode;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private EPaymentMethod paymentMethod;

//    @Column(name = "status")
//    @Enumerated(EnumType.STRING)
//    private ETransactionStatus status;

    @Column(name = "transaction_date_time")
    private String transactionDateTime;

    @Column(name = "raw_api_data", columnDefinition = "TEXT")
    private String rawApiData; //webhook callback

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne
    @JoinColumn(name = "donation_id")
    private Donation donation;
}
