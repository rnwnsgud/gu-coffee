package com.coffee.gu.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OptionJpaRepository extends JpaRepository<OptionEntity, Long> {
    List<OptionEntity> findAllByOptionGroupIdIn(List<Long> optionGroupIds);

}
