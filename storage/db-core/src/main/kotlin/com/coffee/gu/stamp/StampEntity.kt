package com.coffee.gu.stamp

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import com.coffee.gu.enums.StampState
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(
    name = "stamp",
    indexes = [
        Index(
            name = "idx_stamp_user_state_expiry_created",
            columnList = "principalKey, state, expiredAt, createdAt",
        ),
    ],
)
@Entity
class StampEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val orderKey: String,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    @Enumerated(EnumType.STRING)
    var state: StampState,
    val expiredAt: LocalDateTime,
) : BaseEntity() {

    fun toModel(): Stamp {
        return Stamp(
            id = id,
            orderKey = orderKey,
            principal = Principal(principalKey, principalType),
            state = state,
            createdAt = createdAt,
            expiredAt = expiredAt,
        )
    }

    companion object {
        fun from(stamp: Stamp): StampEntity = StampEntity(
            id = stamp.id,
            orderKey = stamp.orderKey,
            principalKey = stamp.principal.key,
            principalType = stamp.principal.type,
            state = stamp.state,
            expiredAt = stamp.expiredAt,
        )
    }

}
