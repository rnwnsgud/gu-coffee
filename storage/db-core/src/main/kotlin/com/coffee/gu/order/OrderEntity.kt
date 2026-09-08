package com.coffee.gu.order

import com.coffee.gu.BaseCustomIdEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.OrderState
import com.coffee.gu.enums.PrincipalType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "`order`")
class OrderEntity(
    @Id
    val orderKey: String,
    val name: String,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    val storeId: Long,
    val totalPrice: BigDecimal,
    @Enumerated(EnumType.STRING)
    var state: OrderState,
    isNewEntity: Boolean = true,
) : BaseCustomIdEntity<String>(isNewEntity) {

    override fun getId(): String = orderKey

    fun toModel(): Order {
        return Order(
            key = orderKey,
            name = name,
            principal = Principal(principalKey, principalType),
            storeId = storeId,
            totalPrice = totalPrice,
            state = state,
            lines = emptyList(),
        )
    }

    companion object {
        @JvmStatic
        fun create(order: Order): OrderEntity = OrderEntity(
            orderKey = order.key,
            name = order.name,
            principalKey = order.principal.key,
            principalType = order.principal.type,
            storeId = order.storeId,
            totalPrice = order.totalPrice,
            state = order.state,
            isNewEntity = true,
        )

        @JvmStatic
        fun from(order: Order): OrderEntity = OrderEntity(
            orderKey = order.key,
            name = order.name,
            principalKey = order.principal.key,
            principalType = order.principal.type,
            storeId = order.storeId,
            totalPrice = order.totalPrice,
            state = order.state,
            isNewEntity = false,
        )
    }
}
