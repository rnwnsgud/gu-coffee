-- V3: 선착순 한정 수량 메뉴 (Limited Menu) 테이블 추가
CREATE TABLE IF NOT EXISTS limited_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    total_quantity INT NOT NULL,
    sold_quantity INT NOT NULL DEFAULT 0,
    max_purchase_per_user INT NOT NULL DEFAULT 1,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_limited_menu_menu_id UNIQUE (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
