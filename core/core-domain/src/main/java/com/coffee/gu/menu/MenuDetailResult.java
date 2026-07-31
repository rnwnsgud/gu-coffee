package com.coffee.gu.menu;

import java.util.List;

public record MenuDetailResult(
        Menu menu,
        List<OptionGroup> optionGroups,
        List<Option> options
) {
}
