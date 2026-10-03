package com.livegroupbuy.backend.member;

import java.util.Map;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class SocialLoginController {

    @GetMapping("/social-session")
    public ResponseEntity<?> socialSession(HttpSession session) {
        Object member = session.getAttribute(SocialLoginSuccessHandler.SOCIAL_MEMBER_SESSION_KEY);
        if (!(member instanceof LoginResponse response)) {
            return ResponseEntity.status(401).body(Map.of("message", "소셜 로그인 정보를 찾을 수 없습니다."));
        }
        session.removeAttribute(SocialLoginSuccessHandler.SOCIAL_MEMBER_SESSION_KEY);
        return ResponseEntity.ok(response);
    }
}
