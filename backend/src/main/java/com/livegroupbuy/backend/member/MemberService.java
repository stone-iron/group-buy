package com.livegroupbuy.backend.member;

import java.util.Locale;
import java.util.UUID;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberService {

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        if (memberRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }
        if (!request.password().equals(request.passwordConfirm())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        MemberRole role = switch (request.role()) {
            case BUYER -> MemberRole.BUYER;
            case SELLER -> MemberRole.SELLER;
        };
        Member member = new Member(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role
        );
        return SignupResponse.from(memberRepository.save(member));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String identifier = request.identifier().trim().toLowerCase(Locale.ROOT);
        Member member = memberRepository.findByEmailIgnoreCase(identifier)
                .orElseThrow(() -> new IllegalArgumentException("로그인 정보 또는 비밀번호가 올바르지 않습니다."));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new IllegalArgumentException("로그인 정보 또는 비밀번호가 올바르지 않습니다.");
        }
        return LoginResponse.from(member);
    }

    @Transactional
    public LoginResponse socialLogin(String provider, String providerId, String name, String email) {
        String normalizedEmail = email == null || email.isBlank()
                ? provider + "-" + providerId + "@social.yeogimoyeo.local"
                : email.trim().toLowerCase(Locale.ROOT);

        Member member = memberRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseGet(() -> memberRepository.save(new Member(
                        name == null || name.isBlank() ? "여기모여 회원" : name.trim(),
                        normalizedEmail,
                        passwordEncoder.encode(UUID.randomUUID().toString()),
                        MemberRole.BUYER
                )));
        return LoginResponse.from(member);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long memberId) {
        return ProfileResponse.from(findMember(memberId));
    }

    @Transactional
    public ProfileResponse updateProfile(Long memberId, UpdateProfileRequest request) {
        Member member = findMember(memberId);
        verifyCurrentPassword(member, request.currentPassword());

        String name = request.name().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (member.getRole() != MemberRole.ADMIN && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("올바른 이메일 형식으로 입력해 주세요.");
        }
        if (memberRepository.existsByEmailIgnoreCaseAndIdNot(email, memberId)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        member.updateProfile(name, email);
        return ProfileResponse.from(member);
    }

    @Transactional
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = findMember(memberId);
        verifyCurrentPassword(member, request.currentPassword());
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new IllegalArgumentException("새 비밀번호가 일치하지 않습니다.");
        }
        if (passwordEncoder.matches(request.newPassword(), member.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호와 다른 비밀번호를 입력해 주세요.");
        }
        member.changePasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    private Member findMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원 정보를 찾을 수 없습니다."));
    }

    private void verifyCurrentPassword(Member member, String currentPassword) {
        if (!passwordEncoder.matches(currentPassword, member.getPasswordHash())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
    }
}
