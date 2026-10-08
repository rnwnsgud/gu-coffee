-- =======================================================================
-- seed-data.sql
-- Description: 로컬 개발 및 테스트 환경용 초기 시드 데이터
-- =======================================================================

-- 1. 카테고리 (Category)
INSERT INTO category (id, name, entity_status, created_at, updated_at) VALUES
(1, '커피 (Coffee)', 'ACTIVE', NOW(6), NOW(6)),
(2, '콜드브루 (Cold Brew)', 'ACTIVE', NOW(6), NOW(6)),
(3, '티 & 에이드 (Tea & Ade)', 'ACTIVE', NOW(6), NOW(6)),
(4, '디저트 & 베이커리 (Bakery)', 'ACTIVE', NOW(6), NOW(6));

-- 2. 메뉴 (Menu)
INSERT INTO menu (id, name, type, cost_price, sales_price, image_url, description, capacity, caffeine, calories, sodium, carbohydrate, sugar, fat, saturated_fat, protein, contained_allergens, may_contain_allergens, entity_status, created_at, updated_at) VALUES
(1, '아메리카노 (Americano)', 'DRINK', 1200.00, 4500.00, 'https://images.unsplash.com/photo-1514432324607-a09d9b4aefdd', '고소한 견과류 풍미와 깔끔한 바디감의 시그니처 아메리카노', 355.0, 150.0, 10.0, 5.0, 2.0, 0.0, 0.0, 0.0, 1.0, NULL, NULL, 'ACTIVE', NOW(6), NOW(6)),
(2, '카페라떼 (Cafe Latte)', 'DRINK', 1500.00, 5000.00, 'https://images.unsplash.com/photo-1570968915860-54d5c301fa9f', '진한 에스프레소와 부드러운 스팀밀크의 완벽한 조화', 355.0, 150.0, 180.0, 115.0, 14.0, 13.0, 9.0, 5.0, 10.0, '우유', NULL, 'ACTIVE', NOW(6), NOW(6)),
(3, '바닐라빈 라떼 (Vanilla Bean Latte)', 'DRINK', 1800.00, 5800.00, 'https://images.unsplash.com/photo-1534778101976-62847782c213', '천연 바닐라빈 시럽이 더해진 달콤하고 풍부한 풍미의 라떼', 355.0, 150.0, 240.0, 110.0, 30.0, 28.0, 8.0, 5.0, 9.0, '우유', NULL, 'ACTIVE', NOW(6), NOW(6)),
(4, '시그니처 콜드브루 (Cold Brew)', 'DRINK', 1600.00, 5500.00, 'https://images.unsplash.com/photo-1517701550927-30cf4ba1dba5', '14시간 동안 저온 침출하여 깔끔하고 초콜릿 풍미가 돋보이는 콜드브루', 473.0, 180.0, 15.0, 10.0, 2.0, 0.0, 0.0, 0.0, 1.0, NULL, NULL, 'ACTIVE', NOW(6), NOW(6)),
(5, '자몽 허니 블랙티 (Grapefruit Honey Black Tea)', 'DRINK', 1400.00, 5300.00, 'https://images.unsplash.com/photo-1556679343-c7306c1976bc', '새콤달콤한 자몽과 은은한 홍차가 어우러진 산뜻한 티 음료', 473.0, 70.0, 125.0, 15.0, 30.0, 28.0, 0.0, 0.0, 0.0, NULL, NULL, 'ACTIVE', NOW(6), NOW(6));

-- 3. 메뉴-카테고리 매핑 (Menu Category Mapping)
INSERT INTO menu_category (id, menu_id, category_id, entity_status, created_at, updated_at) VALUES
(1, 1, 1, 'ACTIVE', NOW(6), NOW(6)),
(2, 2, 1, 'ACTIVE', NOW(6), NOW(6)),
(3, 3, 1, 'ACTIVE', NOW(6), NOW(6)),
(4, 4, 2, 'ACTIVE', NOW(6), NOW(6)),
(5, 5, 3, 'ACTIVE', NOW(6), NOW(6));

-- 4. 옵션 그룹 (Option Group)
INSERT INTO option_group (id, name, is_exclusive, is_required, entity_status, created_at, updated_at) VALUES
(1, '온도 선택 (HOT/ICE)', true, true, 'ACTIVE', NOW(6), NOW(6)),
(2, '사이즈 선택', true, true, 'ACTIVE', NOW(6), NOW(6)),
(3, '에스프레소 샷 추가', false, false, 'ACTIVE', NOW(6), NOW(6)),
(4, '시럽 추가', false, false, 'ACTIVE', NOW(6), NOW(6));

-- 5. 옵션 (Option)
INSERT INTO `option` (id, option_group_id, name, extra_price, entity_status, created_at, updated_at) VALUES
(1, 1, 'HOT', 0.00, 'ACTIVE', NOW(6), NOW(6)),
(2, 1, 'ICE', 0.00, 'ACTIVE', NOW(6), NOW(6)),
(3, 2, 'Regular (기본)', 0.00, 'ACTIVE', NOW(6), NOW(6)),
(4, 2, 'Large (+500원)', 500.00, 'ACTIVE', NOW(6), NOW(6)),
(5, 2, 'Venti (+1000원)', 1000.00, 'ACTIVE', NOW(6), NOW(6)),
(6, 3, '샷 추가 1회', 500.00, 'ACTIVE', NOW(6), NOW(6)),
(7, 3, '디카페인 원두 변경', 700.00, 'ACTIVE', NOW(6), NOW(6)),
(8, 4, '바닐라 시럽 추가', 500.00, 'ACTIVE', NOW(6), NOW(6)),
(9, 4, '헤이즐넛 시럽 추가', 500.00, 'ACTIVE', NOW(6), NOW(6));

-- 6. 메뉴-옵션그룹 매핑 (Menu Option Group Mapping)
-- 아메리카노: 온도, 사이즈, 샷추가
INSERT INTO menu_option_group (id, menu_id, option_group_id, entity_status, created_at, updated_at) VALUES
(1, 1, 1, 'ACTIVE', NOW(6), NOW(6)),
(2, 1, 2, 'ACTIVE', NOW(6), NOW(6)),
(3, 1, 3, 'ACTIVE', NOW(6), NOW(6)),
-- 카페라떼: 온도, 사이즈, 샷추가, 시럽추가
(4, 2, 1, 'ACTIVE', NOW(6), NOW(6)),
(5, 2, 2, 'ACTIVE', NOW(6), NOW(6)),
(6, 2, 3, 'ACTIVE', NOW(6), NOW(6)),
(7, 2, 4, 'ACTIVE', NOW(6), NOW(6)),
-- 바닐라빈 라떼: 온도, 사이즈, 샷추가
(8, 3, 1, 'ACTIVE', NOW(6), NOW(6)),
(9, 3, 2, 'ACTIVE', NOW(6), NOW(6)),
(10, 3, 3, 'ACTIVE', NOW(6), NOW(6)),
-- 콜드브루: 사이즈, 샷추가
(11, 4, 2, 'ACTIVE', NOW(6), NOW(6)),
(12, 4, 3, 'ACTIVE', NOW(6), NOW(6)),
-- 자몽 허니 블랙티: 온도, 사이즈
(13, 5, 1, 'ACTIVE', NOW(6), NOW(6)),
(14, 5, 2, 'ACTIVE', NOW(6), NOW(6));

-- 7. 매장 (Store)
INSERT INTO store (id, name, branch_code, status, address, latitude, longitude, phone_number, representative, trade_name, business_registration_number, business_address, entity_status, created_at, updated_at) VALUES
(1, '구커피 강남본점', 'STORE-GANGNAM-01', 'OPEN', '서울시 강남구 테헤란로 123', 37.4979, 127.0276, '02-1234-5678', '구준형', '구커피 강남', '123-45-67890', '서울시 강남구 테헤란로 123', 'ACTIVE', NOW(6), NOW(6)),
(2, '구커피 판교역점', 'STORE-PANGYO-01', 'OPEN', '경기도 성남시 분당구 판교역로 100', 37.3948, 127.1119, '031-9876-5432', '구준형', '구커피 판교', '987-65-43210', '경기도 성남시 분당구 판교역로 100', 'ACTIVE', NOW(6), NOW(6));

-- 8. 매장 영업시간 (Sales Hour)
INSERT INTO sales_hour (id, store_id, `day`, open, close, entity_status, created_at, updated_at) VALUES
(1, 1, 'MONDAY', '08:00:00', '22:00:00', 'ACTIVE', NOW(6), NOW(6)),
(2, 1, 'TUESDAY', '08:00:00', '22:00:00', 'ACTIVE', NOW(6), NOW(6)),
(3, 1, 'WEDNESDAY', '08:00:00', '22:00:00', 'ACTIVE', NOW(6), NOW(6)),
(4, 1, 'THURSDAY', '08:00:00', '22:00:00', 'ACTIVE', NOW(6), NOW(6)),
(5, 1, 'FRIDAY', '08:00:00', '22:00:00', 'ACTIVE', NOW(6), NOW(6)),
(6, 1, 'SATURDAY', '09:00:00', '22:00:00', 'ACTIVE', NOW(6), NOW(6)),
(7, 1, 'SUNDAY', '09:00:00', '21:00:00', 'ACTIVE', NOW(6), NOW(6)),
(8, 2, 'MONDAY', '08:00:00', '21:00:00', 'ACTIVE', NOW(6), NOW(6)),
(9, 2, 'TUESDAY', '08:00:00', '21:00:00', 'ACTIVE', NOW(6), NOW(6)),
(10, 2, 'WEDNESDAY', '08:00:00', '21:00:00', 'ACTIVE', NOW(6), NOW(6)),
(11, 2, 'THURSDAY', '08:00:00', '21:00:00', 'ACTIVE', NOW(6), NOW(6)),
(12, 2, 'FRIDAY', '08:00:00', '21:00:00', 'ACTIVE', NOW(6), NOW(6)),
(13, 2, 'SATURDAY', '09:00:00', '21:00:00', 'ACTIVE', NOW(6), NOW(6)),
(14, 2, 'SUNDAY', '09:00:00', '20:00:00', 'ACTIVE', NOW(6), NOW(6));

-- 9. 쿠폰 마스터 (Coupon)
INSERT INTO coupon (id, name, type, discount, expired_at, entity_status, created_at, updated_at) VALUES
(1, '[오픈기념] 1,000원 할인 쿠폰', 'FIXED_AMOUNT', 1000.00, '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6)),
(2, '[웰컴쿠폰] 2,000원 특별 할인 쿠폰', 'FIXED_AMOUNT', 2000.00, '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6)),
(3, '[스탬프완성] 아메리카노 1잔 무료 교환권', 'FREE_DRINK', 4500.00, '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6));

-- 10. 쿠폰 타겟 매핑 (Coupon Target)
INSERT INTO coupon_target (id, coupon_id, target_type, target_id, entity_status, created_at, updated_at) VALUES
(1, 1, 'MENU_CATEGORY', 1, 'ACTIVE', NOW(6), NOW(6)),
(2, 2, 'MENU_CATEGORY', 1, 'ACTIVE', NOW(6), NOW(6)),
(3, 3, 'MENU', 1, 'ACTIVE', NOW(6), NOW(6));

-- 11. 테스트 사용자(U1)에게 쿠폰 발급 (Issued Coupon)
INSERT INTO issued_coupon (id, principal_key, principal_type, coupon_id, state, entity_status, created_at, updated_at) VALUES
(1, 'U1', 'USER', 1, 'DOWNLOADED', 'ACTIVE', NOW(6), NOW(6)),
(2, 'U1', 'USER', 2, 'DOWNLOADED', 'ACTIVE', NOW(6), NOW(6));

-- 12. 테스트 사용자(U1) 스탬프 3개 적립 (Stamp)
INSERT INTO stamp (id, order_key, principal_key, principal_type, state, expired_at, entity_status, created_at, updated_at) VALUES
(1, 'ORDER-SEED-01', 'U1', 'USER', 'EARNED', '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6)),
(2, 'ORDER-SEED-01', 'U1', 'USER', 'EARNED', '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6)),
(3, 'ORDER-SEED-02', 'U1', 'USER', 'EARNED', '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6));

-- 13. 스탬프 적립 이력 (Stamp History)
INSERT INTO stamp_history (id, principal_key, principal_type, type, store_id, store_name, quantity, recorded_at, expired_at, entity_status, created_at, updated_at) VALUES
(1, 'U1', 'USER', 'EARNED', 1, '구커피 강남본점', 2, NOW(6), '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6)),
(2, 'U1', 'USER', 'EARNED', 1, '구커피 강남본점', 1, NOW(6), '2030-12-31 23:59:59.000000', 'ACTIVE', NOW(6), NOW(6));
