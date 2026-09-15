package com.coffee.gu.transaction

import com.coffee.gu.BaseEntity
import com.coffee.gu.Principal
import com.coffee.gu.enums.PrincipalType
import com.coffee.gu.enums.TransactionType
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "transaction_history")
class TransactionHistoryEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    @Enumerated(EnumType.STRING)
    val type: TransactionType,
    val principalKey: String,
    @Enumerated(EnumType.STRING)
    val principalType: PrincipalType,
    val orderKey: String,
    val paymentId: Long,
    val externalTransactionKey: String? = null,
    val amount: BigDecimal,
    val message: String? = null,
    val occurredAt: OffsetDateTime? = null,
) : BaseEntity() {

    fun toModel(): TransactionHistory {
        return TransactionHistory(
            id = id,
            type = type,
            principal = Principal(principalKey, principalType),
            orderKey = orderKey,
            paymentId = paymentId,
            externalTransactionKey = externalTransactionKey,
            amount = amount,
            message = message,
            occurredAt = occurredAt,
        )
    }

    companion object {
        fun from(transactionHistory: TransactionHistory): TransactionHistoryEntity = TransactionHistoryEntity(
            id = transactionHistory.id,
            type = transactionHistory.type,
            principalKey = transactionHistory.principal.key,
            principalType = transactionHistory.principal.type,
            orderKey = transactionHistory.orderKey,
            paymentId = transactionHistory.paymentId,
            externalTransactionKey = transactionHistory.externalTransactionKey,
            amount = transactionHistory.amount,
            message = transactionHistory.message,
            occurredAt = transactionHistory.occurredAt,
        )
    }
}
