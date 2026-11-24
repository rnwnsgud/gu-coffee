-- V7: 매장 고정 기기(키오스크 등) 인증 정보 테이블 추가
CREATE TABLE IF NOT EXISTS store_device (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_code VARCHAR(100) NOT NULL,
    store_id BIGINT NOT NULL,
    device_type VARCHAR(30) NOT NULL,
    api_key_hash VARCHAR(64) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_store_device_device_code UNIQUE (device_code),
    CONSTRAINT uk_store_device_api_key_hash UNIQUE (api_key_hash),
    INDEX idx_store_device_store_id (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
