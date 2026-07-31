package com.coffee.gu.menu;

import java.util.List;

public interface OptionGroupRepository {
    List<OptionGroup> findAllById(List<Long> ids);
}
