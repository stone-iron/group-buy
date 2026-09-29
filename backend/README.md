# Live Group Buy Backend

실시간 라이브커머스형 한정 수량 공동구매 플랫폼의 Spring Boot 백엔드입니다.

## 기술 구성

- Java 25
- Spring Boot 4.1.0
- Maven
- Spring Web
- Spring Data JPA
- Bean Validation
- MySQL

## 데이터베이스 생성

```sql
CREATE DATABASE live_group_buy
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
```

기본 연결 정보는 `root` 계정과 빈 비밀번호입니다. 비밀번호가 있다면 실행 전에 환경 변수로 전달합니다.

```bash
export DB_USERNAME=root
export DB_PASSWORD='내 MySQL 비밀번호'
```

## 실행

```bash
./mvnw spring-boot:run
```

서버 실행 후 상태 확인:

```text
http://localhost:8080/api/health
```
