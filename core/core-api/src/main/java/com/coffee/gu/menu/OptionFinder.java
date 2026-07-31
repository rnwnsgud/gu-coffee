package com.coffee.gu.menu;


import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OptionFinder {

    private final MenuOptionGroupRepository menuOptionGroupRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final OptionRepository optionRepository;

    public OptionFinder(MenuOptionGroupRepository menuOptionGroupRepository, OptionGroupRepository optionGroupRepository, OptionRepository optionRepository) {
        this.menuOptionGroupRepository = menuOptionGroupRepository;
        this.optionGroupRepository = optionGroupRepository;
        this.optionRepository = optionRepository;
    }

    public List<OptionGroup> findByMenuId(Long menuId) {
        List<MenuOptionGroup> mappings = menuOptionGroupRepository.findByMenuId(menuId);
        return optionGroupRepository.findAllById(mappings.stream().map(MenuOptionGroup::getOptionGroupId).toList());
    }

    public List<Option> findByOptionGroups(List<OptionGroup> optionGroups) {
        return optionRepository.findAllByIdOptionGroupIdIn(optionGroups.stream().map(OptionGroup::getId).toList());
    }
}
