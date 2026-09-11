package com.coffee.gu.api.controller.v1

import com.coffee.gu.api.controller.v1.request.StoreSearchRequest
import com.coffee.gu.store.Store
import com.coffee.gu.store.StoreService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class StoreController(
    private val storeService: StoreService,
) {
    @GetMapping("/v1/stores")
    fun getStores(@RequestBody request: StoreSearchRequest): List<Store> {
        return storeService.getAroundStores(request.toStoreSearch())
    }
}
