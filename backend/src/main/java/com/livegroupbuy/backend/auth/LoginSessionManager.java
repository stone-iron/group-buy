package com.livegroupbuy.backend.auth;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.livegroupbuy.backend.member.MemberRole;

/**
 * 이메일 로그인·회원가입·소셜 로그인이 끝난 뒤 서버 세션에 로그인 정보를 저장한다.
 * 이후 요청은 세션 쿠키만으로 회원을 식별하므로, 클라이언트가 회원 번호를 보낼 필요가 없다.
 */
@Component
public class LoginSessionManager {

    private final SecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();
    private final SecurityContextHolderStrategy holderStrategy = SecurityContextHolder.getContextHolderStrategy();

    public LoginMember signIn(Long id, String name, String email, MemberRole role,
                              HttpServletRequest request, HttpServletResponse response) {
        // 로그인 전에 쓰던 세션 ID를 그대로 쓰면 세션 고정 공격에 노출되므로 새 ID로 바꾼다.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        LoginMember member = new LoginMember(id, name, email, role);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                member, null, List.of(new SimpleGrantedAuthority("ROLE_" + role.name()))
        );
        SecurityContext context = holderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        holderStrategy.setContext(context);
        contextRepository.saveContext(context, request, response);
        return member;
    }

    public void signOut(HttpServletRequest request) {
        holderStrategy.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    /** 컨트롤러에서 로그인 회원이 반드시 있어야 할 때 사용한다. */
    public static LoginMember require(LoginMember member) {
        if (member == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return member;
    }
}
