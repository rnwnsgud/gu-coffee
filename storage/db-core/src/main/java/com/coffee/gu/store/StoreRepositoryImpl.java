package com.coffee.gu.store;


import com.coffee.gu.enums.EntityStatus;
import com.coffee.gu.enums.StoreStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.coffee.gu.store.QStoreEntity.*;


@Repository
public class StoreRepositoryImpl implements StoreRepository {

    private final StoreJpaRepository storeJpaRepository;
    private final JPAQueryFactory queryFactory;

    public StoreRepositoryImpl(JPAQueryFactory queryFactory, StoreJpaRepository storeJpaRepository) {
        this.queryFactory = queryFactory;
        this.storeJpaRepository = storeJpaRepository;
    }


    @Override
    public Store save(Store store) {
        return storeJpaRepository.save(StoreEntity.from(store)).toModel();
    }

    @Override
    public Optional<Store> findById(Long id) {
        return Optional.ofNullable(
            queryFactory.selectFrom(storeEntity)
                .where(
                        storeEntity.id.eq(id),
                        storeEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetchFirst()
        ).map(StoreEntity::toModel);
    }

    @Override
    public List<Store> findAllByGeography(double latitude, double longitude, double radiusKm) {
        double latDegreeDelta = radiusKm / 111.0;
        double lonDegreeDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(latitude)));

        double minLat = latitude - latDegreeDelta;
        double maxLat = latitude + latDegreeDelta;
        double minLon = longitude - lonDegreeDelta;
        double maxLon = longitude + lonDegreeDelta;

        List<StoreEntity> storesInBox = queryFactory
                .selectFrom(storeEntity)
                .where(
                        storeEntity.status.eq(StoreStatus.OPEN),
                        storeEntity.latitude.between(minLat, maxLat),
                        storeEntity.longitude.between(minLon, maxLon)
                )
                .fetch();
        return storesInBox.stream()
                .map(StoreEntity::toModel)
                .sorted(Comparator.comparingDouble(store ->
                        calculateDistance(latitude, longitude, store.getSalesInformation().location().latitude(), store.getSalesInformation().location().longitude())
                ))
                .toList();
    }

    public static double calculateDistance(double userLat, double userLon, double storeLat, double storeLon) {
        double EARTH_RADIUS = 6371.01;
        double deltaLat = Math.toRadians(storeLat - userLat);
        double deltaLon = Math.toRadians(storeLon - userLon);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(storeLat))
                * Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }
}
