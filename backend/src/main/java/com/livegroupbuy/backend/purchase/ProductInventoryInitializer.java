package com.livegroupbuy.backend.purchase;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ProductInventoryInitializer implements CommandLineRunner {

    private final ProductInventoryRepository repository;

    public ProductInventoryInitializer(ProductInventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        List<ProductInventory> defaults = List.of(
                new ProductInventory(1L, "제주 노지 감귤 5kg 산지직송", 40, 34, 23900),
                new ProductInventory(2L, "1++ 한우 불고기·국거리 세트", 80, 72, 49800),
                new ProductInventory(3L, "저온숙성 국산 참기름 2병", 30, 18, 31500),
                new ProductInventory(4L, "천연펄프 3겹 화장지 30롤", 50, 47, 19900),
                new ProductInventory(5L, "2026년 햅쌀 신동진 10kg", 70, 56, 34900),
                new ProductInventory(6L, "고당도 가정용 홍로 사과 3kg", 40, 29, 26900),
                new ProductInventory(7L, "스페셜티 원두 3종 골라담기 600g", 35, 21, 28900),
                new ProductInventory(8L, "국내산 손질 새우 1kg 냉동배송", 70, 63, 32900),
                new ProductInventory(9L, "매일견과 프리미엄 30봉 세트", 50, 38, 22900),
                new ProductInventory(10L, "엑스트라버진 올리브오일 500ml 2병", 60, 44, 39900),
                new ProductInventory(11L, "고농축 캡슐 세탁세제 60개입", 100, 81, 18900),
                new ProductInventory(12L, "프랑스 버터 크루아상 생지 20개", 30, 26, 21900)
        );
        defaults.forEach(inventory -> repository.findById(inventory.getProductId())
                .ifPresentOrElse(
                        existing -> {
                            existing.updateProductInfo(inventory.getProductTitle(), inventory.getUnitPrice());
                            repository.save(existing);
                        },
                        () -> repository.save(inventory)
                ));
    }
}
