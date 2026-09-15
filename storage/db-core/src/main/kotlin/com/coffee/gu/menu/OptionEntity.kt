package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal

@Table(name = "option")
@Entity
class OptionEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val optionGroupId: Long,
    val name: String,
    val extraPrice: BigDecimal,
) : BaseEntity() {

    fun toModel(): Option {
        return Option(
            id = id,
            optionGroupId = optionGroupId,
            name = name,
            extraPrice = extraPrice,
        )
    }
}
