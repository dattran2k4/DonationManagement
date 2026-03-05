package com.chiaseyeuthuong.model;

import com.chiaseyeuthuong.common.*;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "donations")
public class Donation {

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

    @Column(name = "approval_required")
    private Boolean approvalRequired;

    // Thay vì dùng ID thuần, dùng quan hệ để dễ dàng lấy thông tin Admin
    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    @ManyToOne
    @JoinColumn(name = "confirmed_by_user_id")
    private User confirmedBy;

    @Column(name = "donated_at")
    private LocalDateTime donatedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

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
