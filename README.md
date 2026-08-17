# ☕ GU Coffee (`gu-coffee`)

> **결제 데이터 정합성, 동시성 제어 및 의존성 역전 아키텍처(DIP)를 지향하는 Spring Boot 기반 멀티 모듈 결제·주문 시스템**

---

## 📌 목차
- [1. 프로젝트 개요](#1-프로젝트-개요)
- [2. 핵심 엔지니어링 문제 해결](#2-핵심-엔지니어링-문제-해결)
- [3. 멀티 모듈 아키텍처](#3-멀티-모듈-아키텍처)
- [4. 기술 스택](#4-기술-스택)
- [5. 주요 기능 및 도메인](#5-주요-기능-및-도메인)
- [6. API 엔드포인트 요약](#6-api-엔드포인트-요약)
- [7. 프로젝트 실행 및 검증](#7-프로젝트-실행-및-검증)

---

<a name="1-프로젝트-개요"></a>
## 1. 프로젝트 개요

`gu-coffee`는 커피 오더/결제 서비스의 대용량 트래픽 환경(일 결제 120만 건, 피크 120 RPS)을 가상 비즈니스 규모로 설정하고, **금융/결제 도메인의 동시성 제어, 최종 정합성 보장, 망취소 및 멱등성** 문제를 해결한 백엔드 아키텍처 프로젝트입니다.

---

<a name="2-핵심-엔지니어링-문제-해결"></a>
## 2. 핵심 엔지니어링 문제 해결

### 🔒 1. 결제 위·변조 방지 및 Fast-Fail Locking
* **보안 검증**: 클라이언트의 금액 변조 위험을 차단하기 위해 고유 `orderKey`만 전달받고, 백엔드가 PG사 REST API를 직접 호출해 결제 데이터 대조 검증.
* **Fast-Fail 비관적 락**: PG 승인 전 `Payment` 레코드의 `READY` 상태에 비관적 락(`PESSIMISTIC_WRITE`)을 점유하여 중복 연타 및 PG 승인 웹훅의 동시 도달 시 불필요한 PG 외부 API 호출을 1ms 내 즉시 차단.

### 🔄 2. 트랜잭션 보상 (망취소) 및 트랜잭션 아웃박스 패턴
* **PG 망취소 선택**: 결제 후처리 실패 롤백 시 데이터 정합성을 위해 재시도가 아닌 PG 승인 취소(망취소)를 수행하여 사용자 응답의 명확성 확보.
* **원자적 아웃박스(Outbox) 기록**: `CancelEvent` 발행 및 `EventLog` 저장을 동일 DB 트랜잭션 내 원자적으로 처리하기 위해 `@Transactional(propagation = Propagation.MANDATORY)` 적용.

### ⚡ 3. 고립 결제 복구 스케줄러 성능 최적화 (ShedLock ➔ SKIP LOCKED)
* **병목 발견**: 단일 워커(ShedLock) 방식 사용 시 피크 고립 결제 건수(72건) 처리 시 1건당 평균 218ms, 총 15.7초간 DB 커넥션을 장기 점유하는 병목 확인.
* **최적화**: `SKIP LOCKED` 기반 소단위 청크(Limit 20) 멀티 워커 분산 구조로 전환하여 DB 커넥션 락 점유 시간을 **4.36초로 72.2% 단축**.
* **유예 마진 확보**: `updatedAt < NOW - 5분` 조항을 적용하여 유저의 정상 요청이 스케줄러와 경합하지 않도록 유예 시간 확보.

### 🛡️ 4. 스탬프·쿠폰 비동기 발급 멱등성 및 DEAD 레터 관리
* **멱등성(Idempotency) 보장**: `@Async` 이벤트 환경에서 동시 요청 시 중복 쿠폰 발급을 방지하고자 `EventLog` 기반 `saveIfNotExists` (`INSERT IGNORE`) 메커니즘 구축.
* **관심사 분리**: `StampEventListener` (비동기 디스패치 전담)와 `StampRewardManager` (`@Transactional` 도메인 전담)로 클래스를 분리하여 AOP 프록시 경계 정립.
* **DEAD 상태 이관**: 비동기 실패 이벤트 5회 재시도 실패 시 `DEAD` 상태 전환 및 알림 처리 라우팅 구축.

---

<a name="3-멀티-모듈-아키텍처"></a>
## 3. 멀티 모듈 아키텍처

도메인 모듈을 가운데 두고 **`API (core-api) ──► Domain (core-domain) ◄── DB (db-core)`** 방향으로 의존성이 집중되는 **의존성 역전 원칙(DIP) 기반 Hexagonal / Clean Architecture**를 구현했습니다.

```text
gu-coffee
├── 🚀 coffee-server          # 메인 애플리케이션 실행 모듈 (Spring Boot Entry Point)
├── 🛠️ admin-api              # 관리자 전용 REST API (메뉴/옵션 등록 및 관리)
│
├── 🧠 core                   # 비즈니스 핵심 모듈 그룹
│   ├── core-api              # 사용자 REST API & 서비스 컨트롤러 (core-domain에만 의존)
│   ├── core-domain           # 순수 비즈니스 도메인 모델, 서비스 인터페이스 & Event (외부 의존성 0)
│   └── core-enum             # 공통 Enum 및 도메인 상숫값
│
├── 💾 storage                # 영속성 모듈 그룹
│   └── db-core               # JPA Entity, QueryDSL, Repository 구현체 (core-domain 인터페이스 구현)
│
└── 🔌 support                # 공통 인프라 / 서포트 모듈 그룹
    ├── support-auth          # 인증 및 인가 처리
    ├── support-error         # 예외 처리 및 공통 ErrorType
    ├── support-event         # 이벤트 디스패처
    ├── support-logging       # 로깅 유틸리티
    ├── support-monitoring    # 시스템 프로메테우스/모니터링
    ├── support-pagination    # 페이징 유틸리티
    ├── support-pg            # PG 연동 라우터 및 Toss Payments 구현체
    └── support-web           # 공통 ApiResponse 포맷 및 Web MVC 설정
```

---

<a name="4-기술-스택"></a>
## 4. 기술 스택

### Backend Core & Framework
- **Java 21**, **Spring Boot 4.0.5**
- **Spring Data JPA**, **QueryDSL 5.1.0**
- **Hypersistence TSID** (`io.hypersistence:hypersistence-tsid`) - 분산 PK 생성

### Concurrency & Locking
- JPA Pessimistic Lock (`PESSIMISTIC_WRITE`)
- Spring Scheduling, MySQL `SKIP LOCKED` Batch Query
- Event Outbox (`INSERT IGNORE` Idempotency Table)

### Build & Documentation & Testing
- **Gradle 8.x** (Multi-Module)
- **Spring RestDocs** (Asciidoctor 4.0.2)
- **JUnit 5**, **Mockito**, AssertJ

---

<a name="5-주요-기능-및-도메인"></a>
## 5. 주요 기능 및 도메인

### 💳 결제 (Payment & PG)
- Toss Payments REST API 직접 조회 기반 결제 금액 대조 검증
- Fast-Fail Locking 기반 중복 결제 승인 요청 사전 차단
- 망취소(PG Cancel) 및 아웃박스 정합성 보장 스케줄러

### 🎟️ 스탬프 & 쿠폰 (Stamp & Coupon)
- 음료 주문 시 스탬프 자동 적립
- 스탬프 10개 달성 시 비동기 멱등 리워드 쿠폰 자동 발급
- 결제 취소 시 `USED` 상태 스탬프 역추적 회수 및 연쇄 취소 정책

### 🛒 주문 & 장바구니 & 메뉴 (Order, Cart, Menu)
- 단일/장바구니 기반 주문 생성 및 TSID 고유 결제 키 생성
- 카테고리별 메뉴, 옵션그룹, 옵션 상하 구조 관리 (Admin API)

---

<a name="6-api-엔드포인트-요약"></a>
## 6. API 엔드포인트 요약

### 👤 사용자 API (`/api/v1`)
| 분류 | HTTP Method | Endpoint | 설명 |
| :--- | :--- | :--- | :--- |
| **System** | `GET` | `/health` | 헬스 체크 |
| **Menu** | `GET` | `/api/v1/menus` | 메뉴 및 카테고리 조회 |
| **Cart** | `POST` | `/api/v1/carts` | 장바구니 생성 및 관리 |
| **Order** | `POST` | `/api/v1/orders` | 주문 생성 |
| **Payment** | `POST` | `/api/v1/payments` | 결제 승인 요청 |
| **Cancel** | `POST` | `/api/v1/cancels` | 주문 및 결제 취소 요청 |
| **Coupon** | `GET` / `POST` | `/api/v1/coupons` | 쿠폰 조회 및 발급 |
| **Stamp** | `GET` | `/api/v1/stamps` | 스탬프 조회 |
| **Store** | `GET` | `/api/v1/stores` | 매장 위치/키워드 검색 |

---

<a name="7-프로젝트-실행-및-검증"></a>
## 7. 프로젝트 실행 및 검증

### 빌드 및 테스트 실행
```bash
# 전체 단위 / 통합 테스트 실행
./gradlew test

# 프로젝트 빌드
./gradlew clean build
```

### 애플리케이션 실행
```bash
./gradlew :coffee-server:bootRun
```
