package com.coffee.gu.menu;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.coffee.gu.menu.QMenuCategoryEntity.menuCategoryEntity;

@Repository
public class MenuCategoryRepositoryImpl implements MenuCategoryRepository {

    private final JPAQueryFactory queryFactory;

    public MenuCategoryRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<MenuCategory> findAllByMenuIdIn(List<Long> menuIds) {
        return queryFactory.selectFrom(menuCategoryEntity)
                .where(
                        menuCategoryEntity.menuId.in(menuIds),
                        menuCategoryEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(MenuCategoryEntity::toModel)
                .toList();
    }
}
