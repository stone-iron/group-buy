package com.livegroupbuy.backend.member;

public record SignupResponse(Long id, String name, String email, MemberRole role) {
    public static SignupResponse from(Member member) {
        return new SignupResponse(member.getId(), member.getName(), member.getEmail(), member.getRole());
    }
}
