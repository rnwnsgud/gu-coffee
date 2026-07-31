## 📱 Runtime Container

```
qc-api-container (Spring Boot Application)
├── Domain Modules Integration
├── REST API Endpoints Aggregation  
├── Dependency Injection & Bean Management
└── Application Configuration
```

**🚀 Container 역할 (`qc-api-container`)**

- 모든 도메인 모듈을 Spring Context에 로딩
- 도메인별 API 엔드포인트를 단일 애플리케이션으로 통합
- 공통 설정 및 인프라 구성 관리
- 실제 배포되는 실행 가능한 애플리케이션

---

## 🏗️ Core Domain Modules

- **qc-user** - 유저 도메인 (Core + API + Internal API + RDB + Redis + Social)
- **qc-question** - 문제 도메인 (Core + API + Internal API + RDB + Fixture)
- **qc-creator** - 크리에이터 도메인 (Core + API + Internal API + RDB)
- **qc-point** - 포인트 시스템 도메인 (Core + API + Internal API + RDB + PG)
- **qc-post** - 문제 게시판 도메인 (Core + API + Internal API + RDB)
- **qc-pay** - 결제 도메인 (Core + API + RDB + Mongo)
- **qc-cart** - 장바구니 도메인 (Core + API + RDB)
- **qc-review** - 리뷰 도메인 (Core + API + RDB)
- **qc-subscribe** - 구독 도메인 (Core + API + RDB)

### 🔧 각 도메인의 모듈 구조

```
qc-{domain}/
├── qc-{domain}-api/          # REST API & Service Layer  
├── qc-{domain}-core/         # 도메인 클래스 & 도메인 로직 & Repository 인터페이스
├── qc-{domain}-internal-api/ # 도메인 간 통신 구현체
├── qc-{domain}-rdb/          # JPA Repository 구현체
└── qc-{domain}-{infra}/      # 이외 Infra (Redis, Mongo, PG API 등)
```

## 🔌 Internal API Interface Layer

도메인 간 통신을 위한 인터페이스 정의 모듈:

```
qc-internal-api-interface/
├── qc-user-internal-api-interface
├── qc-question-internal-api-interface  
├── qc-creator-internal-api-interface
├── qc-point-internal-api-interface
└── qc-post-internal-api-interface
```

## 🛠️ Infrastructure Modules

```
qc-infra/
├── qc-event/
│   ├── qc-event-core/       # 이벤트 처리 구현체 (AWS SNS, AWS SQS)
│   └── qc-event-rdb/        # 이벤트 관련 Repository 구현체
├── qc-rdb/                  # 공통 JPA 설정
├── qc-redis/                # Redis 연동
├── qc-http/                 # HTTP Client
├── qc-mail/                 # 이메일 발송
├── qc-lock/                 # 분산 락
└── qc-external-pg/          # 외부 PG 연동
```

## 🔧 Shared Modules

```
├── qc-common/               # 공통 유틸리티, Response, Exception, Common Infra Interface,...
├── qc-logging/              # 로깅 설정  
└── qc-test-utils/           # 테스트 유틸리티
```