# Picnee Backend 프로젝트 요약

## 1. 프로젝트 개요

이 프로젝트는 Spring Boot 기반의 여행/장소 커뮤니티 백엔드입니다.  
주요 기능은 다음 축으로 구성되어 있습니다.

- 사용자 회원가입, 로그인, OAuth2 로그인
- JWT 기반 인증/인가와 토큰 재발급
- 장소 등록 및 조건 기반 장소 조회
- 리뷰 등록/수정/삭제/좋아요/평가
- 게시글 등록/수정/삭제/조회와 댓글 관리
- 알림 생성 및 미확인 알림 조회

코드베이스는 전형적인 Spring 계층형 아키텍처를 따르면서도, 알림과 일부 조회 로직은 이벤트/커스텀 리포지토리로 분리해 관심사를 나누고 있습니다.

## 2. 기술 스택

### Backend Core

- Java 17
- Spring Boot 3.3.1
- Spring Web
- Spring Validation
- Spring Data JPA
- Spring Security
- Spring OAuth2 Client

### Persistence / Query

- JPA / Hibernate
- Querydsl
- MySQL / MariaDB 드라이버
- H2(test/runtime 지원)

### Auth / Infra

- JJWT
- Redis
- P6Spy

### Docs / Dev Experience

- Springdoc OpenAPI Swagger UI
- Lombok
- Gradle

## 3. 디렉터리 구조와 역할

```text
src/main/java/com/picnee/travel
├─ api
│  ├─ *Controller          : HTTP 엔드포인트
│  └─ in                   : Swagger 문서용 API 인터페이스
├─ domain
│  ├─ user/place/post/...  : 도메인별 entity, dto, repository, service, exception
│  └─ base                 : 공통 엔티티 추상화
└─ global
   ├─ config               : Security, Redis, Querydsl, Swagger 등 설정
   ├─ exception            : 전역 예외 처리
   ├─ jwt                  : JWT 필터, provider, DTO, properties
   ├─ oauth                : OAuth2 사용자 처리, 성공 핸들러
   ├─ redis                : Redis 프로퍼티와 서비스
   └─ security             : UserDetailsService, principal 어댑터, 커스텀 어노테이션
```

테스트는 `src/test/java` 아래에 서비스 중심으로 배치되어 있으며, 주요 비즈니스 흐름 검증에 초점이 맞춰져 있습니다.

## 4. 전반적인 아키텍처

이 프로젝트는 기본적으로 `Controller -> Service -> Repository -> DB` 흐름을 따르는 계층형 아키텍처입니다.

### 계층별 책임

- Controller
  - HTTP 요청/응답 담당
  - DTO 바인딩과 상태 코드 반환
  - 인증 사용자 주입
- Service
  - 비즈니스 규칙 처리
  - 권한 확인, 생성/수정/삭제, 이벤트 발행
  - 트랜잭션 경계 관리
- Repository
  - JPA 기본 CRUD
  - Querydsl 기반 동적 조회
- Entity
  - 도메인 상태와 핵심 행위 보유
  - soft delete, 카운트 증가, 상태 변경 등

## 5. 사용된 설계 패턴

### 5.1 계층형 아키텍처

가장 핵심적인 패턴입니다.  
예를 들어 게시글 기능은 다음처럼 역할이 분리됩니다.

- `PostController`: 요청 수신
- `PostService`: 작성자 검증, 조회수 증가, 삭제 정책 처리
- `PostRepository`: 조회/페이징/동적 정렬

장점은 도메인별 책임이 비교적 명확하고, 테스트 단위도 서비스 기준으로 잡기 쉽다는 점입니다.

### 5.2 도메인 중심 패키징

`user`, `post`, `review`, `notification`, `place`처럼 기능 기준으로 패키지를 나눴습니다.  
덕분에 각 도메인의 DTO, 엔티티, 서비스, 예외가 한곳에 모여 있어 추적이 쉽습니다.

### 5.3 DTO 분리 패턴

요청/응답 DTO를 엔티티와 분리했습니다.

- 요청 DTO: `CreatePostReq`, `LoginUserReq`, `CreatePlaceReq`
- 응답 DTO: `FindPostRes`, `JwtTokenRes`, `FindNotificationRes`

이 방식은 API 계약과 내부 엔티티 변경을 분리해 주고, 검증 어노테이션 적용에도 유리합니다.

### 5.4 커스텀 리포지토리 + Querydsl 패턴

단순 CRUD는 `JpaRepository`를 사용하고, 복잡한 검색은 `*RepositoryCustom`, `*RepositoryImpl`로 분리했습니다.

대표 예시:

- `PostRepositoryImpl`: 게시글 조건 검색, 정렬, 페이징
- `ReviewRepositoryImpl`: 리뷰 정렬/페이징
- `PlaceRepositoryImpl`: 장소 필터링, 리뷰/투표 기반 집계 조회

즉, “간단한 것은 Spring Data JPA”, “복잡한 조회는 Querydsl”이라는 방향으로 설계되어 있습니다.

### 5.5 이벤트 기반 분리

알림 생성은 직접 호출보다 이벤트 기반으로 느슨하게 연결되어 있습니다.

- 댓글 작성 시 `ApplicationEventPublisher`로 `PostCommentEvent` 발행
- `NotificationEventListener`가 `@TransactionalEventListener`로 수신
- `NotificationService`가 실제 알림 저장

이 패턴 덕분에 댓글/리뷰 기능이 알림 저장 구현에 강하게 결합되지 않습니다.

### 5.6 템플릿 성격의 공통 엔티티 추상화

공통 엔티티 필드를 `BaseEntity`, `SoftDeleteBaseEntity`로 추상화했습니다.

- `BaseEntity`
  - `createdAt`, `modifiedAt`
- `SoftDeleteBaseEntity`
  - `deletedAt`, `isDeleted`

여러 도메인에서 생성/수정 시각과 soft delete 정책을 일관되게 재사용할 수 있습니다.

### 5.7 어댑터 패턴 성격의 인증 사용자 래핑

Spring Security가 요구하는 `UserDetails` 형식에 맞추기 위해 `LoginUserAdapter`를 두고, 실제 애플리케이션 사용자 정보는 `AuthenticatedUserReq`로 감쌌습니다.

이는 보안 프레임워크 객체와 애플리케이션 DTO를 연결하는 브리지 역할을 합니다.

## 6. 인증/인가 구조

이 프로젝트의 인증 구조는 `Spring Security + JWT + Redis + OAuth2` 조합입니다.

### 6.1 로그인 방식

- 일반 로그인
  - 이메일/비밀번호 인증
  - Access Token / Refresh Token 발급
- OAuth2 로그인
  - 소셜 인증 완료 후 임시 auth token을 Redis에 저장
  - 별도 토큰 발급 API에서 JWT로 교환

### 6.2 SecurityFilterChain 동작

`SecurityConfig` 기준 흐름은 아래와 같습니다.

1. CSRF 비활성화
2. CORS 필터 선행 적용
3. `JwtFilter`를 `UsernamePasswordAuthenticationFilter` 앞에 배치
4. 인증 실패/인가 실패 핸들러 연결
5. OAuth2 로그인 사용자 서비스와 성공 핸들러 연결
6. 세션은 `STATELESS`로 설정

즉, 서버 세션에 의존하지 않는 토큰 기반 인증 구조입니다.

### 6.3 JWT 처리 방식

`JwtFilter`는 다음 헤더를 읽습니다.

- `AccessToken`
- `RefreshToken`

동작 방식:

1. 헤더에서 토큰 추출
2. `TokenProvider.validateToken()`으로 유효성 검증
3. 유효하면 `TokenProvider.getAuthentication()`으로 `Authentication` 생성
4. `SecurityContextHolder`에 저장

### 6.4 Redis 활용 방식

Redis는 주로 토큰성 데이터 저장에 사용됩니다.

- 일반 로그인 refresh token 저장
- OAuth 로그인 후 임시 auth token 저장

`RedisService`는 TTL 기반 저장을 제공하며, 만료 시간은 JWT 설정값을 따라갑니다.

### 6.5 컨트롤러에서 인증 사용자 사용법

커스텀 어노테이션 `@AuthenticatedUser`를 통해 인증 사용자를 바로 주입합니다.

예:

```java
public ResponseEntity<String> updateUser(
    @AuthenticatedUser AuthenticatedUserReq auth,
    @Valid @RequestBody UpdateUserReq dto
)
```

이 패턴은 컨트롤러에서 `SecurityContextHolder`를 직접 다루지 않게 해 줍니다.

## 7. 요청 흐름

### 7.1 일반 로그인 흐름

```text
Client
 -> POST /users/login
 -> UserController
 -> UserService.login()
 -> AuthenticationManager 인증
 -> TokenProvider가 access/refresh token 생성
 -> RedisService가 refresh token 저장
 -> 응답 본문 + HttpOnly Cookie 반환
```

추가로 로그인 실패 횟수를 누적하고, 5회 이상 실패하면 계정을 잠그는 정책이 들어가 있습니다.

### 7.2 JWT 인증 요청 흐름

```text
Client
 -> API 요청 + AccessToken 헤더
 -> JwtFilter
 -> TokenProvider.validateToken()
 -> TokenProvider.getAuthentication()
 -> SecurityContext 저장
 -> Controller 진입
 -> @AuthenticatedUser 로 사용자 정보 주입
 -> Service 비즈니스 로직 수행
```

### 7.3 OAuth2 로그인 흐름

```text
Client
 -> OAuth2 로그인
 -> CustomOauth2UserService
 -> 기존 사용자 조회 또는 신규 사용자 생성
 -> OAuth2UserSuccessHandler
 -> Redis에 auth token(UUID) 저장
 -> 프론트로 redirect
 -> POST /tokens
 -> TokenService.createOauthToken()
 -> auth token을 JWT로 교환
```

이 흐름은 소셜 로그인 직후 바로 JWT를 넘기지 않고, 중간 교환 단계를 둔 구조입니다.

### 7.4 게시글 조회 흐름

```text
GET /posts/{postId}
 -> PostController.findPost()
 -> PostService.find()
 -> 삭제 여부 확인
 -> 인증 사용자인 경우 최초 조회 여부 확인
 -> 조회수 증가
 -> FindPostRes 변환 후 응답
```

여기서 조회수는 `UsersPostService`를 통해 “사용자별 첫 조회”를 구분하는 방식으로 보입니다.

### 7.5 댓글 작성 시 알림 흐름

```text
POST /posts/{postId}/comments
 -> PostCommentService.create()
 -> PostCommentEvent 발행
 -> 트랜잭션 이벤트 리스너 수신
 -> NotificationService.create()
 -> 대상 소유자 조회
 -> Notification 저장
```

## 8. 도메인별 구현 특징

### 8.1 User

- 이메일/닉네임 중복 확인 API 제공
- 비밀번호는 `BCryptPasswordEncoder`로 암호화
- 로그인 실패 횟수 누적 및 잠금 처리
- 사용자 상태(`ACTIVE`, `LOCKED` 등) 관리

### 8.2 Post

- 게시판 카테고리와 지역 정보 포함
- 수정/삭제 시 작성자 검증
- soft delete 적용
- 게시글 목록은 조건 검색 + 페이징 + 정렬 지원

### 8.3 PostComment

- 댓글/대댓글 구조 지원
- 작성자 검증 후 수정/삭제
- 좋아요 토글 기능
- 댓글 작성 시 알림 이벤트 발행

### 8.4 Place

- 장소 생성과 운영 시간(`OpeningHours`) 분리 관리
- 지역, 타입, 정렬, 상세 필터 기반 조회 제공
- 복합 필터 조회는 Querydsl로 직접 구현

### 8.5 Review

- 장소 타입별 세부 투표 엔티티 분리
  - 음식점
  - 숙소
  - 관광지
- 리뷰 본문과 상세 평가 항목을 분리 저장
- 좋아요, 평가, 인기 리뷰 조회 지원

리뷰는 “공통 리뷰 + 타입별 상세 평가” 구조라서, 완전한 상속 전략보다는 조합에 가까운 모델링을 사용하고 있습니다.

### 8.6 Notification

- 댓글, 댓글 좋아요, 리뷰 평가 등의 이벤트를 알림으로 전환
- 읽지 않은 알림 목록 조회
- 수신자 본인만 읽음 처리 가능

## 9. 데이터 접근 전략

### 9.1 JPA 기본 사용

생성/수정/삭제와 단순 조회는 Spring Data JPA에 맡기고 있습니다.

예:

- `UserRepository`
- `PostRepository`
- `NotificationRepository`

### 9.2 Querydsl 사용 목적

복합 조건, 정렬, 집계, 페이징이 필요한 조회에서 Querydsl을 사용합니다.

예:

- 게시글 필터링과 정렬
- 장소 필터 기반 추천/리스트 조회
- 리뷰 정렬 조회

### 9.3 Soft Delete 전략

여러 도메인에서 실제 삭제 대신 `isDeleted` 플래그를 사용합니다.  
서비스 계층에서는 `findByIdNotDeleted...()` 메서드로 삭제된 데이터를 차단합니다.

장점:

- 복구 가능성 확보
- 참조 무결성 부담 감소
- 이력성 있는 데이터 관리에 유리

주의할 점:

- 모든 조회에서 삭제 조건이 빠지지 않도록 일관성이 중요

## 10. 예외 처리 전략

전역 예외 처리는 `GlobalExceptionHandler`에서 담당합니다.

### 구성

- 일반 예외 처리
- `NullPointerException`
- `MethodArgumentNotValidException`
- 커스텀 `BusinessException`
- 커스텀 `BusinessMessageException`

각 도메인에서는 `ErrorCode` 기반 예외를 던지고, 전역 핸들러가 `ErrorResponse` 형태로 통일합니다.

이 구조 덕분에 컨트롤러는 예외 응답 포맷을 직접 만들지 않아도 됩니다.

## 11. 문서화와 운영 편의성

### Swagger / OpenAPI

컨트롤러가 `api.in.*` 인터페이스를 구현하는 형태로 Swagger 문서를 분리했습니다.

장점:

- 컨트롤러 코드와 문서 어노테이션 분리
- API 설명 관리가 비교적 깔끔함

### Request Logging

`CommonsRequestLoggingFilter`를 통해 아래 정보를 로그로 남깁니다.

- Query String
- Payload
- Headers

### SQL Logging

P6Spy를 적용해 SQL과 실행 시간을 보기 좋게 포맷팅합니다.

## 12. 테스트 현황

테스트는 통합 테스트 성격의 서비스 테스트가 중심입니다.

확인된 테스트 범위:

- `UserServiceTest`
  - 회원가입
  - 로그인
  - 중복 검증
  - 닉네임 수정
- `PostServiceTest`
  - 생성/수정/삭제
  - 작성자 권한 검증
  - 게시글 조회/목록/조회수
- `NotificationServiceTest`
  - 알림 생성
  - 미확인 알림 조회

즉, 핵심 비즈니스 규칙 검증은 들어가 있지만, 인증 필터/컨트롤러/복합 Querydsl 조회에 대한 테스트는 더 보강할 여지가 있습니다.

## 13. 이 프로젝트의 장점

- 도메인 기준 패키징으로 기능 추적이 쉽다
- 계층 분리가 비교적 명확하다
- 인증 구조가 Spring Security 표준에 가깝다
- Querydsl을 적절히 도입해 복잡한 조회를 분리했다
- 이벤트 기반 알림 처리로 결합도를 낮췄다
- 공통 엔티티/예외/보안 구성이 재사용 가능하게 정리되어 있다

## 14. 개선 포인트

문서화 관점에서 함께 기록해 둘 만한 개선 포인트도 보입니다.

- `SecurityConfig`에서 현재 `anyRequest().permitAll()`이라 실제 인가 정책은 서비스 검증에 많이 의존함
- 토큰은 쿠키와 헤더가 혼재되어 있어 클라이언트 표준화가 필요할 수 있음
- 일부 서비스 메서드의 인증 체크 메서드 이름과 조건식이 직관적이지 않음
- `PlaceRepositoryImpl`는 동적 필터 로직이 복잡해 테스트 보강이 중요함
- 댓글/좋아요/리뷰 평가 이벤트가 더 늘어나면 이벤트 타입과 핸들러 분리가 더 필요할 수 있음

## 15. 새로 합류한 사람이 먼저 보면 좋은 파일

- `build.gradle`
- `src/main/resources/application.yml`
- `src/main/java/com/picnee/travel/global/config/SecurityConfig.java`
- `src/main/java/com/picnee/travel/global/jwt/provider/TokenProvider.java`
- `src/main/java/com/picnee/travel/api/UserController.java`
- `src/main/java/com/picnee/travel/domain/user/service/UserService.java`
- `src/main/java/com/picnee/travel/domain/post/service/PostService.java`
- `src/main/java/com/picnee/travel/domain/postComment/service/PostCommentService.java`
- `src/main/java/com/picnee/travel/domain/notification/service/NotificationService.java`
- `src/main/java/com/picnee/travel/domain/place/repository/PlaceRepositoryImpl.java`

## 16. 한 줄 정리

이 프로젝트는 Spring Boot 기반 여행 커뮤니티 백엔드로,  
계층형 아키텍처를 바탕으로 JWT/OAuth2 인증, Querydsl 기반 조회, 이벤트 기반 알림, soft delete 정책을 조합해 구현한 구조입니다.
