package com.coffee.gu.menu

import com.coffee.gu.CoreException
import com.coffee.gu.ErrorType
import com.coffee.gu.OffsetLimit
import com.coffee.gu.enums.EntityStatus
import com.coffee.gu.menu.QMenuCategoryEntity.menuCategoryEntity
import com.coffee.gu.menu.QMenuEntity.menuEntity
import com.querydsl.core.types.dsl.BooleanExpression
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
class MenuRepositoryImpl(
    private val menuJpaRepository: MenuJpaRepository,
    private val jpaQueryFactory: JPAQueryFactory,
) : MenuRepository {

    override fun findByCategoryId(categoryId: Long, offsetLimit: OffsetLimit): List<Menu> {
        return jpaQueryFactory
            .selectFrom(menuEntity)
            .distinct()
            .join(menuCategoryEntity)
            .on(menuEntity.id.eq(menuCategoryEntity.menuId))
            .where(
                menuCategoryEntity.categoryId.eq(categoryId),
                menuCategoryEntity.entityStatus.eq(EntityStatus.ACTIVE),
                menuEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .offset(offsetLimit.offset.toLong())
            .limit(offsetLimit.limit.toLong())
            .fetch()
            .map { it.toModel() }
    }

    override fun findByCategoryId(
        categoryId: Long,
        cursor: LocalDateTime?,
        lastId: Long?,
        pageSize: Int,
    ): List<Menu> {
        val entities = jpaQueryFactory
            .selectFrom(menuEntity)
            .distinct()
            .join(menuCategoryEntity).on(menuCategoryEntity.menuId.eq(menuEntity.id))
            .where(
                menuCategoryEntity.categoryId.eq(categoryId),
                menuCategoryEntity.entityStatus.eq(EntityStatus.ACTIVE),
                menuEntity.entityStatus.eq(EntityStatus.ACTIVE),
                cursorPagination(cursor, lastId),
            )
            .orderBy(menuEntity.createdAt.desc(), menuEntity.id.desc())
            .limit(pageSize.toLong() + 1)
            .fetch()

        return entities.map { it.toModel() }
    }

    override fun findById(menuId: Long): Menu {
        return jpaQueryFactory
            .selectFrom(menuEntity)
            .where(
                menuEntity.id.eq(menuId),
                menuEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetchFirst()
            ?.toModel() ?: throw CoreException(ErrorType.NOT_FOUND_DATA)
    }

    override fun findAllByIdIn(menuIds: List<Long>): List<Menu> {
        return jpaQueryFactory
            .selectFrom(menuEntity)
            .where(
                menuEntity.id.`in`(menuIds),
                menuEntity.entityStatus.eq(EntityStatus.ACTIVE),
            )
            .fetch()
            .map { it.toModel() }
    }

    private fun cursorPagination(cursor: LocalDateTime?, lastId: Long?): BooleanExpression? {
        if (cursor == null) {
            return null
        }
        return menuEntity.createdAt.before(cursor)
            .or(menuEntity.createdAt.eq(cursor).and(menuEntity.id.lt(lastId)))
    }
}
