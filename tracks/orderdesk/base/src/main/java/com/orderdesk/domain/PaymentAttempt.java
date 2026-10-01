package com.orderdesk.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "payment_attempt")
public class PaymentAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    private BigDecimal amount;
    private String outcome;

    @Column(name = "created_at")
    private Instant createdAt;

    protected PaymentAttempt() {
    }

    public PaymentAttempt(Long orderId, BigDecimal amount, String outcome, Instant createdAt) {
        this.orderId = orderId;
        this.amount = amount;
        this.outcome = outcome;
        this.createdAt = createdAt;
    }

    public String getOutcome() {
        return outcome;
    }
}
