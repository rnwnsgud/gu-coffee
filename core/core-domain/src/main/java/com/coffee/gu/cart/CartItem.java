package com.coffee.gu.cart;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.menu.Menu;

public class CartItem {
    private Long id;
    private Menu menu;
    private Long quantity;
    private boolean isDeleted;

    public CartItem(Long id, Menu menu, Long quantity, boolean isDeleted) {
        this.id = id;
        this.menu = menu;
        this.quantity = quantity;
        this.isDeleted = isDeleted;
    }

    public static CartItem create(Long menuId, Long quantity) {
        return new CartItem(null, Menu.createIdOnly(menuId), quantity, false);
    }

    public static CartItem createUnresolved(Long id, Long menuId, long quantity, boolean isDeleted) {
        return new CartItem(id, Menu.createIdOnly(menuId), quantity, isDeleted);
    }

    public CartItem resolveMenu(Menu completedMenu) {
        if (!this.menu.getId().equals(completedMenu.getId())) {
            throw new CoreException(ErrorType.SYSTEM_LOGIC_ERROR , "장바구니의 메뉴 ID와 조립하려는 메뉴 ID가 일치하지 않습니다.");
        }
        return new CartItem(this.id, completedMenu, this.quantity, this.isDeleted);
    }

    public void applyQuantity(Long quantity) {
        this.quantity = (quantity < 1) ? 1 : quantity;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void active() {
        this.isDeleted = false;
    }

    public Long getId() {
        return id;
    }
    public Menu getMenu() {
        return menu;
    }
    public Long getQuantity() {
        return quantity;
    }
    public boolean getIsDeleted() {
        return isDeleted;
    }
}
