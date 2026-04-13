package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "donations")
public class Donation extends AbstractEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    @Column(name = "order_code")
    private Long orderCode;

    @Column(name = "memo_code")
    private String memoCode;

    @Column(name = "need_receipt")
    private Boolean needReceipt;

    @Column(name = "receipt_name")
    private String receiptName;

    @Column(name = "receipt_email")
    private String receiptEmail;

    @Column(name = "payment_method")
    @Enumerated(EnumType.STRING)
    private EPaymentMethod paymentMethod;

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private EDonationStatus status;

    @Column(name = "type")
    @Enumerated(EnumType.STRING)
    private EDonationType type;

    @Column(name = "target")
    @Enumerated(EnumType.STRING)
    private EDonationTarget target;

    @Enumerated(EnumType.STRING)
    @Column(name = "donation_via")
    private EDonationVia donationVia;

    @ManyToOne
    @JoinColumn(name = "confirmed_by_user_id")
    private User confirmedBy;

    @Column(name = "donated_at")
    private LocalDateTime donatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @ManyToOne
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne
    @JoinColumn(name = "activity_id")
    private Activity activity;

    @ManyToOne
    @JoinColumn(name = "donor_id")
    private Donor donor;

    @OneToOne(mappedBy = "donation")
    private Transaction transaction;
}
