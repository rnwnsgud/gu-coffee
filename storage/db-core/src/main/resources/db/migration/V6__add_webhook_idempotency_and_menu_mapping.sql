-- V6: 외부 배달 플랫폼 웹훅 멱등성(webhook_idempotency) 및 외부 메뉴 코드 매핑(external_menu_mapping) 테이블 추가
CREATE TABLE IF NOT EXISTS external_menu_mapping (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform VARCHAR(30) NOT NULL,
    external_menu_code VARCHAR(100) NOT NULL,
    menu_id BIGINT NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_external_menu_mapping_platform_code UNIQUE (platform, external_menu_code),
    INDEX idx_external_menu_mapping_menu_id (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS webhook_idempotency (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    platform VARCHAR(30) NOT NULL,
    idempotency_key VARCHAR(100) NOT NULL,
    order_key VARCHAR(100) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_webhook_idempotency_platform_key UNIQUE (platform, idempotency_key),
    INDEX idx_webhook_idempotency_order_key (order_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
