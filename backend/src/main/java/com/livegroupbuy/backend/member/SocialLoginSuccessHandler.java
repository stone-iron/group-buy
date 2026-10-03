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

@Component
public class SocialLoginSuccessHandler implements AuthenticationSuccessHandler {

    public static final String SOCIAL_MEMBER_SESSION_KEY = "SOCIAL_MEMBER";

    private final MemberService memberService;
    private final String frontendUrl;

    public SocialLoginSuccessHandler(
            MemberService memberService,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) {
        this.memberService = memberService;
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
