# Run Checklist

프로젝트 전체 기능을 실행하기 전에 아래 항목을 순서대로 점검하면 됩니다.

## 1. Java / Gradle

- Java 17 설치 여부 확인
- `JAVA_HOME` 설정 확인
- Gradle wrapper 실행 가능 여부 확인

```powershell
java -version
echo $env:JAVA_HOME
.\gradlew.bat -v
```

## 2. DB 스키마 반영

- 현재 애플리케이션은 `ddl-auto: validate` 이므로 런타임 DB와 엔티티 스키마가 일치해야 합니다.
- 장소 동기화 기능을 위해 `place.google_synced_at` 컬럼이 반드시 존재해야 합니다.
- 아래 SQL을 운영 DB에 먼저 적용하세요.

파일:

- [`docs/sql/2026-04-10-add-google-synced-at-to-place.sql`](C:\Users\sangsu\IdeaProjects\picnee_back\docs\sql\2026-04-10-add-google-synced-at-to-place.sql)

## 3. Redis

- Redis가 `localhost:6379` 에서 실행 중인지 확인
- 장소 캐시, refresh token, OAuth auth token 저장에 사용됩니다.

```powershell
Test-NetConnection -ComputerName localhost -Port 6379
```

## 4. 환경 변수 / Secret 설정

- DB 접속 정보
- JWT secret
- OAuth2 client 설정
- `GOOGLE_PLACE_API_KEY`

특히 Google Place 재동기화는 API Key가 없으면 외부 조회가 동작하지 않습니다.  
이 경우 DB/캐시 데이터만 사용하게 됩니다.

## 5. 애플리케이션 실행

```powershell
.\gradlew.bat bootRun
```

확인 포인트:

- schema validation 오류 없이 시작되는지
- Redis 연결 오류가 없는지
- OAuth / JWT / Swagger 관련 Bean 생성 오류가 없는지

## 6. 테스트 실행

전체 테스트:

```powershell
.\gradlew.bat test
```

장소 조회 테스트만 실행:

```powershell
.\gradlew.bat test --tests com.picnee.travel.domain.place.service.PlaceServiceTest
```

## 7. 기능별 스모크 체크

### 사용자

- `POST /users` 회원가입
- `POST /users/login` 로그인
- `POST /tokens/reissue` Access Token 재발급

### 장소

- `POST /places` 장소 생성
- `GET /places/{placeId}` 장소 상세 조회
- 동일 `placeId` 재호출 시 캐시 hit 여부 로그 확인

### 게시글 / 댓글

- `POST /posts`
- `GET /posts`
- `POST /posts/{postId}/comments`

### 리뷰

- 리뷰 생성
- 리뷰 목록 조회
- 리뷰 좋아요 / 평가

### 알림

- 댓글 작성 또는 리뷰 평가 후 알림 생성 확인
- `GET /notifications` 미확인 알림 확인

## 8. 로그 확인 포인트

- 요청 로그 출력 여부
- P6Spy SQL 로그 출력 여부
- 장소 상세 조회 시 아래 시나리오가 구분되는지
  - Redis hit
  - DB fallback
  - Google API 재동기화

## 9. 현재 검증 상태

- 현재 작업 환경에는 JDK가 없어 테스트를 실제 실행하지 못했습니다.
- 따라서 최종 확인은 로컬 또는 CI 환경에서 위 체크리스트 기준으로 한 번 더 수행하는 것을 권장합니다.
