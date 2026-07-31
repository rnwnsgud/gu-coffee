package com.coffee.gu.cart;

import com.coffee.gu.Principal;
import com.coffee.gu.enums.PrincipalType;
import com.coffee.gu.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Table(name = "cart_item")
@Entity
public class CartItemEntity extends BaseEntity {
    private String principalKey;
    @Enumerated(EnumType.STRING)
    private PrincipalType principalType;
    private Long menuId;
    private Long quantity;

    protected CartItemEntity() {}

    public CartItemEntity(Long id,String principalKey, PrincipalType principalType, Long menuId, Long quantity) {
        this.id = id;
        this.principalKey = principalKey;
        this.principalType = principalType;
        this.menuId = menuId;
        this.quantity = quantity;
    }

    public static CartItemEntity of(CartItem cartItem, Principal principal) {
        return new CartItemEntity(cartItem.getId(), principal.getKey(), principal.getType(), cartItem.getMenu().getId(), cartItem.getQuantity());
    }

    public CartItem toModel() {
        return CartItem.createUnresolved(id, menuId, quantity, isDeleted());
    }

    public Long getQuantity() {
        return quantity;
    }


    public Long getMenuId() {
        return menuId;
    }
}
