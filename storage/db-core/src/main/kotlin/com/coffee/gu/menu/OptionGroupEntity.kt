package com.coffee.gu.menu

import com.coffee.gu.BaseEntity
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Table(name = "option_group")
@Entity
class OptionGroupEntity @JvmOverloads constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,
    val name: String,
    val isExclusive: Boolean,
    val isRequired: Boolean,
) : BaseEntity() {

    fun toModel(): OptionGroup {
        return OptionGroup(
            id = id,
            name = name,
            isExclusive = isExclusive,
            isRequired = isRequired,
        )
    }
}
