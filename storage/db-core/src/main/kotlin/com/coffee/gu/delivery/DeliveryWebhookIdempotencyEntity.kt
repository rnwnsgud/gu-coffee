package com.coffee.gu.delivery

import com.coffee.gu.BaseEntity
import com.coffee.gu.enums.DeliveryPlatform
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "webhook_idempotency")
@Entity
class DeliveryWebhookIdempotencyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Enumerated(EnumType.STRING)
    val platform: DeliveryPlatform,
    val idempotencyKey: String,
    val orderKey: String,
) : BaseEntity() {

    fun toModel(): DeliveryWebhookIdempotencyRecord {
        return DeliveryWebhookIdempotencyRecord(
            id = id,
            platform = platform,
            idempotencyKey = idempotencyKey,
            orderKey = orderKey,
        )
    }

    companion object {
        fun from(record: DeliveryWebhookIdempotencyRecord): DeliveryWebhookIdempotencyEntity {
            return DeliveryWebhookIdempotencyEntity(
                id = record.id,
                platform = record.platform,
                idempotencyKey = record.idempotencyKey,
                orderKey = record.orderKey,
            )
        }
    }
}
