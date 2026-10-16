package com.livegroupbuy.backend.groupbuy;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record GroupBuyJoinRequest(
        Long memberId, // 서버가 로그인 세션 값으로 채운다.
        @Positive @Max(3) int quantity
) {
}
