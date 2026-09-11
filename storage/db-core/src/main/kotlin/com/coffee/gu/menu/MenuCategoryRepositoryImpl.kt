package com.coffee.gu.menu

import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.menu.QMenuCategoryEntity.menuCategoryEntity
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class MenuCategoryRepositoryImpl(
    private val queryFactory: JPAQueryFactory,
) : MenuCategoryRepository {

    override fun findAllByMenuIdIn(menuIds: List<Long>): List<MenuCategory> {
        return queryFactory.selectFrom(menuCategoryEntity)
            .where(
                menuCategoryEntity.menuId.`in`(menuIds),
                menuCategoryEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }
}
