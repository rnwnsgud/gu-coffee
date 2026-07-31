package com.coffee.gu.cart;

import com.coffee.gu.CoreException;
import com.coffee.gu.ErrorType;
import com.coffee.gu.Principal;
import com.coffee.gu.menu.Menu;
import com.coffee.gu.menu.MenuFinder;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CartReader {

    private final CartItemRepository cartItemRepository;
    private final MenuFinder menuFinder;

    public CartReader(CartItemRepository cartItemRepository, MenuFinder menuFinder) {
        this.cartItemRepository = cartItemRepository;
        this.menuFinder = menuFinder;
    }

    public List<CartItem> findByPrincipal(Principal principal) {
        List<CartItem> cartItems = cartItemRepository.findByPrincipalKey(principal.getKey());
        if(cartItems.isEmpty()) return List.of();
        List<Long> menuIds = cartItems.stream()
                .map(cartItem -> cartItem.getMenu().getId())
                .toList();
        Map<Long, Menu> menuMap = menuFinder.findAllByIdIn(menuIds)
                .stream()
                .collect(Collectors.toMap(Menu::getId, Function.identity()));
        return cartItems.stream()
                .map(cartItem -> cartItem.resolveMenu(menuMap.get(cartItem.getMenu().getId()))).toList();
    }

    public Optional<CartItem> findByPrincipalAndMenuId(Principal principal, Long menuId) {
        Optional<CartItem> cartItem = cartItemRepository.findByPrincipalIncludingDeleted(principal.getKey());
        if (cartItem.isEmpty()) return cartItem;
        return cartItem.filter(item -> item.getMenu().getId().equals(menuId));
    }

    public CartItem getByPrincipalAndId(Principal principal, Long cartItemId) {
        List<CartItem> cartItems = findByPrincipal(principal);
        return cartItems.stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst()
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND_DATA, null));
    }


}
