package com.livegroupbuy.backend.purchase;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/*
 * 인덱스가 없으면 MySQL은 FOR UPDATE 조회 때 테이블 전체를 훑으며 관계없는 행까지 잠근다.
 * 그러면 대기열 처리(WAITING 조회)가 결제 중인 RESERVED 티켓을 잠그거나,
 * 만료 처리가 새 티켓 INSERT를 막아 데드락이 생긴다. 아래 인덱스로 조회 범위를 필요한 행으로 좁힌다.
 */
@Entity
@Table(name = "purchase_queue_tickets",
        uniqueConstraints = @UniqueConstraint(name = "uk_purchase_queue_request_key", columnNames = "requestKey"),
        indexes = {
                // 상품별 다음 대기 티켓 조회 (processNext)
                @Index(name = "ix_ticket_product_status_id", columnList = "productId, status, id"),
                // 상품별 만료된 예약 조회 (processNext)
                @Index(name = "ix_ticket_product_status_expires", columnList = "productId, status, expiresAt"),
                // 회원별 활성 티켓·구매 수량 조회 (enqueue)
                @Index(name = "ix_ticket_product_member_status", columnList = "productId, memberId, status")
        })
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
