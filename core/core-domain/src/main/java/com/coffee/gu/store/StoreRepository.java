package com.coffee.gu.store;


import java.util.List;
import java.util.Optional;

public interface StoreRepository {
   Store save(Store store);
   Optional<Store> findById(Long id);
   List<Store> findAllByGeography(double latitude, double longitude, double radiusKm);
}
