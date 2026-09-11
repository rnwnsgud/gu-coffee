package com.coffee.gu.menu

import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.menu.QMenuOptionGroupEntity.menuOptionGroupEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class MenuOptionRepositoryImpl(
    private val jpaQueryFactory: JPAQueryFactory,
) : MenuOptionGroupRepository {

    override fun findByMenuId(menuId: Long): List<MenuOptionGroup> {
        return jpaQueryFactory.selectFrom(menuOptionGroupEntity)
            .where(
                menuOptionGroupEntity.menuId.eq(menuId),
                menuOptionGroupEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }
}
