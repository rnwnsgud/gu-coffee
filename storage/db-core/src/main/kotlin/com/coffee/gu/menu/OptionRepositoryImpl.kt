package com.coffee.gu.menu

import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.menu.QOptionEntity.optionEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class OptionRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : OptionRepository {

    override fun findAllByIdOptionGroupIdIn(optionGroupIds: List<Long>): List<Option> {
        return queryFactory.selectFrom(optionEntity)
            .where(
                optionEntity.optionGroupId.`in`(optionGroupIds),
                optionEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }
}
