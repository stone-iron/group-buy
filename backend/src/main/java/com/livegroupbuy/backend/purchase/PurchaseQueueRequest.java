package com.livegroupbuy.backend.purchase;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PurchaseQueueRequest(
        @NotNull Long productId,
        Long memberId, // 서버가 로그인 세션 값으로 채운다. 클라이언트가 보낸 값은 쓰지 않는다.
        @Min(1) @Max(10) int quantity,
        @NotBlank @Size(max = 80) String requestKey
) {
}
