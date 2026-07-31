package com.coffee.gu.stamp;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface StampRepository {
    Stamp save(Stamp stamp);
    List<Stamp> saveAll(Collection<Stamp> stamps);
    Long countAvailableStamps(String principalKey, LocalDateTime now);
    List<Stamp> getAvailableStamps(String principalKey, LocalDateTime now, int pageNumber, int pageSize);
    Long countExpiringStamps(String principalKey, LocalDateTime now, LocalDateTime nDaysLater);
    List<Stamp> findByOrderKey(String orderKey);
}
