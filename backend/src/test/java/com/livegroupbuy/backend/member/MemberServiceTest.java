package com.livegroupbuy.backend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class MemberServiceTest {

    @Test
    void signsUpBuyerWithNormalizedEmailAndHashedPassword() throws ReflectiveOperationException {
        AtomicReference<Member> savedMember = new AtomicReference<>();
        MemberService service = new MemberService(repositoryStub(savedMember));

        SignupResponse response = service.signup(new SignupRequest(
                "홍길동", " BUYER@EXAMPLE.COM ", "password123", "password123", SignupRequest.SignupRole.BUYER
        ));

        Field passwordHash = Member.class.getDeclaredField("passwordHash");
        passwordHash.setAccessible(true);
        assertThat(response.email()).isEqualTo("buyer@example.com");
        assertThat(response.role()).isEqualTo(MemberRole.BUYER);
        assertThat(passwordHash.get(savedMember.get())).isNotEqualTo("password123");
    }

    @Test
    void rejectsMismatchedPasswords() {
        MemberService service = new MemberService(repositoryStub(new AtomicReference<>()));

        assertThatThrownBy(() -> service.signup(new SignupRequest(
                "홍길동", "buyer@example.com", "password123", "different123", SignupRequest.SignupRole.BUYER
        ))).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("비밀번호가 일치하지 않습니다.");
    }

    @Test
    void logsInAdminWithValidCredentials() {
        Member admin = new Member(
                "admin",
                "admin",
                new BCryptPasswordEncoder().encode("12341234"),
                MemberRole.ADMIN
        );
        MemberService service = new MemberService(repositoryStub(new AtomicReference<>(admin)));

        LoginResponse response = service.login(new LoginRequest("admin", "12341234"));

        assertThat(response.email()).isEqualTo("admin");
        assertThat(response.role()).isEqualTo(MemberRole.ADMIN);
    }

    @Test
    void rejectsUnknownLoginId() {
        MemberService service = new MemberService(repositoryStub(new AtomicReference<>()));

        assertThatThrownBy(() -> service.login(new LoginRequest("a", "12341234")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("로그인 정보 또는 비밀번호가 올바르지 않습니다.");
    }

    @Test
    void updatesProfileAfterCheckingCurrentPassword() {
        Member member = new Member(
                "홍길동",
                "buyer@example.com",
                new BCryptPasswordEncoder().encode("password123"),
                MemberRole.BUYER
        );
        MemberService service = new MemberService(repositoryStub(new AtomicReference<>(member)));

        ProfileResponse response = service.updateProfile(null, new UpdateProfileRequest(
                "김모임", "NEW@EXAMPLE.COM", "password123"
        ));

        assertThat(response.name()).isEqualTo("김모임");
        assertThat(response.email()).isEqualTo("new@example.com");
    }

    @Test
    void changesPasswordAndRejectsTheOldPassword() {
        Member member = new Member(
                "홍길동",
                "buyer@example.com",
                new BCryptPasswordEncoder().encode("password123"),
                MemberRole.BUYER
        );
        MemberService service = new MemberService(repositoryStub(new AtomicReference<>(member)));

        service.changePassword(null, new ChangePasswordRequest("password123", "newPassword123", "newPassword123"));

        assertThatThrownBy(() -> service.login(new LoginRequest("buyer@example.com", "password123")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.login(new LoginRequest("buyer@example.com", "newPassword123")).name()).isEqualTo("홍길동");
    }

    private MemberRepository repositoryStub(AtomicReference<Member> savedMember) {
        return (MemberRepository) Proxy.newProxyInstance(
                MemberRepository.class.getClassLoader(),
                new Class<?>[]{MemberRepository.class},
                (proxy, method, arguments) -> switch (method.getName()) {
                    case "existsByEmailIgnoreCase" -> false;
                    case "existsByEmailIgnoreCaseAndIdNot" -> false;
                    case "findByEmailIgnoreCase" -> Optional.ofNullable(savedMember.get());
                    case "findById" -> Optional.ofNullable(savedMember.get());
                    case "save" -> {
                        Member member = (Member) arguments[0];
                        savedMember.set(member);
                        yield member;
                    }
                    case "toString" -> "MemberRepositoryStub";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == arguments[0];
                    default -> throw new UnsupportedOperationException(method.getName());
                }
        );
    }
}
