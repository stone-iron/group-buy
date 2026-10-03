package com.livegroupbuy.backend.purchase;

public record InventoryResponse(
        Long productId,
        String productTitle,
        int totalQuantity,
        int soldQuantity,
        int reservedQuantity,
        int remainingQuantity,
        int availableQuantity
) {
    static InventoryResponse from(ProductInventory inventory) {
        return new InventoryResponse(
                inventory.getProductId(),
                inventory.getProductTitle(),
                inventory.getTotalQuantity(),
                inventory.getSoldQuantity(),
                inventory.getReservedQuantity(),
                inventory.remainingQuantity(),
                inventory.availableQuantity()
        );
    }
}
