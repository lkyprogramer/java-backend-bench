package com.orderdesk.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id")
    private String customerId;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    private BigDecimal total;

    @Column(name = "paid_amount")
    private BigDecimal paidAmount;

    @Column(name = "refunded_amount")
    private BigDecimal refundedAmount;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sku")
    private List<OrderLine> lines = new ArrayList<>();

    protected Order() {
    }

    public Order(String customerId, Instant createdAt) {
        this.customerId = customerId;
        this.createdAt = createdAt;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.total = BigDecimal.ZERO;
        this.paidAmount = BigDecimal.ZERO;
        this.refundedAmount = BigDecimal.ZERO;
    }

    public void addLine(OrderLine line) {
        line.setOrder(this);
        lines.add(line);
        total = total.add(line.getLineTotal());
    }

    public Long getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getRefundedAmount() {
        return refundedAmount;
    }

    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount;
    }

    public Instant getShippedAt() {
        return shippedAt;
    }

    public void setShippedAt(Instant shippedAt) {
        this.shippedAt = shippedAt;
    }

    public List<OrderLine> getLines() {
        return lines;
    }
}
