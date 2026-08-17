# ☕ GU Coffee (`gu-coffee`)

> **Spring Boot 기반 커피 주문 & 결제 시스템 멀티 모듈 백엔드 프로젝트**

---

## 📌 목차
- [1. 프로젝트 개요](#1-프로젝트-개요)
- [2. 멀티 모듈 아키텍처](#2-멀티-모듈-아키텍처)
- [3. 기술 스택](#3-기술-스택)
- [4. 주요 기능 및 도메인](#4-주요-기능-및-도메인)
- [5. API 엔드포인트 요약](#5-api-엔드포인트-요약)
- [6. 프로젝트 실행 및 빌드](#6-프로젝트-실행-및-빌드)

---

<a name="1-프로젝트-개요"></a>
## 1. 프로젝트 개요

`gu-coffee`는 커피 프랜차이즈 오더/결제 서비스를 모티브로 구축된 백엔드 시스템입니다.  
도메인별로 역할과 책임을 명확히 분리한 **멀티 모듈 아키텍처(Multi-Module Architecture)**를 채택하였으며, **Toss Payments PG 결제 승인 및 취소 연동**, **스탬프 적립 및 리워드 쿠폰 전환**, **장바구니/주문/결제 처리** 및 **매장 검색** 기능을 제공합니다.

---

<a name="2-멀티-모듈-아키텍처"></a>
## 2. 멀티 모듈 아키텍처

도메인 모듈을 가운데 두고 **`API (core-api) ──► Domain (core-domain) ◄── DB (db-core)`** 방향으로 의존성이 수렴하는 **의존성 역전 원칙(DIP) 기반 아키텍처**를 적용했습니다.

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

<a name="3-기술-스택"></a>
## 3. 기술 스택

### Core & Framework
- **Java 21**, **Spring Boot 4.0.5**
- **Spring Data JPA**, **QueryDSL 5.1.0**
- **Hypersistence TSID** (`io.hypersistence:hypersistence-tsid`) - 분산 PK 생성

### Database & Security
- **MySQL**, **H2 Database**
- Spring Security, JWT (Auth)

### Build & Documentation & Testing
- **Gradle 8.x** (Multi-Module)
- **Spring RestDocs** (Asciidoctor 4.0.2)
- **JUnit 5**, **Mockito**, AssertJ

---

<a name="4-주요-기능-및-도메인"></a>
## 4. 주요 기능 및 도메인

### 📋 1. 메뉴 (Menu)
- 카테고리별 메뉴 목록, 메뉴 상세(가격, 선택 옵션그룹) 조회
- 관리자(Admin) 전용 메뉴/옵션그룹/옵션 생성 및 매핑 관리

### 🛒 2. 장바구니 (Cart)
- 장바구니 생성, 메뉴/옵션 추가, 수량 변경 및 아이템 삭제

### 🛍️ 3. 주문 (Order)
- 단일/장바구니 기반 주문 생성 및 TSID 고유 결제 키 생성
- 주문 목록, 주문 상세 라인 아이템(OrderLine) 관리

### 💳 4. 결제 및 PG 연동 (Payment & PG)
- Toss Payments 결제 승인 및 검증
- PG 결제 할인(쿠폰 적용) 및 결제 상태/거래 내역 관리

### ❌ 5. 주문/결제 취소 (Cancel)
- 주문 취소 요청 처리 및 PG 결제 자동 취소 연동

### 🎟️ 6. 스탬프 & 쿠폰 (Stamp & Coupon)
- 음료 주문 시 스탬프 자동 적립
- 스탬프 10개 달성 시 리워드 쿠폰 자동 발급
- 결제 취소 시 스탬프 역추적 회수 및 쿠폰 취소 연쇄 처리

### 🏬 7. 매장 (Store)
- 위치 기반 또는 키워드 기반 매장 검색 및 상세 정보 조회

---

<a name="5-api-엔드포인트-요약"></a>
## 5. API 엔드포인트 요약

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

### 🛠️ 관리자 API (`/admin/v1`)
| 분류 | HTTP Method | Endpoint | 설명 |
| :--- | :--- | :--- | :--- |
| **Menu Admin** | `POST` | `/admin/v1/menu` | 메뉴 생성 |
| **Option Group** | `POST` | `/admin/v1/menu/option-group` | 옵션 그룹 생성 |
| **Option** | `POST` | `/admin/v1/menu/option` | 옵션 생성 |
| **Menu-Option** | `POST` | `/admin/v1/menu/menu-option-group` | 메뉴에 옵션 그룹 매핑 |

---

<a name="6-프로젝트-실행-및-빌드"></a>
## 6. 프로젝트 실행 및 빌드

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
