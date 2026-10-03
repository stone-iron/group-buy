package com.livegroupbuy.backend.live;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "live_broadcasts")
public class LiveBroadcast {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 50)
    private String seller;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false, length = 160)
    private String productTitle;

    @Column(nullable = false, length = 16)
    private String emoji;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LiveStatus status;

    @Column(nullable = false, length = 80)
    private String scheduledAt;

    @Column(nullable = false)
    private int viewers;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(length = 300)
    private String rejectionReason;

    @Column(nullable = false, unique = true, length = 100)
    private String streamKey;

    private Long startedAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected LiveBroadcast() {
    }

    public LiveBroadcast(String title, String seller, Long productId, String productTitle, String emoji,
                         String imageUrl, LiveStatus status, String scheduledAt, int viewers,
                         String description, String rejectionReason, String streamKey, Long startedAt) {
        this.title = title;
        this.seller = seller;
        this.productId = productId;
        this.productTitle = productTitle;
        this.emoji = emoji;
        this.imageUrl = imageUrl;
        this.status = status;
        this.scheduledAt = scheduledAt;
        this.viewers = viewers;
        this.description = description;
        this.rejectionReason = rejectionReason;
        this.streamKey = streamKey;
        this.startedAt = startedAt;
    }

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getSeller() { return seller; }
    public Long getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public String getEmoji() { return emoji; }
    public String getImageUrl() { return imageUrl; }
    public LiveStatus getStatus() { return status; }
    public String getScheduledAt() { return scheduledAt; }
    public int getViewers() { return viewers; }
    public String getDescription() { return description; }
    public String getRejectionReason() { return rejectionReason; }
    public String getStreamKey() { return streamKey; }
    public Long getStartedAt() { return startedAt; }

    public void updateStatus(LiveStatus status, String rejectionReason) {
        this.status = status;
        this.rejectionReason = status == LiveStatus.REJECTED ? rejectionReason : null;
        if (status == LiveStatus.LIVE) {
            this.startedAt = System.currentTimeMillis();
            this.viewers = Math.max(viewers, 1);
        } else if (status == LiveStatus.ENDED) {
            this.viewers = 0;
        }
    }
}
