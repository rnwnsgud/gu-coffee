package com.coffee.gu.menu;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.coffee.gu.menu.QMenuCategoryEntity.*;
import static com.coffee.gu.menu.QMenuEntity.menuEntity;

@Repository
public class MenuRepositoryImpl implements MenuRepository {

    private final MenuJpaRepository menuJpaRepository;
    private final JPAQueryFactory jpaQueryFactory;

    public MenuRepositoryImpl(MenuJpaRepository menuJpaRepository, JPAQueryFactory jpaQueryFactory) {
        this.menuJpaRepository = menuJpaRepository;
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<Menu> findByCategoryId(Long categoryId, Pageable pageable) {
        return jpaQueryFactory
                .selectFrom(menuEntity)
                .distinct()
                .join(menuCategoryEntity)
                .on(menuEntity.id.eq(menuCategoryEntity.menuId))
                .where(
                        menuCategoryEntity.categoryId.eq(categoryId),
                        menuCategoryEntity.entityStatus.eq(EntityStatus.ACTIVE),
                        menuEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch()
                .stream()
                .map(MenuEntity::toModel)
                .toList();
    }

    @Override
    public List<Menu> findByCategoryId(Long categoryId, LocalDateTime cursor, Long lastId, int pageSize) {
        List<MenuEntity> entities = jpaQueryFactory
                .selectFrom(menuEntity)
                .distinct()
                .join(menuCategoryEntity).on(menuCategoryEntity.menuId.eq(menuEntity.id))
                .where(
                        menuCategoryEntity.categoryId.eq(categoryId),
                        menuCategoryEntity.entityStatus.eq(EntityStatus.ACTIVE),
                        menuEntity.entityStatus.eq(EntityStatus.ACTIVE),
                        cursorPagination(cursor, lastId)
                )
                .orderBy(menuEntity.createdAt.desc(), menuEntity.id.desc())
                .limit(pageSize + 1)
                .fetch();

        return entities.stream().map(MenuEntity::toModel).toList();
    }

    @Override
    public Optional<Menu> findById(Long menuId) {
        return Optional.ofNullable(
                jpaQueryFactory
                .selectFrom(menuEntity)
                .where(
                        menuEntity.id.eq(menuId),
                        menuEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetchFirst()
        ).map(MenuEntity::toModel);
    }

    @Override
    public List<Menu> findAllByIdIn(List<Long> menuIds) {
        return jpaQueryFactory
                .selectFrom(menuEntity)
                .where(
                        menuEntity.id.in(menuIds),
                        menuEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(MenuEntity::toModel)
                .toList();
    }

    private BooleanExpression cursorPagination(LocalDateTime cursor, Long lastId) {
        if (cursor == null) {
            return null;
        }
        return menuEntity.createdAt.before(cursor)
                .or(menuEntity.createdAt.eq(cursor).and(menuEntity.id.lt(lastId)));
    }

}
