package com.coffee.gu.menu;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.coffee.gu.menu.QOptionEntity.optionEntity;

@Repository
public class OptionRepositoryImpl implements OptionRepository{

    private final JPAQueryFactory queryFactory;

    public OptionRepositoryImpl(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    public List<Option> findAllByIdOptionGroupIdIn(List<Long> optionGroupIds) {
        return queryFactory.selectFrom(optionEntity)
                .where(
                        optionEntity.optionGroupId.in(optionGroupIds),
                        optionEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(OptionEntity::toModel)
                .toList();
    }
}
