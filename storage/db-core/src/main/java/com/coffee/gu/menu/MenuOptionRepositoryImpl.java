package com.coffee.gu.menu;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.coffee.gu.menu.QMenuOptionGroupEntity.menuOptionGroupEntity;

@Repository
public class MenuOptionRepositoryImpl implements MenuOptionGroupRepository{

    private final JPAQueryFactory jpaQueryFactory;

    public MenuOptionRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<MenuOptionGroup> findByMenuId(Long menuId) {
        return jpaQueryFactory.selectFrom(menuOptionGroupEntity)
                .where(
                        menuOptionGroupEntity.menuId.eq(menuId),
                        menuOptionGroupEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(MenuOptionGroupEntity::toModel)
                .toList();
    }
}
