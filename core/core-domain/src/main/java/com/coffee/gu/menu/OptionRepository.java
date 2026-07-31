package com.coffee.gu.menu;

import java.util.List;

public interface OptionRepository {
    List<Option> findAllByIdOptionGroupIdIn(List<Long> optionGroupIds);
}
