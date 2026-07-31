package com.coffee.admin.domain;

import com.coffee.gu.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AdminMenuService {

    private final AdminMenuRepository adminMenuRepository;
    private final AdminOptionGroupRepository adminOptionGroupRepository;
    private final AdminOptionRepository adminOptionRepository;
    private final AdminMenuOptionGroupRepository adminMenuOptionGroupRepository;

    public AdminMenuService(AdminMenuRepository adminMenuRepository, AdminOptionGroupRepository adminOptionGroupRepository, AdminOptionRepository adminOptionRepository, AdminMenuOptionGroupRepository adminMenuOptionGroupRepository) {
        this.adminMenuRepository = adminMenuRepository;
        this.adminOptionGroupRepository = adminOptionGroupRepository;
        this.adminOptionRepository = adminOptionRepository;
        this.adminMenuOptionGroupRepository = adminMenuOptionGroupRepository;
    }

    public Long create(CreateAdminMenu command) {
        return adminMenuRepository.save(new AdminMenuEntity(
                command.name(),
                command.price().costPrice(),
                command.price().salesPrice(),
                command.imageUrl(),
                command.description(),
                command.detail().nutrition().capacity(),
                command.detail().nutrition().caffeine(),
                command.detail().nutrition().calories(),
                command.detail().nutrition().sodium(),
                command.detail().nutrition().carbohydrate(),
                command.detail().nutrition().sugar(),
                command.detail().nutrition().fat(),
                command.detail().nutrition().saturatedFat(),
                command.detail().nutrition().protein(),
                command.detail().containedAllergens(),
                command.detail().mayContainAllergens()
        )).getId();
    }

    public Long createOptionGroup(String name, Boolean exclusive, Boolean required) {
        return adminOptionGroupRepository.save(new AdminOptionGroupEntity(name, exclusive, required)).getId();
    }

    public Long createOption(Long optionGroupId, String name, long extraPrice) {
        return adminOptionRepository.save(new AdminOptionEntity(optionGroupId, name, BigDecimal.valueOf(extraPrice))).getId();
    }

    public Long createMenuOptionGroup(Long menuId, Long optionGroupId) {
        return adminMenuOptionGroupRepository.save(new AdminMenuOptionGroupEntity(menuId, optionGroupId)).getId();
    }
}
