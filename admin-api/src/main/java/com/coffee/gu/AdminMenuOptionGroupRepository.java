package com.coffee.gu;

import com.coffee.gu.admin.domain.AdminEntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminMenuOptionGroupRepository extends JpaRepository<AdminMenuOptionGroupEntity, Long> {
    List<AdminMenuOptionGroupEntity> findByMenuIdAndEntityStatus(Long menuId, AdminEntityStatus entityStatus);
    List<AdminMenuOptionGroupEntity> findByMenuId(Long menuId);
}
