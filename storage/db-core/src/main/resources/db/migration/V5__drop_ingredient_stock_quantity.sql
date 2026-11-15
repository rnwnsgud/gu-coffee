-- V5: 매장별 원자재(ingredient) 테이블에서 수량(stock_quantity) 컬럼 제거 (가용 여부 is_available 중심으로 단순화)
ALTER TABLE ingredient DROP COLUMN stock_quantity;
