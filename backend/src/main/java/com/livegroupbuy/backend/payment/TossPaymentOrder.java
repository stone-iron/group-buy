package com.livegroupbuy.backend.payment;

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
@Table(name = "toss_payment_orders", uniqueConstraints = {
        @UniqueConstraint(name = "uk_toss_payment_order_id", columnNames = "orderId"),
        @UniqueConstraint(name = "uk_toss_payment_ticket_id", columnNames = "ticketId")
})
public class TossPaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String orderId;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false, length = 100)
    private String orderName;

    @Column(nullable = false, length = 64)
    private String customerKey;

    @Column(length = 50)
    private String recipientName;

    @Column(length = 20)
    private String phone;

    @Column(length = 10)
    private String postalCode;

    @Column(length = 200)
    private String address;

    @Column(length = 200)
    private String addressDetail;

    @Column(length = 200)
    private String deliveryMessage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TossPaymentStatus status;

    @Column(length = 200)
    private String paymentKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime approvedAt;

    protected TossPaymentOrder() {
    }

    public TossPaymentOrder(String orderId, Long ticketId, Long memberId, long amount,
                            String orderName, String customerKey,
                            String recipientName, String phone, String postalCode,
                            String address, String addressDetail, String deliveryMessage) {
        this.orderId = orderId;
        this.ticketId = ticketId;
        this.memberId = memberId;
        this.amount = amount;
        this.orderName = orderName;
        this.customerKey = customerKey;
        updateShippingInformation(recipientName, phone, postalCode, address, addressDetail, deliveryMessage);
        this.status = TossPaymentStatus.READY;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public void approve(String paymentKey) {
        status = TossPaymentStatus.APPROVED;
        this.paymentKey = paymentKey;
        approvedAt = LocalDateTime.now();
    }

    public void updateShippingInformation(String recipientName, String phone, String postalCode,
                                          String address, String addressDetail, String deliveryMessage) {
        this.recipientName = recipientName;
        this.phone = phone;
        this.postalCode = postalCode;
        this.address = address;
        this.addressDetail = addressDetail;
        this.deliveryMessage = deliveryMessage;
    }

    public String getOrderId() { return orderId; }
    public Long getTicketId() { return ticketId; }
    public Long getMemberId() { return memberId; }
    public long getAmount() { return amount; }
    public String getOrderName() { return orderName; }
    public String getCustomerKey() { return customerKey; }
    public TossPaymentStatus getStatus() { return status; }
    public String getPaymentKey() { return paymentKey; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getPostalCode() { return postalCode; }
    public String getAddress() { return address; }
    public String getAddressDetail() { return addressDetail; }
    public String getDeliveryMessage() { return deliveryMessage; }
}
