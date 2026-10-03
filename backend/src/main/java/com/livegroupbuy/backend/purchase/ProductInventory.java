package com.livegroupbuy.backend.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_inventories")
public class ProductInventory {

    @Id
    private Long productId;

    @Column(nullable = false, length = 160)
    private String productTitle;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int soldQuantity;

    @Column(nullable = false)
    private int reservedQuantity;

    @Column(nullable = false)
    private int unitPrice;

    protected ProductInventory() {
    }

    public ProductInventory(Long productId, String productTitle, int totalQuantity, int soldQuantity) {
        this(productId, productTitle, totalQuantity, soldQuantity, 1);
    }

    public ProductInventory(Long productId, String productTitle, int totalQuantity, int soldQuantity, int unitPrice) {
        if (totalQuantity < 0 || soldQuantity < 0 || soldQuantity > totalQuantity) {
            throw new IllegalArgumentException("재고 수량이 올바르지 않습니다.");
        }
        if (unitPrice <= 0) throw new IllegalArgumentException("상품 가격은 0원보다 커야 합니다.");
        this.productId = productId;
        this.productTitle = productTitle;
        this.totalQuantity = totalQuantity;
        this.soldQuantity = soldQuantity;
        this.reservedQuantity = 0;
        this.unitPrice = unitPrice;
    }

    public void updateProductInfo(String productTitle, int unitPrice) {
        if (unitPrice <= 0) throw new IllegalArgumentException("상품 가격은 0원보다 커야 합니다.");
        this.productTitle = productTitle;
        this.unitPrice = unitPrice;
    }

    public boolean reserve(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("구매 수량은 1개 이상이어야 합니다.");
        if (availableQuantity() < quantity) return false;
        reservedQuantity += quantity;
        return true;
    }

    public void confirmReservation(int quantity) {
        if (quantity <= 0 || reservedQuantity < quantity) {
            throw new IllegalStateException("확정할 예약 수량이 부족합니다.");
        }
        reservedQuantity -= quantity;
        soldQuantity += quantity;
    }

    public void releaseReservation(int quantity) {
        if (quantity <= 0 || reservedQuantity < quantity) {
            throw new IllegalStateException("반환할 예약 수량이 부족합니다.");
        }
        reservedQuantity -= quantity;
    }

    public int remainingQuantity() {
        return totalQuantity - soldQuantity;
    }

    public int availableQuantity() {
        return totalQuantity - soldQuantity - reservedQuantity;
    }

    public Long getProductId() { return productId; }
    public String getProductTitle() { return productTitle; }
    public int getTotalQuantity() { return totalQuantity; }
    public int getSoldQuantity() { return soldQuantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public int getUnitPrice() { return unitPrice; }
}
