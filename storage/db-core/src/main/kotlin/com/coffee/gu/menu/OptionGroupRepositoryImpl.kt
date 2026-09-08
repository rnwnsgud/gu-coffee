package com.coffee.gu.menu

import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.menu.QOptionGroupEntity.optionGroupEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class OptionGroupRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
) : OptionGroupRepository {

    override fun findAllById(ids: List<Long>): List<OptionGroup> {
        return jpaQueryFactory.selectFrom(optionGroupEntity)
            .where(
                optionGroupEntity.id.`in`(ids),
                optionGroupEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }
}
