package com.livegroupbuy.backend.auth;

import java.io.Serializable;

import com.livegroupbuy.backend.member.MemberRole;

/**
 * 로그인한 회원 정보. 세션의 SecurityContext에 저장되고,
 * 컨트롤러에서는 {@code @AuthenticationPrincipal LoginMember member}로 꺼내 쓴다.
 * 회원 번호는 항상 여기서 가져오고, 요청 본문이나 쿼리 파라미터의 값은 믿지 않는다.
 */
public record LoginMember(Long id, String name, String email, MemberRole role) implements Serializable {

    public boolean isAdmin() {
        return role == MemberRole.ADMIN;
    }
}
