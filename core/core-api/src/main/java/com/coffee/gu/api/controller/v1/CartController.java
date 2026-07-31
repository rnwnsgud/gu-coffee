package com.coffee.gu.api.controller.v1;

import com.coffee.gu.Principal;
import com.coffee.gu.api.controller.v1.request.AddCartItemRequest;
import com.coffee.gu.api.controller.v1.request.ModifyCartItemRequest;
import com.coffee.gu.api.controller.v1.response.CartResponse;
import com.coffee.gu.cart.Cart;
import com.coffee.gu.cart.CartService;

import com.coffee.gu.response.ApiResponse;
import org.springframework.web.bind.annotation.*;

@RestController
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/v1/cart")
    public ApiResponse<CartResponse> getCart(Principal principal) {
        Cart cart = cartService.getCart(principal);
        return ApiResponse.success(new CartResponse(cart.getItems().stream().map(CartResponse.CartItemResponse::from).toList()));
    }

    @PostMapping("/v1/cart/items")
    public ApiResponse<?> addCartItem(Principal principal, @RequestBody AddCartItemRequest request) {
        cartService.addCartItem(principal, request.toAddCartItem());
        return ApiResponse.success();
    }

    @PutMapping("/v1/cart/items/{cartItemId}")
    public ApiResponse<?> modifyCartItem(
            Principal principal,
            @PathVariable Long cartItemId,
            @RequestBody ModifyCartItemRequest request) {
        cartService.modifyCartItem(principal, request.toModifyCartItem(cartItemId));
        return ApiResponse.success();
    }

    @DeleteMapping("/v1/cart/items/{cartItemId}")
    public ApiResponse<?> deleteCartItem(
            Principal principal,
            @PathVariable Long cartItemId) {
        cartService.deleteCartItem(principal, cartItemId);
        return ApiResponse.success();
    }

}
