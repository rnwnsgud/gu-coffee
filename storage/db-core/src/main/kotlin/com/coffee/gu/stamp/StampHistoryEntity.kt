package com.coffee.gu.stamp

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import com.coffee.gu.enums.StampHistoryType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "stamp_history")
class StampHistoryEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    @Enumerated(EnumType.STRING)
    val type: StampHistoryType,
    val storeId: Long,
    val storeName: String,
    val quantity: Long,
    val recordedAt: LocalDateTime,
    val expiredAt: LocalDateTime? = null,
) : BaseEntity() {

    fun toModel(): StampHistory {
        return StampHistory(
            id = id,
            principal = Principal(principalKey, principalType),
            type = type,
            storeId = storeId,
            storeName = storeName,
            quantity = quantity,
            recordedAt = recordedAt,
            expiredAt = expiredAt,
        )
    }

    companion object {
        @JvmStatic
        fun from(stampHistory: StampHistory): StampHistoryEntity = StampHistoryEntity(
            id = stampHistory.id,
            principalKey = stampHistory.principal.key,
            principalType = stampHistory.principal.type,
            type = stampHistory.type,
            storeId = stampHistory.storeId,
            storeName = stampHistory.storeName,
            quantity = stampHistory.quantity,
            recordedAt = stampHistory.recordedAt,
            expiredAt = stampHistory.expiredAt,
        )
    }
}
