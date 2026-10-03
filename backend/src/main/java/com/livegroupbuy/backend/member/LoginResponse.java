package com.livegroupbuy.backend.member;

public record LoginResponse(Long id, String name, String email, MemberRole role) {
    public static LoginResponse from(Member member) {
        return new LoginResponse(member.getId(), member.getName(), member.getEmail(), member.getRole());
    }
}
