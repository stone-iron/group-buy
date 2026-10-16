package com.livegroupbuy.backend.member;

import java.io.IOException;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.livegroupbuy.backend.auth.LoginSessionManager;

@Component
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SOCIAL_MEMBER_SESSION_KEY = "SOCIAL_MEMBER";

    private final MemberService memberService;
    private final LoginSessionManager loginSessionManager;
    private final String frontendUrl;

    public SocialLoginSuccessHandler(
            MemberService memberService,
            LoginSessionManager loginSessionManager,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.memberService = memberService;
        this.loginSessionManager = loginSessionManager;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        OAuth2User user = token.getPrincipal();
        String provider = token.getAuthorizedClientRegistrationId();
        SocialProfile profile = extractProfile(provider, user.getAttributes());
        LoginResponse member = memberService.socialLogin(provider, profile.id(), profile.name(), profile.email());

        // OAuth2 인증 정보 대신 우리 회원 정보로 세션 로그인 상태를 만든다.
        loginSessionManager.signIn(member.id(), member.name(), member.email(), member.role(), request, response);
        request.getSession(true).setAttribute(SOCIAL_MEMBER_SESSION_KEY, member);
        response.sendRedirect(frontendUrl + "/?social=success");
    }

    @SuppressWarnings("unchecked")
    private SocialProfile extractProfile(String provider, Map<String, Object> attributes) {
        if ("kakao".equals(provider)) {
            Map<String, Object> account = (Map<String, Object>) attributes.getOrDefault("kakao_account", Map.of());
            Map<String, Object> profile = (Map<String, Object>) account.getOrDefault("profile", Map.of());
            return new SocialProfile(
                    String.valueOf(attributes.get("id")),
                    stringValue(profile.get("nickname")),
                    stringValue(account.get("email"))
            );
        }
        if ("naver".equals(provider)) {
            Map<String, Object> profile = (Map<String, Object>) attributes.getOrDefault("response", Map.of());
            return new SocialProfile(
                    stringValue(profile.get("id")),
                    stringValue(profile.get("name")),
                    stringValue(profile.get("email"))
            );
        }
        return new SocialProfile(
                stringValue(attributes.get("sub")),
                stringValue(attributes.get("name")),
                stringValue(attributes.get("email"))
        );
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private record SocialProfile(String id, String name, String email) {
    }
}
