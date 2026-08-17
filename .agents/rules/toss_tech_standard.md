# Toss & High-Traffic Fintech Engineering Standards

## 1. 아키텍처 및 설계 원칙
- 항상 토스/토스페이먼츠 수준의 **대용량 트래픽 및 이벤트 피크 타임(Black Friday 등)**을 가상 환경으로 전제합니다.
- 단일 서버 전담 구조(Single Point of Failure / Bottleneck) 대신 **수평 확장(Scale-Out) 가능한 동시성 제어 모델(예: DB SKIP LOCKED, 메시지 큐, 분산 워커)**을 우선 고려합니다.
- 외부 PG 연동 시 **네트워크 타임아웃, 멱등성(Idempotency), 보상 트랜잭션(Saga / Outbox / Status Check)** 및 최종 정합성을 철저히 설계합니다.

## 2. 답변 및 스토리텔링 가이드
- 단순히 기능을 구현하는 방법만 제시하지 않고, **비즈니스 SLA, 최악의 장애(Worst-case) 계산, 트레이드오프(Trade-off)** 관점에서 근거를 명확하게 제시합니다.
- 면접관(특히 토스/핀테크 기술 면접관)이 높게 평가할 수 있도록 **논리적 수치 산정 기준 및 포트폴리오 면접 답변 스크립트**를 항상 함께 제공합니다.
