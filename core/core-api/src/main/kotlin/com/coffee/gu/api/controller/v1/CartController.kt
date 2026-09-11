package com.coffee.gu.api.controller.v1

import com.coffee.gu.Principal
import com.coffee.gu.api.controller.v1.request.AddCartItemRequest
import com.coffee.gu.api.controller.v1.request.ModifyCartItemRequest
import com.coffee.gu.api.controller.v1.response.CartResponse
import com.coffee.gu.cart.CartService
import com.coffee.gu.response.ApiResponse
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class CartController(
    private val cartService: CartService,
) {
    @GetMapping("/v1/cart")
    fun getCart(principal: Principal): ApiResponse<CartResponse> {
        val cart = cartService.getCart(principal)
        return ApiResponse.success(CartResponse(cart.items.map { CartResponse.CartItemResponse.from(it) }))
    }

    @PostMapping("/v1/cart/items")
    fun addCartItem(
        principal: Principal,
        @RequestBody request: AddCartItemRequest,
        ): ApiResponse<Unit> {
        cartService.addCartItem(principal, request.toAddCartItem())
        return ApiResponse.success()
    }

    @PutMapping("/v1/cart/items/{cartItemId}")
    fun modifyCartItem(
        principal: Principal,
        @PathVariable cartItemId: Long,
        @RequestBody request: ModifyCartItemRequest,
    ): ApiResponse<Unit> {
        cartService.modifyCartItem(principal, request.toModifyCartItem(cartItemId))
        return ApiResponse.success()
    }

    @DeleteMapping("/v1/cart/items/{cartItemId}")
    fun deleteCartItem(
        principal: Principal,
        @PathVariable cartItemId: Long,
    ): ApiResponse<Unit> {
        cartService.deleteCartItem(principal, cartItemId)
        return ApiResponse.success()
    }
}
