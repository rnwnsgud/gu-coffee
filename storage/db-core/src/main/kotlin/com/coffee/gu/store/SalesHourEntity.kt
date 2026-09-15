package com.coffee.gu.store

import com.coffee.gu.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.DayOfWeek
import java.time.LocalTime

@Table(name = "sales_hour")
@Entity
class SalesHourEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val storeId: Long,
    @Column(name = "`day`")
    val day: DayOfWeek,
    val open: LocalTime,
    val close: LocalTime,
) : BaseEntity() {

    fun toModel(): SalesHour {
        return SalesHour(
            storeId = storeId,
            day = day,
            open = open,
            close = close,
        )
    }
}
