package com.coffee.gu.menu;

import com.coffee.gu.enums.EntityStatus;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;

import java.util.List;

import static com.coffee.gu.menu.QOptionGroupEntity.optionGroupEntity;

@Repository
public class OptionGroupRepositoryImpl implements OptionGroupRepository{

    private final JPAQueryFactory jpaQueryFactory;

    public OptionGroupRepositoryImpl(JPAQueryFactory jpaQueryFactory) {
        this.jpaQueryFactory = jpaQueryFactory;
    }

    @Override
    public List<OptionGroup> findAllById(List<Long> ids) {
        return jpaQueryFactory.selectFrom(optionGroupEntity)
                .where(
                        optionGroupEntity.id.in(ids),
                        optionGroupEntity.entityStatus.eq(EntityStatus.ACTIVE)
                )
                .fetch()
                .stream()
                .map(OptionGroupEntity::toModel)
                .toList();
    }
}
