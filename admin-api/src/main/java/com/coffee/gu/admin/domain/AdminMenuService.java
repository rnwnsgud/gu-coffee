package com.coffee.gu.admin.domain;

import com.coffee.gu.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
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

    @CacheEvict(cacheNames = "menuDetail", key = "#menuId")
    public Long createMenuOptionGroup(Long menuId, Long optionGroupId) {
        return adminMenuOptionGroupRepository.save(new AdminMenuOptionGroupEntity(menuId, optionGroupId)).getId();
    }

    @CacheEvict(cacheNames = "menuDetail", key = "#menuId")
    public void updateMenuOptionGroups(Long menuId, List<Long> optionGroupIds) {
        List<AdminMenuOptionGroupEntity> existingMappings =
                adminMenuOptionGroupRepository.findByMenuIdAndEntityStatus(menuId, AdminEntityStatus.ACTIVE);

        for (AdminMenuOptionGroupEntity mapping : existingMappings) {
            mapping.delete();
        }

        if (optionGroupIds != null && !optionGroupIds.isEmpty()) {
            List<AdminMenuOptionGroupEntity> newMappings = optionGroupIds.stream()
                    .distinct()
                    .map(optionGroupId -> new AdminMenuOptionGroupEntity(menuId, optionGroupId))
                    .toList();
            adminMenuOptionGroupRepository.saveAll(newMappings);
        }
    }
}
