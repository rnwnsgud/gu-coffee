# ☕ GU Coffee (`gu-coffee`)

> Kotlin 2.1 & Spring Boot 4.0 기반의 커피 주문·결제 멀티 모듈 시스템

---

## 📌 목차
- [1. 프로젝트 개요](#1-프로젝트-개요)
- [2. 멀티 모듈 아키텍처](#2-멀티-모듈-아키텍처)
- [3. 핵심 엔지니어링 & 시스템 신뢰성 설계](#3-핵심-엔지니어링--시스템-신뢰성-설계)
- [4. 기술 스택](#4-기술-스택)
- [5. 주요 도메인 기능](#5-주요-도메인-기능)
- [6. API 엔드포인트 요약](#6-api-엔드포인트-요약)
- [7. 빌드 및 테스트 실행](#7-빌드-및-테스트-실행)

---

<a name="1-프로젝트-개요"></a>
## 1. 프로젝트 개요

`gu-coffee`는 프랜차이즈 오더/결제 서비스를 기반으로 학습목적으로 만든 프로젝트입니다.
아키텍처를 점차 확장하면서 확장 가능한 설계가 어떤것인지 느껴보는 중입니다.

---

<a name="2-멀티-모듈-아키텍처"></a>
## 2. 멀티 모듈 아키텍처 (Modular Monolith)

단일 배포 단위(Single Deployment Unit)로 실행되지만 내부 도메인 간의 물리적/논리적 경계를 엄격히 분리한 **모듈러 모놀리스(Modular Monolith)** 아키텍처를 채택했습니다.
도메인 모듈을 중앙에 두고 **`API (core-api) ──► Domain (core-domain) ◄── DB (db-core)`** 방향으로 의존성이 수렴하는 의존성 역전 원칙(DIP)을 적용하여, 향후 특정 도메인을 독립 마이크로서비스(MSA)로 분리해낼 수 있는 구조적 확장성을 확보했습니다.

```text
gu-coffee
├── 🚀 coffee-server          # 메인 애플리케이션 실행 모듈 (Spring Boot Entry Point)
├── 🛠️ admin-api              # [일부구현] 관리자 전용 REST API (메뉴/옵션 등록 및 매핑)
│
├── 🧠 core                   # 비즈니스 핵심 모듈 그룹
│   ├── core-api              # 사용자 REST API & 오케스트레이션 서비스 (core-domain에만 의존)
│   ├── core-domain           # 순수 비즈니스 모델, 영속성 포트(인터페이스), 도메인 이벤트 (외부 의존성 x)
│   └── core-enum             # 공통 Enum 및 도메인 상숫값
│
├── 💾 storage                # 영속성 구현 모듈 그룹
│   └── db-core               # Spring Data JPA, QueryDSL 5.1, Entity, Repository 구현체
│
└── 🔌 support                # 공통 인프라 / 서포트 모듈 그룹
    ├── support-auth          # Principal 기반 인증/인가 객체
    ├── support-error         # 비즈니스 예외 계층 및 공통 ErrorType
    ├── support-event         # 트랜잭셔널 아웃박스 이벤트 디스패처 및 이벤트 로그
    ├── support-lock          # Redisson 기반 분산 락 및 로컬 락 추상화(LockManager) 인프라
    ├── support-logging       # [미구현] AOP 기반 분산 추적 로깅 유틸리티 예정
    ├── support-monitoring    # [미구현] 시스템 프로메테우스/메트릭 모니터링 예정
    ├── support-pagination    # 슬라이스/오프셋 페이징 유틸리티
    ├── support-pg            # Toss Payments PG 연동 및 WebClient 클라이언트
    └── support-web           # 공통 ApiResponse 포맷 및 Spring Web MVC 설정
```

---

<a name="3-핵심-엔지니어링--시스템-신뢰성-설계"></a>
## 3. 핵심 엔지니어링 & 시스템 신뢰성 설계

### 🛡️ 1. 결제 멱등성 보장 및 DB Connection Hold 방지
- **문제 인식**: 외부 PG 승인 통신(평균 RTT 200ms) 구간을 DB 트랜잭션으로 묶을 경우, 동시 요청 폭주 시 HikariCP 커넥션 풀이 급속히 고갈되어 시스템 전체가 마비되는 `Connection Pool Exhaustion` 위험
- **아키텍처 해결**:
  - `PaymentService.approvePayment` 메서드 전체를 **Non-Transactional**로 유지.
  - 비관적 락(`SELECT ... FOR UPDATE`)과 상태 전이(`READY -> PENDING_PG`)를 담당하는 `PaymentPreparer.prepare`로 트랜잭션 범위를 극소화.
  - 외부 PG 네트워크 I/O 호출 이전에 DB 트랜잭션을 커밋하고 커넥션을 즉시 풀에 반환함으로써 **커넥션 점유 시간을 200ms $\to$ 수 ms 단위로 단축**.

### ⚡ 2. 네트워크 타임아웃 대응 3중 안전망
외부 PG 승인 요청 중 Read Timeout 발생 시 결제건이 고아 상태로 남는 것을 방지하기 위해 3단계 즉각 복구 파이프라인 구축:
1. **1차 즉각 동기 조회**: 예외 포착 즉시 `paymentGatewayProcessor.getPGPayment`를 호출하여 PG사의 실제 승인 완료 여부를 실시간 확인 $\to$ 승인 완료 시 정상 완료(`complete`) 처리.
2. **2차 자동 망취소**: 미승인 상태이거나 응답 불능 시 즉시 자동 망취소(`cancelPayment`)를 호출하여 결제 승인 취소.
3. **3차 원자적 Outbox 적재**: 망취소마저 실패하거나 프로세스 비정상 종료 시, 트랜잭셔널 아웃박스 테이블에 `CancelEvent`를 원자적으로 영속화하여 스케줄러 기반 비동기 보상 트랜잭션 수행.

### 🚀 3. `FOR UPDATE SKIP LOCKED` 기반 분산 스케일아웃 복구 파이프라인
- 실측 레이턴시 기반 단일 스레드 용량:
  - 외부 PG 단건 조회 네트워크 RTT(200ms)를 포함할 때 결제 복구 1건당 평균 211.4ms 소요됨을 통합 테스트로 실측.
  - 단일 스레드가 1분 내내 100% 가동될 때의 물리적 처리 상한선은 최대 283건($\frac{60\text{초}}{211.4\text{ms}}$).
- 평상시 최소 침해 청크 설정 (`LIMIT = 20`):
  - 결제 복구 배치가 일반 고객의 주문/결제와 동일한 DB 커넥션 풀을 공유하므로, 커넥션 독점을 방지하기 위해 1회 처리량을 20건(약 4.2초 소요, 1분 주기의 약 7%만 점유)으로 제한하여 사용자 트래픽 영향도를 최소화.
- 피크 타임 장애 시나리오(72건 백로그)와 `SKIP LOCKED` 선형 확장:
  - 72건 백로그 도출 근거: 전국 3,000개 매장 규모의 점심 피크 순간 최대 스파이크(120 RPS) 발생 시, 1분 동안 유입되는 7,200건 중 공용 모바일망 패킷 유실/타임아웃(1%)으로 인해 1분 주기당 정확히 72건($120 \times 60 \times 0.01$)의 고립 결제(`PENDING_PG`)가 적체되는 실무 장애 상황을 모델링.
  - 기존 방식의 한계 (ShedLock): 단일 인스턴스만 독점 실행되는 ShedLock 방식은 서버가 많아도 1대만 20건씩 순차 처리하므로 72건 해소에 총 4분(4주기)이 소요되어 점심 피크 고객 대기 시간 증가.
  - 아키텍처 해결 (`SKIP LOCKED`): 행 단위 비차단 락인 `FOR UPDATE SKIP LOCKED`를 적용하여, 서버 4대 증설 시 락 경합 없이 각 서버가 20건씩 총 80건을 단 4.2초 만에 병렬로 즉시 해소하여 72건 백로그를 1회 주기 내에 전량 복구하도록 설계.

### 📦 4. 트랜잭셔널 아웃박스 패턴 (Transactional Outbox Pattern)
- 메시지 발행과 도메인 변경의 원자성을 보장하기 위해 별도 이벤트 저장소(`event_log`)를 도메인 로컬 트랜잭션 내에서 커밋.
- 발행 실패 이벤트는 스케줄러가 주기적으로 재시도하며, 이벤트 ID 기반의 멱등 테이블을 통해 중복 소비 방지.

### 🧩 5. 도메인 간 트랜잭션 디커플링 및 장애 격리 (Fault Isolation)
- 문제 인식: 결제 승인 완료 트랜잭션(`PaymentCompleter.complete`) 내부에 부가 기능인 스탬프 적립이 동기로 묶여 있을 경우, 스탬프 테이블 락/데드락 등 부가 기능의 일시적 장애로 인해 이미 카드사 승인이 끝난 결제 전체가 롤백되어 망취소/환불되는 치명적 결합 위험 감지.
- 아키텍처 해결:
  - 결제 코어와 마케팅 리워드의 트랜잭션 라이프사이클을 완전 분리.
  - `PaymentCompleter`에서 스탬프 동기 의존성을 제거하고, 결제 DB 커밋 완료 후 Spring 트랜잭션 이벤트(`@TransactionalEventListener(phase = AFTER_COMMIT)`) 및 비동기 워커 풀(`stampAsyncExecutor`)을 통해 스탬프를 적립하도록 파이프라인 구축.
  - 스탬프 모듈에 장애가 발생하더라도 결제 성공을 100% 보장하는 장애 격리(Fault Isolation)를 달성하고, 모놀리스 내에서 도메인 간 최종적 일관성(Eventual Consistency)을 확보.

### ☕ 6. Read-Heavy 마스터 데이터 Redis Cache-Aside 및 스탬피드 방어
- 문제 인식: 메뉴 상세 조회 시 1회 요청당 4단 순차 DB 쿼리(Menu $\to$ MenuOptionGroup $\to$ OptionGroup $\to$ Option)가 발생하여 동시 접속 시 DB 커넥션 풀을 과도하게 점유하는 문제 감지.
- 아키텍처 해결:
  - 1:N:M 다중 조인으로 인한 대량 데이터 중복 전송과 실행 계획 복잡도를 배제하고, DB는 단순 PK/IN 기반 조회를 유지하도록 역할을 분리.
  - 조립된 메뉴 상세 데이터는 Redis 기반 Cache-Aside 패턴을 구축하여 95% 이상의 조회 트래픽을 메모리에서 즉시 서빙(RTT 1~2ms)하도록 격리.
  - 대규모 트래픽 환경에서 캐시 만료 시 동시 다발적 DB 조회가 몰리는 캐시 스탬피드(Cache Stampede)를 방어하기 위해 `@Cacheable(sync = true)` 동기화 락을 적용하여 동일 키에 대한 DB 접근을 직렬화.
  - 관리자 메뉴-옵션 매핑 수정 시 `@CacheEvict`를 통해 캐시를 즉시 무효화하여 데이터 일관성 보장.

### 🎟️ 7. Redisson 기반 분산 락을 통한 쿠폰 다운로드 동시성 제어
- 문제 인식:
  - 동일 사용자가 모바일 네트워크 지연 등으로 다운로드 버튼을 연타(따닥)하거나 동시 요청을 보낼 때, 애플리케이션 레벨의 중복 검사(`existsByPrincipalKeyAndCouponId`)가 동시에 통과되어 동일 쿠폰이 중복 발급되는 동시성 이슈 발생.
- 아키텍처 해결:
  - Redisson 의존성을 독립 인프라 모듈(`support:lock`)로 격리하여 비즈니스 코어와 서드파티 락 라이브러리 간의 결합도를 낮추고 `LockManager` 인터페이스로 추상화.
  - Lettuce의 스핀 락(Spin Lock) 폴링 방식 대신 Redis Pub/Sub 기반의 `RedissonClient`를 채택하여 불필요한 네트워크 트래픽 및 Redis CPU 부하를 최소화.
  - 트랜잭션 커밋 전에 락이 조기 해제되는 경합을 방지하기 위해, 트랜잭션 외부에서 락을 획득/해제하고 내부에서 DB 커밋까지 마치는 수명주기(`CouponService` $\to$ `CouponDownloadExecutor`)를 확립.
  - `COUPON-DOWNLOAD-{couponId}-{principalKey}` 단위로 락을 세분화하여 타 유저의 발급 요청에는 영향을 주지 않도록 격리.
  - 분산 락 실패 시나리오 및 인프라 순단에 대비하여 DB `issued_coupon` 테이블에 `(principal_key, coupon_id)` 복합 유니크 제약조건을 병행 적용해 데이터 정합성을 이중 보장.

---

<a name="4-기술-스택"></a>
## 4. 기술 스택

### Language & Framework
- **Kotlin 2.1.0** (admin-api 모듈 제외)
- **Spring Boot 4.0.5**
- **Spring Data JPA**, **QueryDSL 5.1.0 (KAPT)**
- **Spring Data Redis 4.0.4**, **Redisson 3.45.0**
- **Hypersistence TSID 2.1.4** (분산 분할 시간순 정렬 고유 식별자 PK)


### Testing & Build
- **Gradle 8.x** (Kotlin DSL Multi-Module)
- **JUnit 5**, **Mockito-Kotlin 5.4.0**, **AssertJ**
- **Spring RestDocs** (Asciidoctor 4.0.2)

---

<a name="5-주요-도메인-기능"></a>
## 5. 주요 도메인 기능

### 📋 1. 메뉴 (Menu)
- 카테고리별 메뉴 목록, 상세 정보(영양성분, 알레르기 유발물질) 조회
- 관리자(Admin) 전용 메뉴/옵션그룹/옵션 등록 및 복합 매핑

### 🛒 2. 장바구니 (Cart)
- 사용자/게스트별 장바구니 생성, 옵션별 메뉴 담기, 수량 변경 및 삭제

### 🛍️ 3. 주문 (Order)
- 단일 메뉴 즉시 주문 및 장바구니 기반 주문서 생성 (TSID 고유 식별자 발급)
- 주문 라인별(OrderLine) 스탬프 적립 대상 여부 및 금액 계산

### 💳 4. 결제 및 PG 연동 (Payment & PG)
- Toss Payments 결제 승인, 쿠폰 할인 금액 검증 및 멱등 결제 처리
- 결제 거래 내역(`transaction_history`) 기록 및 상태 추적

### ❌ 5. 주문 및 결제 취소 (Cancel)
- 고객/관리자 주문 취소 요청 시 PG사 자동 결제 취소 연동
- 보상 트랜잭션 기반 쿠폰/스탬프 롤백

### 🎟️ 6. 스탬프 & 리워드 쿠폰 (Stamp & Coupon)
- 음료 구매 시 결제 완료 이벤트 기반 스탬프 자동 적립
- 10개 적립 시 무료 음료 쿠폰 자동 발급 및 결제 취소 시 스탬프 역순 회수

### 🏬 7. 매장 (Store)
- Haversine 공식 기반 사용자 위치 반경 매장 검색 및 지점 영업 정보 조회

---

<a name="6-api-엔드포인트-요약"></a>
## 6. API 엔드포인트 요약

### 👤 사용자 API (`/api/v1`)
| 도메인 | HTTP Method | Endpoint | 설명 |
| :--- | :--- | :--- | :--- |
| **Health** | `GET` | `/health` | 서버 헬스 체크 |
| **Menu** | `GET` | `/api/v1/menus` | 카테고리별 메뉴 목록 및 상세 조회 |
| **Cart** | `POST` / `GET` / `DELETE` | `/api/v1/carts` | 장바구니 생성, 아이템 추가/수정/삭제 |
| **Order** | `POST` / `GET` | `/api/v1/orders` | 주문서 생성 및 주문 내역 조회 |
| **Payment** | `POST` | `/api/v1/payments` | 결제 승인 요청 (PG 결제 연동) |
| **Cancel** | `POST` | `/api/v1/cancels` | 주문 및 결제 취소 요청 |
| **Coupon** | `GET` / `POST` | `/api/v1/coupons` | 보유 쿠폰 목록 조회 및 다운로드 |
| **Stamp** | `GET` | `/api/v1/stamps` | 사용자 스탬프 적립 현황 및 히스토리 조회 |
| **Store** | `GET` | `/api/v1/stores` | 위치 반경 및 키워드 기반 매장 검색 |

### 🛠️ 관리자 API (`/admin/v1`)
| 도메인 | HTTP Method | Endpoint | 설명 |
| :--- | :--- | :--- | :--- |
| **Menu** | `POST` | `/admin/v1/menu` | 신규 메뉴 등록 |
| **Option Group** | `POST` | `/admin/v1/menu/option-group` | 옵션 그룹(온도, 사이즈 등) 등록 |
| **Option** | `POST` | `/admin/v1/menu/option` | 세부 옵션(HOT/ICE, 샷 추가 등) 등록 |
| **Mapping** | `POST` | `/admin/v1/menu/menu-option-group` | 메뉴에 옵션 그룹 매핑 |

---

<a name="7-빌드-및-테스트-실행"></a>
## 7. 빌드 및 테스트 실행

### 테스트 스위트 실행
```bash
# 전체 단위 및 통합 테스트 실행
./gradlew test

# 핵심 결제/동시성 테스트만 실행
./gradlew :core:core-api:test --tests "com.coffee.gu.payment.*"
```

### 애플리케이션 실행
```bash
# 메인 서버 실행
./gradlew :coffee-server:bootRun
```
