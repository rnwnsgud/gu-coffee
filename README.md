# ☕ GU Coffee (`gu-coffee`)

> 커피 주문, 장바구니, 결제(PG 연동), 쿠폰, 스탬프 적립 및 매장 검색 기능을 제공하는 **Spring Boot 기반 멀티 모듈 백엔드 시스템**입니다.

---

## 📌 목차
- [1. 프로젝트 개요](#-프로젝트-개요)
- [2. 기술 스택 (Tech Stack)](#-기술-스택-tech-stack)
- [3. 멀티 모듈 아키텍처 (Architecture)](#-멀티-모듈-아키텍처-architecture)
- [4. 주요 기능 및 도메인 (Key Features)](#-주요-기능-및-도메인-key-features)
- [5. API 엔드포인트 요약](#-api-엔드포인트-요약)
- [6. 프로젝트 실행 및 빌드 (Getting Started)](#-프로젝트-실행-및-빌드-getting-started)

---

## 1. 프로젝트 개요
`gu-coffee`는 커피 프랜차이즈/매장 서비스를 모티브로 한 주문 및 결제 백엔드 시스템입니다.  
도메인별로 명확히 분리된 **멀티 모듈 구조(Multi-Module Architecture)**를 채택하여 높은 재사용성과 유지보수성을 보장하며, **Toss Payments PG 연동**, **스탬프 적립 및 쿠폰 전환**, **장바구니/주문/결제 플로우** 등의 도메인 로직을 구획화하여 제공합니다.

---

## 2. 기술 스택 (Tech Stack)

### Core & Framework
- **Language**: Java 21
- **Framework**: Spring Boot 4.0.5
- **ORM / Persistence**: Spring Data JPA, Hibernate
- **ID Generation**: Hypersistence TSID (`io.hypersistence:hypersistence-tsid`)

### Build & Tooling
- **Build System**: Gradle (Multi-Module)
- **API Docs**: Spring RestDocs (Asciidoctor 4.0.2)
- **Testing**: JUnit 5, Spring Boot Starter Test

### External Integration
- **PG Integration**: Toss Payments (PG Gateway Router 구현)

---

## 3. 멀티 모듈 아키텍처 (Architecture)

프로젝트는 역할과 책임에 따라 계층화된 멀티 모듈로 구성되어 있습니다.

```text
gu-coffee
├── 🚀 coffee-server          # 애플리케이션 메인 실행 모듈 (Spring Boot Main Application)
├── 🛠️ admin-api              # 관리자 전용 REST API (메뉴/옵션 등록 및 관리)
│
├── 🧠 core                   # 비즈니스 핵심 모듈 그룹
│   ├── core-api              # 사용자 전용 REST API 컨트롤러
│   ├── core-domain           # 핵심 비즈니스 도메인 모델, 서비스 및 이벤트
│   └── core-enum             # 공통 Enum 및 상숫값 정의
│
├── 💾 storage                # 영속성 모듈 그룹
│   └── db-core               # JPA Entity, Repository 및 DB 영속성 레이어
│
└── 🔌 support                # 공통 인프라 / 지원 모듈 그룹
    ├── support-auth          # 인증 및 인가 처리
    ├── support-error         # 예외 처리 및 에러 타입/코드 정의
    ├── support-event         # 이벤트 발행/수신 레이어
    ├── support-logging       # 로깅 AOP 및 유틸리티
    ├── support-monitoring    # 시스템 모니터링
    ├── support-pagination    # 페이징 및 정렬 지원
    ├── support-pg            # PG 결제 연동 추상화 및 Toss Payments 구현체
    └── support-web           # API 공통 응답 포맷(ApiResponse) 및 Web MVC 설정
```

---

## 4. 주요 기능 및 도메인 (Key Features)

### 📋 1. 메뉴 (Menu)
- 카테고리별 메뉴 목록 조회 및 메뉴 상세 정보 (가격, 영양성분, 선택 옵션그룹) 조회
- 관리자(Admin) 전용 메뉴/옵션그룹/옵션 생성 및 연결 관리

### 🛒 2. 장바구니 (Cart)
- 장바구니 생성, 메뉴/옵션 추가, 수량 수정 및 아이템 삭제

### 🛍️ 3. 주문 (Order)
- 바로 주문(Direct Order) 및 장바구니 기반 주문 생성
- 주문 목록, 주문 상세(Line Items) 및 주문 요약 정보 제공

### 💳 4. 결제 및 PG 연동 (Payment & PG)
- Toss Payments 결제 승인 및 결제 상태 관리
- `PaymentGatewayRouter` 패턴을 통한 확장 가능한 PG 연동 구조
- 결제 할인(쿠폰 적용) 및 결제 이력 관리

### ❌ 5. 주문/결제 취소 (Cancel)
- 주문 취소 요청 처리 및 PG 결제 자동 취소 연동

### 🎟️ 6. 쿠폰 (Coupon)
- 사용자 쿠폰 조회, 특정 조건 쿠폰 발급 및 사용 관리

### 🏷️ 7. 스탬프 (Stamp)
- 음료 주문에 따른 스탬프 적립 및 만료 예정 스탬프 조회
- 일정 스탬프 달성 시 쿠폰 교환 기능

### 🏬 8. 매장 (Store)
- 위치 기반 또는 키워드 기반 매장 검색 및 매장 상세 조회

---

## 5. API 엔드포인트 요약

### 👤 사용자 API (`/api/v1`)
| 분류 | HTTP Method | Endpoint | 설명 |
| :--- | :--- | :--- | :--- |
| **System** | `GET` | `/health` | 헬스 체크 |
| **Menu** | `GET` | `/api/v1/menus` | 메뉴 및 카테고리 조회 |
| **Cart** | `POST` | `/api/v1/carts` | 장바구니 생성 및 아이템 관리 |
| **Order** | `POST` | `/api/v1/orders` | 주문 생성 (직접/장바구니) |
| **Payment** | `POST` | `/api/v1/payments` | 결제 승인 요청 |
| **Cancel** | `POST` | `/api/v1/cancels` | 주문 및 결제 취소 요청 |
| **Coupon** | `GET` / `POST` | `/api/v1/coupons` | 쿠폰 목록/발급/사용 |
| **Stamp** | `GET` / `POST` | `/api/v1/stamps` | 스탬프 조회 및 쿠폰 교환 |
| **Store** | `GET` | `/api/v1/stores` | 매장 검색 및 정보 조회 |

### 🛠️ 관리자 API (`/admin/v1`)
| 분류 | HTTP Method | Endpoint | 설명 |
| :--- | :--- | :--- | :--- |
| **Menu Admin** | `POST` | `/admin/v1/menu` | 메뉴 생성 |
| **Option Group** | `POST` | `/admin/v1/menu/option-group` | 옵션 그룹 생성 |
| **Option** | `POST` | `/admin/v1/menu/option` | 옵션 생성 |
| **Menu-Option** | `POST` | `/admin/v1/menu/menu-option-group` | 메뉴에 옵션 그룹 매핑 |

---

## 6. 프로젝트 실행 및 빌드 (Getting Started)

### 사전 요구 사항
- Java 21 이상
- Gradle 8.x 이상 (Gradle Wrapper 포함)

### 빌드 (Build)
```bash
./gradlew clean build
```

### 애플리케이션 실행 (Run)
```bash
./gradlew :coffee-server:bootRun
```

---
