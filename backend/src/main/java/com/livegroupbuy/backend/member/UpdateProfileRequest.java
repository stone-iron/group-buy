package com.livegroupbuy.backend.member;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 50, message = "이름은 50자 이하로 입력해 주세요.")
        String name,

        @NotBlank(message = "이메일 또는 아이디를 입력해 주세요.")
        @Size(max = 190, message = "이메일 또는 아이디가 너무 깁니다.")
        String email,

        @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
        String currentPassword
) {
}
