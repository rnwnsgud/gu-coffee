package com.coffee.gu.api.controller.v1;

import com.coffee.gu.api.controller.v1.request.StoreSearchRequest;
import com.coffee.gu.store.Store;
import com.coffee.gu.store.StoreService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/v1/stores")
    public List<Store> getStores(@RequestBody StoreSearchRequest request) {
        return storeService.getAroundStores(request.toStoreSearch());
    }
}
