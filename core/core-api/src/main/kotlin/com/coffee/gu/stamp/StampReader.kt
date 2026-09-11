package com.coffee.gu.stamp

import com.coffee.gu.Principal
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class StampReader(
    private val stampRepository: StampRepository,
    private val stampHistoryRepository: StampHistoryRepository,
) {
    fun getAvailableStampCounts(principal: Principal): Int {
        return stampRepository.countAvailableStamps(principal.key, LocalDateTime.now()).toInt()
    }

    fun getExpiringSoonCount(principal: Principal, now: LocalDateTime): Int {
        return stampRepository.countExpiringStamps(principal.key, now, now.plusDays(Stamp.EXPIRY_ALARM_DAYS)).toInt()
    }

    fun getStampHistories(principal: Principal): List<StampHistory> {
        return stampHistoryRepository.getWithinNMonths(principal.key, LocalDateTime.now().minusMonths(Stamp.EXPIRY_DAYS))
    }
}
