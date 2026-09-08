package com.coffee.gu.coupon

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.coupon.QCouponEntity.couponEntity
import com.coffee.gu.coupon.QIssuedCouponEntity.issuedCouponEntity
import com.coffee.gu.enums.EntityStatus
import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
class IssuedCouponRepositoryImpl(
    private val issuedCouponJpaRepository: IssuedCouponJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
) : IssuedCouponRepository {

    override fun save(issuedCoupon: IssuedCoupon): IssuedCoupon {
        return issuedCouponJpaRepository.save(IssuedCouponEntity.from(issuedCoupon))
            .toModel(issuedCoupon.coupon)
    }

    override fun saveAll(issuedCoupons: List<IssuedCoupon>): List<IssuedCoupon> {
        val couponMap = issuedCoupons.map { it.coupon }.distinctBy { it.id }.associateBy { it.id }
        val entities = issuedCoupons.map { IssuedCouponEntity.from(it) }
        val savedEntities = issuedCouponJpaRepository.saveAll(entities)
        return savedEntities.mapNotNull { entity ->
            couponMap[entity.couponId]?.let { coupon ->
                entity.toModel(coupon)
            }
        }
    }

    override fun findAllByPrincipalKey(principalKey: String): List<IssuedCoupon> {
        val issuedCoupons = jpaQueryFactory
            .selectFrom(issuedCouponEntity)
            .where(
                issuedCouponEntity.principalKey.eq(principalKey),
                issuedCouponEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
        return getIssuedCoupons(issuedCoupons)
    }

    override fun existsByPrincipalKeyAndCouponId(principalKey: String, couponId: Long): Boolean {
        return jpaQueryFactory.selectFrom(issuedCouponEntity)
            .where(
                issuedCouponEntity.principalKey.eq(principalKey),
                issuedCouponEntity.couponId.eq(couponId),
                issuedCouponEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetchFirst() != null
    }

    override fun findById(issuedCouponId: Long): IssuedCoupon {
        val issuedCoupon = issuedCouponJpaRepository.findById(issuedCouponId).orElseThrow { CoreException(ErrorType.NOT_FOUND_DATA) }
        val coupon = jpaQueryFactory
            .selectFrom(couponEntity)
            .where(couponEntity.id.eq(issuedCoupon.couponId))
            .fetchFirst()

        return issuedCoupon.toModel(coupon?.toModel() ?: throw CoreException(ErrorType.NOT_FOUND_DATA))
    }

    override fun findAllByIdIn(issuedCouponIds: List<Long>): List<IssuedCoupon> {
        val issuedCoupons = jpaQueryFactory
            .selectFrom(issuedCouponEntity)
            .where(
                issuedCouponEntity.id.`in`(issuedCouponIds),
                issuedCouponEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
        return getIssuedCoupons(issuedCoupons)
    }

    private fun getIssuedCoupons(issuedCoupons: List<IssuedCouponEntity>): List<IssuedCoupon> {
        if (issuedCoupons.isEmpty()) return emptyList()
        val couponIds = issuedCoupons.map { it.couponId }.distinct()
        val coupons = jpaQueryFactory
            .selectFrom(couponEntity)
            .where(couponEntity.id.`in`(couponIds))
            .fetch()
        val couponMap = coupons.associateBy { it.id }
        return issuedCoupons.mapNotNull { entity ->
            val couponEntity = couponMap[entity.couponId] ?: return@mapNotNull null
            entity.toModel(couponEntity.toModel())
        }
    }
}
