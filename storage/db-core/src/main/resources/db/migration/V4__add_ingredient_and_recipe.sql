-- V4: 매장별 원자재(Ingredient), 메뉴 레시피(BOM: MenuRecipe) 테이블 및 주문 채널(order_channel) 컬럼 추가
CREATE TABLE IF NOT EXISTS ingredient (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    stock_quantity INT NOT NULL DEFAULT 0,
    is_available BOOLEAN NOT NULL DEFAULT TRUE,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_ingredient_store_ingredient UNIQUE (store_id, ingredient_id),
    INDEX idx_ingredient_store_id (store_id),
    INDEX idx_ingredient_status (entity_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS menu_recipe (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    menu_id BIGINT NOT NULL,
    ingredient_id BIGINT NOT NULL,
    required_quantity INT NOT NULL DEFAULT 1,
    entity_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_menu_recipe_menu_ingredient UNIQUE (menu_id, ingredient_id),
    INDEX idx_menu_recipe_menu_id (menu_id),
    INDEX idx_menu_recipe_ingredient_id (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

ALTER TABLE `order` ADD COLUMN order_channel VARCHAR(30) NOT NULL DEFAULT 'APP';
