package com.coffee.gu.webhook

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
class WebhookIdempotencyEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Enumerated(EnumType.STRING)
    val platform: DeliveryPlatform,
    val idempotencyKey: String,
    val orderKey: String,
) : BaseEntity() {

    fun toModel(): WebhookIdempotencyRecord {
        return WebhookIdempotencyRecord(
            id = id,
            platform = platform,
            idempotencyKey = idempotencyKey,
            orderKey = orderKey,
        )
    }

    companion object {
        fun from(record: WebhookIdempotencyRecord): WebhookIdempotencyEntity {
            return WebhookIdempotencyEntity(
                id = record.id,
                platform = record.platform,
                idempotencyKey = record.idempotencyKey,
                orderKey = record.orderKey,
            )
        }
    }
}
