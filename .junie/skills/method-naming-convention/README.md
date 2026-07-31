---
name: method-naming-convention
description: 프로젝트의 일관된 메서드 네이밍 규칙(get vs find 등)을 정의하고 적용합니다.
---

# Method Naming Convention Skill

이 스킬은 프로젝트 내에서 메서드 이름을 명명할 때 일관성을 유지하기 위한 규칙을 담고 있습니다. 특히 `Reader`, `Finder`, `Repository` 계층에서 데이터를 조회하거나 존재 여부를 확인할 때 적용됩니다.

## 핵심 네이밍 규칙

### 1. `exist...()` 메서드
- **목적**: 데이터의 존재 여부를 확인합니다.
- **동작**: 
  - 존재하면 `true`, 존재하지 않으면 `false`를 반환합니다.
  - **주의**: `Optional` 사용을 지양합니다.
- **예시**:
  ```java
  public Boolean existByUserAndCoupon(com.coffee.gu.Principal principal, Long couponId) {
      return issuedCouponRepository.existsByUserIdAndCouponId(principal.getKey(), couponId);
  }
  ```

### 2. `get...()` 메서드
- **목적**: 특정 조건에 맞는 데이터가 반드시 존재할 것으로 예상될 때 사용합니다.
- **동작**:
  - 데이터가 존재하면 해당 객체를 즉시 반환합니다.
  - 데이터가 존재하지 않으면 **예외(com.coffee.gu.CoreException 등)를 발생**시킵니다.
- **예시**:
  ```java
  public Order getByPrincipalAndOrderKey(com.coffee.gu.Principal principal, String orderKey, OrderState state) {
      return findByPrincipal(principal, state)
              .stream()
              .filter(o -> o.getKey().equals(orderKey))
              .findFirst()
              .orElseThrow(() -> new com.coffee.gu.CoreException(com.coffee.gu.ErrorType.NOT_FOUND_DATA, null));
  }
  ```

### 3. `find...()` 메서드
- **목적**: 데이터가 존재하지 않을 가능성이 있을 때(검색 등) 사용합니다.
- **동작**:
  - **단일 조회**: `Optional<T>`를 반환합니다.
  - **다건 조회**: `List<T>`를 반환하며, 결과가 없으면 **빈 리스트**를 반환합니다.
- **예시**:
  ```java
  // 단일 조회 (Optional 반환)
  public Optional<CartItemEntity> findEntityByPrincipalAndMenuId(com.coffee.gu.Principal principal, Long menuId) {
      return findByPrincipal(principal).stream()
              .filter(item -> item.getMenuId().equals(menuId))
              .findFirst();
  }

  // 다건 조회 (빈 리스트 반환)
  public List<Order> findByPrincipal(com.coffee.gu.Principal principal, OrderState state) {
      List<OrderEntity> orders = orderRepository.findByPrincipalKey(principal.getKey())
              .stream()
              .filter(OrderEntity::isActive)
              .filter(order -> order.getState() == state)
              .toList();

      return orders.isEmpty() ? List.of() : toOrders(orders);
  }
  ```

## 사용 시점
- 새로운 `Reader`나 `Finder` 클래스를 생성할 때
- 기존 코드의 메서드 네이밍이 규칙과 맞지 않아 리팩토링이 필요할 때
- 코드 리뷰 시 네이밍의 적절성을 판단할 때

## 적용 계층
이 규칙은 주로 다음 계층에 적용됩니다:
- `...Reader`
- `...Finder`
- `...Repository`
- `...Service` (데이터 조회 성격의 메서드)
