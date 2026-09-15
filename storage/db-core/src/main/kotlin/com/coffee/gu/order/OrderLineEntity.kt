package com.coffee.gu.order

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "order_line")
class OrderLineEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val orderKey: String,
    val menuId: Long,
    val menuName: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val quantity: Long,
    val unitPrice: BigDecimal,
    val totalPrice: BigDecimal,
    val isStampEligible: Boolean,
) : BaseEntity() {

    fun toModel(): OrderLine {
        return OrderLine(
            id = id,
            orderKey = orderKey,
            menuId = menuId,
            menuName = menuName,
            imageUrl = imageUrl,
            description = description,
            quantity = quantity,
            unitPrice = unitPrice,
            totalPrice = totalPrice,
            isStampEligible = isStampEligible,
        )
    }

    companion object {
        fun from(orderLine: OrderLine): OrderLineEntity = OrderLineEntity(
            id = orderLine.id ?: 0L,
            orderKey = orderLine.orderKey,
            menuId = orderLine.menuId,
            menuName = orderLine.menuName,
            imageUrl = orderLine.imageUrl,
            description = orderLine.description,
            quantity = orderLine.quantity,
            unitPrice = orderLine.unitPrice,
            totalPrice = orderLine.totalPrice,
            isStampEligible = orderLine.isStampEligible,
        )
    }
}
