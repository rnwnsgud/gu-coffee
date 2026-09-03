package com.coffee.gu.stamp

import java.time.LocalDateTime

interface StampRepository {
    fun save(stamp: Stamp): Stamp
    fun saveAll(stamps: Collection<Stamp>): List<Stamp>
    fun countAvailableStamps(principalKey: String, now: LocalDateTime): Long
    fun getAvailableStamps(principalKey: String, now: LocalDateTime, pageNumber: Int, pageSize: Int): List<Stamp>
    fun countExpiringStamps(principalKey: String, now: LocalDateTime, nDaysLater: LocalDateTime): Long
    fun findByOrderKey(orderKey: String): List<Stamp>
}
