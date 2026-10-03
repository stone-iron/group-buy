# Live Group Buy Backend

실시간 라이브커머스형 한정 수량 공동구매 플랫폼의 Spring Boot 백엔드입니다.

## 기술 구성

- Java 25
- Spring Boot 4.1.0
- Maven
- Spring Web
- Spring Data JPA
- Spring Data Redis
- Bean Validation
- MySQL
- Redis 7

## Redis 실행

한정판매 구매 요청의 사용자·IP별 속도 제한에 Redis를 사용합니다. 프로젝트 루트에서 실행합니다.

```bash
docker compose up -d redis
```

Redis가 일시적으로 꺼져도 요청은 차단하지 않고 DB의 수량 제한과 재고 잠금은 계속 적용됩니다.
미결제 예약 만료 횟수와 차단 종료 시각은 MySQL에 저장되므로 Redis 재시작과 관계없이 유지됩니다.

기본 정책은 사용자당 초당 3회, IP당 초당 20회입니다. 최근 24시간 동안 자동 미결제 만료가 3회 발생하면 30분, 5회 발생하면 24시간 동안 한정판매 참여가 제한됩니다. 사용자가 직접 취소한 요청과 토스 결제 실패는 패널티에 포함되지 않습니다.

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

토스페이먼츠 개발자센터에서 발급한 테스트 시크릿 키를 백엔드 환경변수로 설정합니다. 시크릿 키는 프런트엔드 파일이나 Git에 저장하지 않습니다.

```bash
export TOSS_SECRET_KEY='test_sk_발급받은_값'
```

```bash
./mvnw spring-boot:run
```

서버 실행 후 상태 확인:

```text
http://localhost:8080/api/health
```
