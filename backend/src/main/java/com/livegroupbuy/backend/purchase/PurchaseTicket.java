package com.livegroupbuy.backend.purchase;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "purchase_queue_tickets", uniqueConstraints =
        @UniqueConstraint(name = "uk_purchase_queue_request_key", columnNames = "requestKey"))
public class PurchaseTicket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, length = 80)
    private String requestKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private LocalDateTime expiresAt;

    protected PurchaseTicket() {
    }

    public PurchaseTicket(Long productId, Long memberId, int quantity, String requestKey) {
        this.productId = productId;
        this.memberId = memberId;
        this.quantity = quantity;
        this.requestKey = requestKey;
        this.status = PurchaseStatus.WAITING;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public void startProcessing() {
        if (status == PurchaseStatus.WAITING) status = PurchaseStatus.PROCESSING;
    }

    public void returnToWaiting() {
        if (status == PurchaseStatus.PROCESSING) status = PurchaseStatus.WAITING;
    }

    public void reserveUntil(LocalDateTime expiresAt) {
        status = PurchaseStatus.RESERVED;
        this.expiresAt = expiresAt;
    }

    public void confirm() {
        status = PurchaseStatus.CONFIRMED;
        processedAt = LocalDateTime.now();
        expiresAt = null;
    }

    public void expire() {
        status = PurchaseStatus.EXPIRED;
        processedAt = LocalDateTime.now();
        expiresAt = null;
    }

    public void cancel() {
        status = PurchaseStatus.CANCELLED;
        processedAt = LocalDateTime.now();
        expiresAt = null;
    }

    public void rejectAsSoldOut() {
        status = PurchaseStatus.SOLD_OUT;
        processedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getMemberId() { return memberId; }
    public int getQuantity() { return quantity; }
    public String getRequestKey() { return requestKey; }
    public PurchaseStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
