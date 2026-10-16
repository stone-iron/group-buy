package com.livegroupbuy.backend.purchase;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 취소 요청자. 컨트롤러가 로그인 세션의 회원 번호로 만든다. */
public record PurchaseCancelRequest(@NotNull @Positive Long memberId) {
}
