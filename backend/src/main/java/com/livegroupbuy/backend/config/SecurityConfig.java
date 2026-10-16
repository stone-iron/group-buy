package com.livegroupbuy.backend.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import com.livegroupbuy.backend.member.SocialLoginSuccessHandler;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] SELLER_OR_ADMIN = {"SELLER", "ADMIN"};

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SocialLoginSuccessHandler successHandler,
            @Value("${app.frontend-url:http://localhost:3000}") String frontendUrl
    ) throws Exception {
        http
                // TODO: 세션 쿠키 기반 인증으로 바꿨으므로 다음 단계에서 CSRF 토큰을 적용한다.
                //       지금은 세션 쿠키를 SameSite=Lax로 내려 다른 사이트에서 보낸 POST 요청에 쿠키가 실리지 않게 막는다.
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(authorize -> authorize
                        // 로그인 없이 쓸 수 있는 요청
                        .requestMatchers("/api/health", "/ws/**", "/oauth2/**", "/login/**", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/members/signup", "/api/members/login", "/api/members/logout").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/social-session").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/purchase-queue/inventories", "/api/group-buys", "/api/live-broadcasts").permitAll()

                        // 상품 등록·가격 변경, 방송 관리, 판매자용 참여자 조회는 판매자·관리자만
                        .requestMatchers(HttpMethod.POST,
                                "/api/purchase-queue/inventories", "/api/group-buys/campaigns",
                                "/api/live-broadcasts", "/api/live-broadcasts/import").hasAnyRole(SELLER_OR_ADMIN)
                        .requestMatchers(HttpMethod.PATCH, "/api/live-broadcasts/*/status").hasAnyRole(SELLER_OR_ADMIN)
                        .requestMatchers(HttpMethod.GET, "/api/group-buys/*/participations").hasAnyRole(SELLER_OR_ADMIN)

                        // 그 밖의 API(구매, 취소, 결제, 내 주문, 프로필 등)는 로그인 필요
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        // API 요청은 로그인 페이지로 리다이렉트하지 않고 JSON으로 응답한다.
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "로그인이 필요합니다."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "이 기능을 사용할 권한이 없습니다."))
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                        .failureHandler((request, response, exception) -> redirectFailure(response, frontendUrl))
                );
        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    private void redirectFailure(HttpServletResponse response, String frontendUrl) throws IOException {
        response.sendRedirect(frontendUrl + "/?social=error");
    }
}
