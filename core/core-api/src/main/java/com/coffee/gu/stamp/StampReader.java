package com.coffee.gu.stamp;

import com.coffee.gu.Principal;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class StampReader {
    private final StampRepository stampRepository;
    private final StampHistoryRepository stampHistoryRepository;

    public StampReader(StampRepository stampRepository, StampHistoryRepository stampHistoryRepository) {
        this.stampRepository = stampRepository;
        this.stampHistoryRepository = stampHistoryRepository;
    }

    public Long getAvailableStampCounts(Principal principal) {
        return stampRepository.countAvailableStamps(principal.getKey(), LocalDateTime.now());
    }

    public Long getExpiringSoonCount(Principal principal, LocalDateTime now) {
        return stampRepository.countExpiringStamps(principal.getKey(), now, now.plusDays(Stamp.EXPIRY_ALARM_DAYS));
    }

    public List<StampHistory> getStampHistories(Principal principal) {
        return stampHistoryRepository.getWithinNMonths(principal.getKey(), LocalDateTime.now().minusMonths(Stamp.EXPIRY_DAYS));
    }
}
