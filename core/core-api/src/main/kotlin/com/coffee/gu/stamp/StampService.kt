package com.coffee.gu.stamp

import com.coffee.gu.Principal
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class StampService(
    private val stampReader: StampReader,
) {
    fun count(principal: Principal): StampCount {
        val count = stampReader.getAvailableStampCounts(principal)
        return StampCount(principal.key, count)
    }

    fun countExpiringSoon(principal: Principal): StampExpiringSoonCount {
        val now = LocalDateTime.now()
        val expiringSoonCount = stampReader.getExpiringSoonCount(principal, now)
        return StampExpiringSoonCount(principal.key, expiringSoonCount)
    }

    fun getStampHistories(principal: Principal): List<StampHistory> {
        return stampReader.getStampHistories(principal)
    }
}
