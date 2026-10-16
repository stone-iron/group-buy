package com.livegroupbuy.backend.member;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.livegroupbuy.backend.auth.LoginMember;
import com.livegroupbuy.backend.auth.LoginSessionManager;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;
    private final LoginSessionManager loginSessionManager;

    public MemberController(MemberService memberService, LoginSessionManager loginSessionManager) {
        this.memberService = memberService;
        this.loginSessionManager = loginSessionManager;
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request,
                                                 HttpServletRequest servletRequest,
                                                 HttpServletResponse servletResponse) {
        SignupResponse member = memberService.signup(request);
        // 화면은 가입 직후 바로 로그인 상태로 넘어가므로 서버 세션도 함께 만든다.
        loginSessionManager.signIn(member.id(), member.name(), member.email(), member.role(),
                servletRequest, servletResponse);
        return ResponseEntity.status(HttpStatus.CREATED).body(member);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request,
                               HttpServletRequest servletRequest,
                               HttpServletResponse servletResponse) {
        LoginResponse member = memberService.login(request);
        loginSessionManager.signIn(member.id(), member.name(), member.email(), member.role(),
                servletRequest, servletResponse);
        return member;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest servletRequest) {
        loginSessionManager.signOut(servletRequest);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public LoginResponse me(@AuthenticationPrincipal LoginMember member) {
        LoginMember current = LoginSessionManager.require(member);
        return new LoginResponse(current.id(), current.name(), current.email(), current.role());
    }

    @GetMapping("/{memberId}/profile")
    public ProfileResponse getProfile(@PathVariable Long memberId, @AuthenticationPrincipal LoginMember member) {
        requireSelfOrAdmin(memberId, member);
        return memberService.getProfile(memberId);
    }

    @PutMapping("/{memberId}/profile")
    public ProfileResponse updateProfile(@PathVariable Long memberId,
                                         @Valid @RequestBody UpdateProfileRequest request,
                                         @AuthenticationPrincipal LoginMember member) {
        requireSelfOrAdmin(memberId, member);
        return memberService.updateProfile(memberId, request);
    }

    @PutMapping("/{memberId}/password")
    public ResponseEntity<Void> changePassword(@PathVariable Long memberId,
                                               @Valid @RequestBody ChangePasswordRequest request,
                                               @AuthenticationPrincipal LoginMember member) {
        requireSelfOrAdmin(memberId, member);
        memberService.changePassword(memberId, request);
        return ResponseEntity.noContent().build();
    }

    private void requireSelfOrAdmin(Long memberId, LoginMember member) {
        LoginMember current = LoginSessionManager.require(member);
        if (!current.id().equals(memberId) && !current.isAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인 정보만 조회하거나 수정할 수 있습니다.");
        }
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("입력값을 확인해 주세요.");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
