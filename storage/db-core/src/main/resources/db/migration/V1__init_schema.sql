-- =======================================================================
-- V1__init_schema.sql
-- Description: gu-coffee Initial Database Schema
-- Database: MySQL 8.0+
-- =======================================================================

-- 1. 카테고리 (Category)
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 메뉴 (Menu)
CREATE TABLE IF NOT EXISTS menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    cost_price DECIMAL(19, 2) NOT NULL,
    sales_price DECIMAL(19, 2) NOT NULL,
    image_url VARCHAR(500) NULL,
    description TEXT NULL,
    capacity DOUBLE NULL,
    caffeine DOUBLE NULL,
    calories DOUBLE NULL,
    sodium DOUBLE NULL,
    carbohydrate DOUBLE NULL,
    sugar DOUBLE NULL,
    fat DOUBLE NULL,
    saturated_fat DOUBLE NULL,
    protein DOUBLE NULL,
    contained_allergens VARCHAR(255) NULL,
    may_contain_allergens VARCHAR(255) NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. 메뉴-카테고리 매핑 (Menu Category Mapping)
CREATE TABLE IF NOT EXISTS menu_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_menu_category_category (category_id),
    INDEX idx_menu_category_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 옵션 그룹 (Option Group)
CREATE TABLE IF NOT EXISTS option_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    is_exclusive BOOLEAN NOT NULL,
    is_required BOOLEAN NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 옵션 (Option)
CREATE TABLE IF NOT EXISTS `option` (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    option_group_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    extra_price DECIMAL(19, 2) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_option_group (option_group_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 메뉴-옵션그룹 매핑 (Menu Option Group Mapping)
CREATE TABLE IF NOT EXISTS menu_option_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    option_group_id BIGINT NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_menu_option_group_menu (menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 매장 (Store)
CREATE TABLE IF NOT EXISTS store (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    branch_code VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    phone_number VARCHAR(50) NOT NULL,
    representative VARCHAR(100) NOT NULL,
    trade_name VARCHAR(100) NOT NULL,
    business_registration_number VARCHAR(50) NOT NULL,
    business_address VARCHAR(255) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 매장 영업 시간 (Sales Hour)
CREATE TABLE IF NOT EXISTS sales_hour (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    `day` VARCHAR(20) NOT NULL,
    open TIME NOT NULL,
    close TIME NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_sales_hour_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. 장바구니 (Cart Item)
CREATE TABLE IF NOT EXISTS cart_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    menu_id BIGINT NOT NULL,
    quantity BIGINT NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_cart_item_principal (principal_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. 주문 (Order)
CREATE TABLE IF NOT EXISTS `order` (
    order_key VARCHAR(100) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    store_id BIGINT NOT NULL,
    total_price DECIMAL(19, 2) NOT NULL,
    state VARCHAR(30) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_order_principal (principal_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. 주문 항목 (Order Line)
CREATE TABLE IF NOT EXISTS order_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_key VARCHAR(100) NOT NULL,
    menu_id BIGINT NOT NULL,
    menu_name VARCHAR(100) NOT NULL,
    image_url VARCHAR(500) NULL,
    description TEXT NULL,
    quantity BIGINT NOT NULL,
    unit_price DECIMAL(19, 2) NOT NULL,
    total_price DECIMAL(19, 2) NOT NULL,
    is_stamp_eligible BOOLEAN NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_order_line_order_key (order_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. 결제 (Payment)
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    order_key VARCHAR(100) NOT NULL,
    original_amount DECIMAL(19, 2) NOT NULL,
    issued_coupon_id BIGINT NULL,
    coupon_discount DECIMAL(19, 2) NULL,
    paid_amount DECIMAL(19, 2) NOT NULL,
    state VARCHAR(30) NOT NULL,
    external_payment_key VARCHAR(100) NULL,
    method VARCHAR(30) NULL,
    paid_at DATETIME(6) NULL,
    approve_code VARCHAR(100) NULL,
    retry_count INT NOT NULL DEFAULT 0,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY udx_order_key (order_key),
    INDEX idx_payment_recovery (state, updated_at, retry_count)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. 결제 취소 (Cancel)
CREATE TABLE IF NOT EXISTS cancel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    order_key VARCHAR(100) NOT NULL,
    payment_id BIGINT NOT NULL,
    original_amount DECIMAL(19, 2) NOT NULL,
    issued_coupon_id BIGINT NULL,
    coupon_discount DECIMAL(19, 2) NULL,
    paid_amount DECIMAL(19, 2) NOT NULL,
    canceled_amount DECIMAL(19, 2) NOT NULL,
    external_cancel_key VARCHAR(100) NULL,
    canceled_at DATETIME(6) NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_cancel_payment (payment_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. 거래 이력 (Transaction History)
CREATE TABLE IF NOT EXISTS transaction_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    order_key VARCHAR(100) NOT NULL,
    payment_id BIGINT NOT NULL,
    external_transaction_key VARCHAR(100) NULL,
    amount DECIMAL(19, 2) NOT NULL,
    message VARCHAR(255) NULL,
    occurred_at DATETIME(6) NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_transaction_history_payment (payment_id),
    INDEX idx_transaction_history_order (order_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 15. 쿠폰 마스터 (Coupon)
CREATE TABLE IF NOT EXISTS coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(30) NOT NULL,
    discount DECIMAL(19, 2) NOT NULL,
    expired_at DATETIME(6) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. 쿠폰 타겟 매핑 (Coupon Target)
CREATE TABLE IF NOT EXISTS coupon_target (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_coupon_target_coupon (coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. 발급 쿠폰 (Issued Coupon)
CREATE TABLE IF NOT EXISTS issued_coupon (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    coupon_id BIGINT NOT NULL,
    state VARCHAR(30) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_issued_coupon_principal_coupon UNIQUE (principal_key, coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. 스탬프 (Stamp)
CREATE TABLE IF NOT EXISTS stamp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_key VARCHAR(100) NOT NULL,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    state VARCHAR(30) NOT NULL,
    expired_at DATETIME(6) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_stamp_user_state_expiry_created (principal_key, state, expired_at, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. 스탬프 리워드 쿠폰 사용 내역 (Stamp Coupon Usage)
CREATE TABLE IF NOT EXISTS stamp_coupon_usage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    stamp_id BIGINT NOT NULL,
    issued_coupon_id BIGINT NOT NULL,
    used_at DATETIME(6) NOT NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_stamp_coupon_usage_stamp (stamp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. 스탬프 이력 (Stamp History)
CREATE TABLE IF NOT EXISTS stamp_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    principal_key VARCHAR(100) NOT NULL,
    principal_type VARCHAR(20) NOT NULL,
    type VARCHAR(30) NOT NULL,
    store_id BIGINT NOT NULL,
    store_name VARCHAR(100) NOT NULL,
    quantity INT NOT NULL,
    recorded_at DATETIME(6) NOT NULL,
    expired_at DATETIME(6) NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_stamp_history_principal (principal_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. 트랜잭셔널 아웃박스 이벤트 로그 (Event Log)
CREATE TABLE IF NOT EXISTS event_log (
    event_id VARCHAR(100) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    event_log_target VARCHAR(30) NOT NULL,
    payload TEXT NOT NULL,
    is_published BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    published_at DATETIME(6) NULL,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    INDEX idx_event_recovery (is_published, event_log_target, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
