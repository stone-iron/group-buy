package com.livegroupbuy.backend.member;

import java.time.LocalDateTime;

public record ProfileResponse(Long id, String name, String email, MemberRole role, LocalDateTime createdAt) {
    public static ProfileResponse from(Member member) {
        return new ProfileResponse(member.getId(), member.getName(), member.getEmail(), member.getRole(), member.getCreatedAt());
    }
}
