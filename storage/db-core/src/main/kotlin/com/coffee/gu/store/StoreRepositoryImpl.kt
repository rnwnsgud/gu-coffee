package com.coffee.gu.store

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.enums.StoreStatus
import com.coffee.gu.store.QStoreEntity.storeEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.util.Optional
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Repository
class StoreRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
    private val storeJpaRepository: StoreJpaRepository,
) : StoreRepository {

    override fun save(store: Store): Store {
        return storeJpaRepository.save(StoreEntity.from(store)).toModel()
    }

    override fun findById(id: Long): Store {
        return queryFactory.selectFrom(storeEntity)
            .where(
                storeEntity.id.eq(id),
                storeEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetchFirst()
            ?.toModel()
            ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    override fun findAllByGeography(latitude: Double, longitude: Double, radiusKm: Double): List<Store> {
        val latDegreeDelta = radiusKm / 111.0
        val lonDegreeDelta = radiusKm / (111.0 * cos(Math.toRadians(latitude)))

        val minLat = latitude - latDegreeDelta
        val maxLat = latitude + latDegreeDelta
        val minLon = longitude - lonDegreeDelta
        val maxLon = longitude + lonDegreeDelta

        val storesInBox = queryFactory
            .selectFrom(storeEntity)
            .where(
                storeEntity.status.eq(StoreStatus.OPEN),
                storeEntity.latitude.between(minLat, maxLat),
                storeEntity.longitude.between(minLon, maxLon),
            )
            .fetch()

        return storesInBox
            .map { it.toModel() }
            .sortedBy { store ->
                val loc = store.salesInformation?.location
                if (loc != null) {
                    calculateDistance(latitude, longitude, loc.latitude, loc.longitude)
                } else {
                    Double.MAX_VALUE
                }
            }
    }

    companion object {
        @JvmStatic
        fun calculateDistance(userLat: Double, userLon: Double, storeLat: Double, storeLon: Double): Double {
            val earthRadius = 6371.01
            val deltaLat = Math.toRadians(storeLat - userLat)
            val deltaLon = Math.toRadians(storeLon - userLon)

            val a = sin(deltaLat / 2) * sin(deltaLat / 2) +
                cos(Math.toRadians(userLat)) * cos(Math.toRadians(storeLat)) *
                    sin(deltaLon / 2) * sin(deltaLon / 2)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            return earthRadius * c
        }
    }
}
