package com.livegroupbuy.backend.member;

import jakarta.validation.constraints.NotBlank;
public record LoginRequest(
        @NotBlank(message = "아이디 또는 이메일을 입력해 주세요.")
        String identifier,

        @NotBlank(message = "비밀번호를 입력해 주세요.")
        String password
) {
}
